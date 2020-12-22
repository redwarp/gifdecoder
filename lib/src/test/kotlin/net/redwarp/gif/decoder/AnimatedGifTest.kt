package net.redwarp.gif.decoder

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

class AnimatedGifTest {
    @Test
    fun parseAnimatedGif_graphicControlAllSet() {
        val gifFile = File("./assets/domo.gif")

        val gifDescriptor: GifDescriptor = Parser().parse(gifFile)
        val gif = Gif(gifDescriptor)

        Assertions.assertEquals(3, gif.frameCount)
    }
}
