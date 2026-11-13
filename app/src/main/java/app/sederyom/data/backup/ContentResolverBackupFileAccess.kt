package app.sederyom.data.backup

import android.content.Context
import android.net.Uri
import app.sederyom.domain.repository.BackupFileAccess
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContentResolverBackupFileAccess(private val context: Context) : BackupFileAccess {
    override suspend fun write(location: String, text: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val stream = context.contentResolver.openOutputStream(Uri.parse(location), "wt")
            stream?.bufferedWriter(Charsets.UTF_8)?.use { it.write(text) } != null
        } catch (e: IOException) {
            false
        } catch (e: SecurityException) {
            false
        }
    }

    override suspend fun read(location: String): String? = withContext(Dispatchers.IO) {
        try {
            val stream = context.contentResolver.openInputStream(Uri.parse(location)) ?: return@withContext null
            stream.use { input -> readLimited(input) }
        } catch (e: IOException) {
            null
        } catch (e: SecurityException) {
            null
        }
    }

    /** Reads at most [MAX_BYTES]; returns null for anything larger. */
    private fun readLimited(input: InputStream): String? {
        val output = ByteArrayOutputStream()
        val chunk = ByteArray(CHUNK_BYTES)
        var total = 0
        while (true) {
            val count = input.read(chunk)
            if (count < 0) break
            total += count
            if (total > MAX_BYTES) return null
            output.write(chunk, 0, count)
        }
        return String(output.toByteArray(), Charsets.UTF_8)
    }

    private companion object {
        const val CHUNK_BYTES = 8 * 1024
        const val MAX_BYTES = 2 * 1024 * 1024
    }
}
