package cn.lux.music.data.sync.lx

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

internal class LxFrameCodec(
    private val maxEncodedBytes: Int = 8 * 1024 * 1024,
    private val maxDecodedBytes: Int =
        32 *
            1024 *
            1024,
) {
    init {
        require(maxEncodedBytes > 0 && maxDecodedBytes > 0)
    }

    fun encode(text: String): String {
        val bytes = text.toByteArray(Charsets.UTF_8)
        if (bytes.size > maxDecodedBytes) throw LxProtocolException("LX decoded frame limit exceeded")
        val frame =
            if (text.length > 1024) {
                val output = ByteArrayOutputStream()
                GZIPOutputStream(output).use { it.write(bytes) }
                "cg_" + Base64.getEncoder().encodeToString(output.toByteArray())
            } else {
                text
            }
        checkEncoded(frame)
        return frame
    }

    fun decode(frame: String): String {
        checkEncoded(frame)
        if (!frame.startsWith("cg_")) {
            if (frame.toByteArray(Charsets.UTF_8).size >
                maxDecodedBytes
            ) {
                throw LxProtocolException("LX decoded frame limit exceeded")
            }
            return frame
        }
        return decodeGzip(frame)
    }

    private fun decodeGzip(frame: String): String =
        try {
            val bytes =
                GZIPInputStream(
                    ByteArrayInputStream(Base64.getDecoder().decode(frame.substring(3))),
                ).use(::readBounded)
            Charsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        } catch (
            error: LxProtocolException,
        ) {
            throw error
        } catch (_: Exception) {
            throw LxProtocolException("Invalid LX compressed frame")
        }

    private fun readBounded(input: InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > maxDecodedBytes) throw LxProtocolException("LX decoded frame limit exceeded")
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private fun checkEncoded(frame: String) {
        if (frame.length > maxEncodedBytes || frame.toByteArray(Charsets.UTF_8).size > maxEncodedBytes) {
            throw LxProtocolException("LX encoded frame limit exceeded")
        }
    }
}
