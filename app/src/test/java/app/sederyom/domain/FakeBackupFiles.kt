package app.sederyom.domain

import app.sederyom.domain.repository.BackupFileAccess

class FakeBackupFiles(private val files: MutableMap<String, String> = mutableMapOf()) : BackupFileAccess {
    var failWrites = false

    operator fun get(location: String): String? = files[location]

    operator fun set(location: String, text: String) {
        files[location] = text
    }

    override suspend fun write(location: String, text: String): Boolean {
        if (failWrites) return false
        files[location] = text
        return true
    }

    override suspend fun read(location: String): String? = files[location]
}
