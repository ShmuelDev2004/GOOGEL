package app.sederyom.domain.repository

/** Reads and writes backup text at a location (a content URI on Android, kept as a plain string here). */
interface BackupFileAccess {
    /** Returns false when writing failed. */
    suspend fun write(location: String, text: String): Boolean

    /** Returns null when the file cannot be read or is unreasonably large. */
    suspend fun read(location: String): String?
}
