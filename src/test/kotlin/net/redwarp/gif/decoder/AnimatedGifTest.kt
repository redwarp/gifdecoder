package net.redwarp.gif.decoder

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.io.File
import javax.imageio.ImageIO

class AnimatedGifTest {
    @Test
    fun parseAnimatedGif_graphicControlAllSet() {
        val gifFile = File("./assets/domo.gif")

        val gifParser = GifParser(gifFile)
        gifParser.parseContent()

        Assertions.assertEquals(3, gifParser.imageDescriptors.size)
    }

    @Test
    fun parseAnimatedGif_savePictures() {
        val gifFile = File("./assets/domo-interlaced.gif")

        val gifParser = GifParser(gifFile)
        gifParser.parseContent()

        val dimension = Dimension(gifParser.width, gifParser.height)

        gifParser.imageDescriptors.forEachIndexed { index, imageDescriptor ->
            val image: BufferedImage = BufferedImage(gifParser.width, gifParser.height, BufferedImage.TYPE_INT_ARGB)
            val pixels = (image.raster.dataBuffer as DataBufferInt).data
            imageDescriptor.fillPixels(pixels, dimension, gifParser.globalColorMap)

            ImageIO.write(image, "png", File("./output/frame_$index.png"))
        }
    }
}
