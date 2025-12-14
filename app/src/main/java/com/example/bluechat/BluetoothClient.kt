package com.example.bluechat

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import java.util.UUID

class BluetoothClient(
    private val device: BluetoothDevice,
    private val uuid: UUID,
    private val onConnected: (BluetoothSocket) -> Unit
) : Thread() {

    @SuppressLint("MissingPermission")
    override fun run() {
        try {
            val socket = device.createRfcommSocketToServiceRecord(uuid)
            socket.connect()
            onConnected(socket)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
