package app.sederyom.ui.components

import android.content.Context
import app.sederyom.R
import app.sederyom.domain.usecase.ImportError

fun describeImportError(context: Context, error: ImportError): String = when (error) {
    ImportError.CannotRead -> context.getString(R.string.import_error_read)
    ImportError.NotValidJson -> context.getString(R.string.import_error_json)
    is ImportError.UnsupportedVersion -> context.getString(R.string.import_error_version, error.version)
    is ImportError.BadValue -> context.getString(R.string.import_error_value, isolateLtr(error.location))
    is ImportError.TooMany -> context.getString(R.string.import_error_too_many, isolateLtr(error.location))
    is ImportError.InvalidBlocks -> context.getString(
        R.string.import_error_blocks,
        isolateLtr(error.location),
        describeErrors(context, error.errors),
    )
}
