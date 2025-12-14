package com.example.bluechat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import android.widget.TextView

class ChatAdapter : RecyclerView.Adapter<ChatAdapter.VH>() {

    private val list = mutableListOf<ChatMessage>()

    fun add(msg: ChatMessage) {
        list.add(msg)
        notifyItemInserted(list.size - 1)
    }

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val msg: TextView = v.findViewById(R.id.msg)
    }

    override fun onCreateViewHolder(p: ViewGroup, v: Int): VH {
        val view = LayoutInflater.from(p.context)
            .inflate(R.layout.item_chat, p, false)
        return VH(view)
    }

    override fun onBindViewHolder(h: VH, i: Int) {
        h.msg.text = list[i].text
        h.msg.gravity = if (list[i].mine) Gravity.END else Gravity.START
    }

    override fun getItemCount() = list.size
}
