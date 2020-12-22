package net.redwarp.gif.decoder

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.io.File
import javax.imageio.ImageIO

class GifTest {
    @Test
    fun getFrame_each_properlyRenders() {
        val gifDescriptor = Parser().parse(File("./assets/domo.gif"))
        val gif = Gif(gifDescriptor)
        val dimension = gif.dimension

        val pixels = IntArray(dimension.size)
        for (index in 0 until gif.frameCount) {
            gif.getFrame(index, pixels)
            val expectedPixels = loadExpectedPixels(File("./assets/frames/domo_$index.png"))

            Assertions.assertArrayEquals(expectedPixels, pixels)
        }
    }

    @Test
    fun getFrame_transparentBackground_properlyRenders() {
        val gifDescriptor = Parser().parse(File("./assets/simple-nopalette.gif"))
        val gif = Gif(gifDescriptor)
        val dimension = gif.dimension

        val image = BufferedImage(dimension.width, dimension.height, BufferedImage.TYPE_INT_ARGB)
        val pixels = (image.raster.dataBuffer as DataBufferInt).data
        for (index in 0 until gif.frameCount) {
            gif.getFrame(index, pixels)

            ImageIO.write(image, "png", File("../output/frame_$index.png"))
        }
    }

    private fun loadExpectedPixels(file: File): IntArray {
        val input = ImageIO.read(file)
        val image = BufferedImage(input.width, input.height, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        graphics.drawImage(input, 0, 0, input.width, input.height, null)
        graphics.dispose()
        return (image.raster.dataBuffer as DataBufferInt).data
    }
}