package com.example.bluechat

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager


class ChatActivity : AppCompatActivity() {

    private var service: BluetoothService? = null

    private val adapter = ChatAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rv = RecyclerView(this)
        val input = EditText(this)
        val btn = Button(this).apply { text = "Send" }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(rv)
            addView(input)
            addView(btn)
        }

        setContentView(layout)

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        val btAdapter = BluetoothAdapter.getDefaultAdapter()
        val device = intent.getParcelableExtra<BluetoothDevice>("device")

        if (device == null) {
            BluetoothServer(btAdapter) {
                startService(it)
            }.start()
        } else {
            BluetoothClient(device) {
                startService(it)
            }.start()
        }

        btn.setOnClickListener {
            val msg = input.text.toString()

            if (service == null) {
                Toast.makeText(this, "Not connected yet", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            service?.send(msg)
            adapter.addMessage(ChatMessage(msg, true))
            input.text.clear()
        }

    }

    private fun startService(socket: BluetoothSocket) {
        service = BluetoothService(socket)
        service?.listen {
            runOnUiThread {
                adapter.add(ChatMessage(it, false))
            }
        }
    }
}

private fun ChatAdapter.addMessage(chatMessage: ChatMessage) {}
