package com.example.bluechat

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket

class BluetoothClient(
    private val device: BluetoothDevice,
    private val onConnected: (BluetoothSocket) -> Unit
) : Thread() {

    override fun run() {
        val socket = device.createRfcommSocketToServiceRecord(Constants.CHAT_UUID)
        socket.connect()
        onConnected(socket)
    }
}
