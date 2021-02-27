package app.redwarp.gif.decoder.android

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

fun InputStream.toFile(context: Context, fileName: String): File {
    val file = File(context.filesDir, fileName)
    val fileOutputStream = FileOutputStream(file)

    this.copyTo(fileOutputStream)

    fileOutputStream.close()
    return file
}
