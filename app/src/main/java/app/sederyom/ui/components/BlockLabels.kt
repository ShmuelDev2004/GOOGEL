package app.sederyom.ui.components

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import app.sederyom.R
import app.sederyom.domain.model.BlockType

@StringRes
fun BlockType.labelRes(): Int = when (this) {
    BlockType.SLEEP -> R.string.type_sleep
    BlockType.MEAL -> R.string.type_meal
    BlockType.WORK -> R.string.type_work
    BlockType.STUDY -> R.string.type_study
    BlockType.EXERCISE -> R.string.type_exercise
    BlockType.FREE -> R.string.type_free
    BlockType.OTHER -> R.string.type_other
}

/** The user's own title, or the type's name when no title was given. */
fun blockName(context: Context, type: BlockType, title: String): String =
    title.ifBlank { context.getString(type.labelRes()) }

@Composable
fun blockNameText(type: BlockType, title: String): String = blockName(LocalContext.current, type, title)
