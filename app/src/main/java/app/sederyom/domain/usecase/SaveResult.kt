package app.sederyom.domain.usecase

sealed interface SaveResult {
    data class Saved(val id: Long) : SaveResult

    data class Rejected(val errors: List<BlockError>) : SaveResult
}
