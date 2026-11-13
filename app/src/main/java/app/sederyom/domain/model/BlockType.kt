package app.sederyom.domain.model

enum class BlockType {
    SLEEP,
    MEAL,
    WORK,
    STUDY,
    EXERCISE,
    FREE,
    OTHER,
    ;

    companion object {
        /** Unknown or missing names fall back to [OTHER] so old data never crashes the app. */
        fun fromName(name: String?): BlockType = entries.firstOrNull { it.name == name } ?: OTHER
    }
}
