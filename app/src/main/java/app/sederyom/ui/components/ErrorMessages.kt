package app.sederyom.ui.components

import android.content.Context
import app.sederyom.R
import app.sederyom.domain.usecase.BlockError
import app.sederyom.domain.usecase.ValidateBlock

fun describeErrors(context: Context, errors: List<BlockError>): String = errors.joinToString("\n") { error ->
    when (error) {
        BlockError.DurationTooShort -> context.getString(R.string.error_duration_short)
        BlockError.DurationTooLong -> context.getString(R.string.error_duration_long)
        BlockError.TitleTooLong -> context.getString(R.string.error_title_long, ValidateBlock.MAX_TITLE_LENGTH)
        is BlockError.Overlaps -> context.getString(
            R.string.error_overlap,
            blockName(context, error.other.type, error.other.title),
        )
    }
}
