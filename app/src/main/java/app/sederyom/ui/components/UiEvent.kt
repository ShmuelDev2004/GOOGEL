package app.sederyom.ui.components

import androidx.annotation.StringRes
import app.sederyom.domain.usecase.BlockError

/** One-off messages from a ViewModel to its screen. */
sealed interface UiEvent {
    data class Error(val errors: List<BlockError>) : UiEvent

    data class Message(@StringRes val resId: Int) : UiEvent

    data object Done : UiEvent
}
