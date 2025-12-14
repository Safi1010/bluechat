package com.example.bluechat

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class DeviceListActivity : AppCompatActivity() {

    private lateinit var adapter: DeviceAdapter
    private lateinit var progress: ProgressBar
    private val btAdapter = BluetoothAdapter.getDefaultAdapter()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    device?.let { adapter.addDevice(it) }
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    progress.visibility = View.GONE
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_list)

        progress = findViewById(R.id.progress)
        val recycler = findViewById<RecyclerView>(R.id.deviceList)

        adapter = DeviceAdapter(mutableListOf()) {
            val i = Intent(this, ChatActivity::class.java)
            i.putExtra("device", it)
            startActivity(i)
        }

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        startDiscovery()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private fun startDiscovery() {
        progress.visibility = View.VISIBLE

        registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        registerReceiver(receiver, IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))

        btAdapter.startDiscovery()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
        btAdapter.cancelDiscovery()
    }
}

