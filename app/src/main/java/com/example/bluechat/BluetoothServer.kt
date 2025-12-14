package com.example.bluechat

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.IOException

class BluetoothServer(
    adapter: BluetoothAdapter,
    private val onConnected: (BluetoothSocket) -> Unit
) : Thread() {

    private val serverSocket: BluetoothServerSocket? =
        try {
            adapter.listenUsingRfcommWithServiceRecord(
                Constants.APP_NAME,
                Constants.CHAT_UUID
            )
        } catch (e: SecurityException) {
            Log.e("BluetoothServer", "Failed to listen for connections", e)
            null
        }

    override fun run() {
        try {
            val socket = serverSocket?.accept()
            serverSocket?.close()
            if (socket != null) {
                onConnected(socket)
            }
        } catch (e: IOException) {
            Log.e("BluetoothServer", "Failed to accept connection", e)
        }
    }
}
