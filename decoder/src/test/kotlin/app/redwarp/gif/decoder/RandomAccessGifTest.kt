package app.redwarp.gif.decoder

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class RandomAccessGifTest {

    @Test
    fun fileDeleted_getFrameReturnsFalse() {
        val gifFile = File("../assets/domo.gif")
        val tempFile = File.createTempFile("test", "gif")
        gifFile.copyTo(tempFile, true)

        val gif = Gif.from(tempFile).unwrap()

        val intArray = IntArray(gif.dimension.size)

        assertTrue(gif.getFrame(0, intArray))

        tempFile.delete()

        assertFalse(gif.getFrame(0, intArray))
    }

    @Test
    fun fileDeleted_getFrameReturnsNull() {
        val gifFile = File("../assets/domo.gif")
        val tempFile = File.createTempFile("test", "gif")
        gifFile.copyTo(tempFile, true)

        val gif = Gif.from(tempFile).unwrap()

        assertNotNull(gif.getFrame(0))

        tempFile.delete()

        assertNull(gif.getFrame(0))
    }
}