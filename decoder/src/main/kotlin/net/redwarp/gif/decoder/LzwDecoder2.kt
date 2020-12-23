package net.redwarp.gif.decoder

private const val MAX_STACK_SIZE = 4096

class LzwDecoder2 {
    private var bits = 0
    private var currentByte: Int = 0
    private var blockSize = 0
    private var codeSize = 0
    private var dataIndex = 0

    private var mask: Int = 0 // For codeSize = 3, will output 0b0111

    private val prefix = ShortArray(MAX_STACK_SIZE)
    private val suffix = ByteArray(MAX_STACK_SIZE)
    private val pixelStack = ByteArray(MAX_STACK_SIZE + 1)

    @Suppress("NOTHING_TO_INLINE")
    private inline fun readNextCode(imageData: ByteArray): Int {
        while (bits < codeSize) {
            if (blockSize == 0) {
                blockSize = imageData[dataIndex].toInt() and 0xff
                dataIndex++
            }
            currentByte += ((imageData[dataIndex].toInt() and 0xff).shl(bits))
            dataIndex++
            bits += 8
            blockSize--
        }

        val code = (currentByte and mask)
        bits -= codeSize
        currentByte = currentByte.ushr(codeSize)

        return code
    }

    fun decode(imageData: ByteArray, destination: ByteArray, pixelCount: Int) {
        dataIndex = 0
        val lzwMinimumCodeSize = imageData[dataIndex]
        dataIndex++
        val clear: Int = 1.shl(lzwMinimumCodeSize.toInt())
        val endOfData: Int = clear + 1
        codeSize = lzwMinimumCodeSize.toInt() + 1

        bits = 0
        currentByte = 0
        blockSize = 0
        mask = (1.shl(codeSize) - 1)

        var available = clear + 2
        var stackTop = 0
        var first = 0

        var oldCode: Int = -1

        for (code in 0 until clear) {
            prefix[code] = 0
            suffix[code] = code.toByte()
        }
        var pixelIndex = 0

        while (pixelIndex < pixelCount) {

            var code = readNextCode(imageData)

            if (code == clear) {
                codeSize = lzwMinimumCodeSize.toInt() + 1
                mask = (1.shl(codeSize) - 1)
                available = clear + 2
                oldCode = -1
                continue
            } else if (code > available || code == endOfData) {
                break
            } else if (oldCode == -1) {
                destination[pixelIndex] = suffix[code]
                pixelIndex++
                oldCode = code
                first = code
                continue
            }

            val initialCode = code
            if (code >= available) {
                pixelStack[stackTop] = first.toByte()
                stackTop++
                code = oldCode
            }

            while (code >= clear) {
                pixelStack[stackTop] = suffix[code]
                stackTop++
                code = prefix[code].toInt() and 0xffff
            }

            first = suffix[code].toInt() and 0xff

            destination[pixelIndex] = first.toByte()
            pixelIndex++

            while (stackTop > 0) {
                stackTop--
                destination[pixelIndex] = pixelStack[stackTop]
                pixelIndex++
            }

            if (available < MAX_STACK_SIZE) {
                prefix[available] = oldCode.toShort()
                suffix[available] = first.toByte()
                available++
                if (available and mask == 0 && available < MAX_STACK_SIZE) {
                    codeSize++
                    mask += available
                }
            }
            oldCode = initialCode
        }

    }
}