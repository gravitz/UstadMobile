package com.ustadmobile.lib.rest

import com.ustadmobile.lib.rest.ResumableUploadRoute.SESSIONID
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.gson.GsonConverter
import io.ktor.gson.gson
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.routing.Routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.apache.commons.io.IOUtils
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset

class TestResumableUploadRoute {

    lateinit var server: ApplicationEngine

    private val defaultPort = 8098

    lateinit var tmpFolder: File

    @Before
    fun setup() {
        tmpFolder = File.createTempFile("upload", "")
        tmpFolder.delete()
        tmpFolder.mkdirs()

        server = embeddedServer(Netty, port = defaultPort) {
            install(ContentNegotiation) {
                gson {
                    register(ContentType.Application.Json, GsonConverter())
                    register(ContentType.Any, GsonConverter())
                }
            }

            install(Routing) {
                ResumableUploadRoute(tmpFolder)
            }
        }.start(wait = false)
    }

    @Test
    fun givenAFile_upload() {

        val CHUNKSIZE = (1024 * 16).toLong()

        val epubTmpFile = File.createTempFile("tmp", "epub")
        epubTmpFile.writeBytes(this::class.java.getResourceAsStream("/com/ustadmobile/core/contentformats/tincan/test.zip").use { it.readBytes() })

        val sessionCon = URL("http://localhost:$defaultPort/upload/createSession").openConnection() as HttpURLConnection
        sessionCon.connect()

        val sessionId = IOUtils.toString(sessionCon.inputStream, Charset.defaultCharset())

        var uploadedTo = 0L
        var readRange = 1

        while (readRange >= 1) {

            val inputStream = FileInputStream(epubTmpFile)

            // skip
            var startBytesSkipped: Long = 0
            while (startBytesSkipped < uploadedTo) {
                startBytesSkipped += inputStream.skip(uploadedTo - startBytesSkipped)
            }

            val bufferSize = if (inputStream.available() < CHUNKSIZE) inputStream.available() else CHUNKSIZE.toInt()
            val buffer = ByteArray(bufferSize)

            // read
            readRange = inputStream.read(buffer)
            val end = uploadedTo + readRange - 1

            if (readRange <= 0) {
                return
            }

            val httpCon = URL("http://localhost:$defaultPort/upload/receiveData").openConnection() as HttpURLConnection
            httpCon.doOutput = true
            httpCon.requestMethod = "PUT"
            httpCon.setRequestProperty("Content-Length", readRange.toString())
            httpCon.setRequestProperty("Range", "bytes=$uploadedTo-$end")
            httpCon.setRequestProperty(SESSIONID, sessionId)
            httpCon.outputStream.write(buffer)
            httpCon.outputStream.flush()
            httpCon.outputStream.close()
            httpCon.connect()

            val responseCode = httpCon.responseCode

            httpCon.disconnect()

            uploadedTo += readRange

            Assert.assertEquals("data uploaded successfully", HttpStatusCode.NoContent.value, responseCode)

        }

        Assert.assertArrayEquals("file matches", FileInputStream(epubTmpFile).readBytes(), FileInputStream(File(tmpFolder, sessionId)).readBytes())

    }

}