package com.example.bluechat

import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.os.Looper
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class BluetoothService(private val socket: BluetoothSocket) {

    private val input: InputStream = socket.inputStream
    private val output: OutputStream = socket.outputStream
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        const val MESSAGE_TEXT = 1
        const val MESSAGE_FILE_OFFER = 2 // Header basically
        const val MESSAGE_FILE_CHUNK = 3
        const val MESSAGE_FILE_ACCEPT = 4
        const val MESSAGE_FILE_REJECT = 5
    }

    fun send(msg: String) {
        Thread {
            try {
                // Protocol: Type (1 byte) + Length (4 bytes) + Content
                val bytes = msg.toByteArray()
                val length = bytes.size
                
                output.write(MESSAGE_TEXT)
                output.write(intToByteArray(length))
                output.write(bytes)
                output.flush()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }.start()
    }
    
    // Step 1: Offer file
    fun offerFile(fileName: String, fileSize: Int) {
        Thread {
            try {
                val nameBytes = fileName.toByteArray()
                
                output.write(MESSAGE_FILE_OFFER)
                output.write(intToByteArray(nameBytes.size))
                output.write(nameBytes)
                output.write(intToByteArray(fileSize))
                output.flush()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }.start()
    }

    // Step 2: Send file data (after acceptance)
    fun sendFileData(fileData: ByteArray, onProgress: (Int) -> Unit) {
        Thread {
            try {
                val totalSize = fileData.size
                var sent = 0
                val chunkSize = 4096 // 4KB chunks
                
                while (sent < totalSize) {
                    val remaining = totalSize - sent
                    val toWrite = if (remaining < chunkSize) remaining else chunkSize
                    
                    output.write(MESSAGE_FILE_CHUNK)
                    output.write(intToByteArray(toWrite))
                    output.write(fileData, sent, toWrite)
                    output.flush()
                    
                    sent += toWrite
                    
                    val percent = ((sent.toFloat() / totalSize) * 100).toInt()
                    handler.post { onProgress(percent) }
                    
                    Thread.sleep(2) 
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }.start()
    }
    
    fun acceptFile(accept: Boolean) {
        Thread {
            try {
                val type = if (accept) MESSAGE_FILE_ACCEPT else MESSAGE_FILE_REJECT
                output.write(type)
                output.flush()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }.start()
    }

    fun listen(
        onMessage: (String) -> Unit, 
        onFileOffer: (String, Int) -> Unit,
        onFileTransferStatus: (Boolean) -> Unit, // True = Accepted, False = Rejected
        onFileProgress: (Int) -> Unit, 
        onFileReceived: (String, ByteArray) -> Unit
    ) {
        Thread {
            try {
                var incomingFileName = ""
                var incomingFileSize = 0
                var receivedBytes = 0
                var fileBuffer: ByteArray? = null

                while (true) {
                    val type = input.read()
                    if (type == -1) break
                    
                    when (type) {
                        MESSAGE_TEXT -> {
                            val lengthBytes = ByteArray(4)
                            readFully(lengthBytes)
                            val length = byteArrayToInt(lengthBytes)
                            
                            val buffer = ByteArray(length)
                            readFully(buffer)
                            onMessage(String(buffer))
                        }
                        
                        MESSAGE_FILE_OFFER -> {
                            val nameLenBytes = ByteArray(4)
                            readFully(nameLenBytes)
                            val nameLen = byteArrayToInt(nameLenBytes)
                            
                            val nameBuffer = ByteArray(nameLen)
                            readFully(nameBuffer)
                            incomingFileName = String(nameBuffer)
                            
                            val sizeBytes = ByteArray(4)
                            readFully(sizeBytes)
                            incomingFileSize = byteArrayToInt(sizeBytes)
                            
                            // Initialize reception
                            receivedBytes = 0
                            fileBuffer = ByteArray(incomingFileSize)
                            
                            handler.post { onFileOffer(incomingFileName, incomingFileSize) }
                        }
                        
                        MESSAGE_FILE_ACCEPT -> {
                            handler.post { onFileTransferStatus(true) }
                        }
                        
                        MESSAGE_FILE_REJECT -> {
                            handler.post { onFileTransferStatus(false) }
                        }
                        
                        MESSAGE_FILE_CHUNK -> {
                            if (fileBuffer == null) continue // Should not happen if protocol followed
                            
                            val lenBytes = ByteArray(4)
                            readFully(lenBytes)
                            val chunkLen = byteArrayToInt(lenBytes)
                            
                            readFully(fileBuffer, receivedBytes, chunkLen)
                            receivedBytes += chunkLen
                            
                            val percent = ((receivedBytes.toFloat() / incomingFileSize) * 100).toInt()
                            handler.post { onFileProgress(percent) }
                            
                            if (receivedBytes >= incomingFileSize) {
                                val completeFile = fileBuffer
                                val finalName = incomingFileName
                                handler.post { onFileReceived(finalName, completeFile) }
                                fileBuffer = null // Reset
                            }
                        }
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }.start()
    }
    
    private fun readFully(buffer: ByteArray, offset: Int = 0, length: Int = buffer.size) {
        var bytesRead = 0
        while (bytesRead < length) {
            val count = input.read(buffer, offset + bytesRead, length - bytesRead)
            if (count == -1) throw IOException("End of stream")
            bytesRead += count
        }
    }
    
    private fun intToByteArray(value: Int): ByteArray {
        return byteArrayOf(
            (value shr 24).toByte(),
            (value shr 16).toByte(),
            (value shr 8).toByte(),
            value.toByte()
        )
    }
    
    private fun byteArrayToInt(bytes: ByteArray): Int {
        return (bytes[0].toInt() and 0xFF shl 24) or
               (bytes[1].toInt() and 0xFF shl 16) or
               (bytes[2].toInt() and 0xFF shl 8) or
               (bytes[3].toInt() and 0xFF)
    }
}
