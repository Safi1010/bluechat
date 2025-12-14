package com.example.bluechat

import android.bluetooth.BluetoothSocket

class BluetoothService(private val socket: BluetoothSocket) {

    private val input = socket.inputStream
    private val output = socket.outputStream

    fun send(msg: String) {
        output.write(msg.toByteArray())
    }

    fun listen(onMessage: (String) -> Unit) {
        Thread {
            val buffer = ByteArray(1024)
            while (true) {
                val bytes = input.read(buffer)
                onMessage(String(buffer, 0, bytes))
            }
        }.start()
    }
}
