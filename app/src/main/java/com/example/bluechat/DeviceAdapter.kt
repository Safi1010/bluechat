package com.example.bluechat
import android.Manifest
import android.view.ViewGroup
import android.view.LayoutInflater
import android.bluetooth.BluetoothDevice
import android.view.View
import android.widget.TextView
import androidx.annotation.RequiresPermission
import androidx.recyclerview.widget.RecyclerView

class DeviceAdapter(
private val devices: MutableList<BluetoothDevice>,
private val onClick: (BluetoothDevice) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return VH(view)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.name.text = devices[position].name ?: "Unknown Device"

        holder.itemView.alpha = 0f
        holder.itemView.animate()
            .alpha(1f)
            .setDuration(400)
            .start()

        holder.itemView.setOnClickListener {
            onClick(devices[position])
        }
    }

    override fun getItemCount() = devices.size

    fun addDevice(device: BluetoothDevice) {
        if (!devices.contains(device)) {
            devices.add(device)
            notifyItemInserted(devices.size - 1)
        }
    }
}

