package com.example.bluechat

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter(
    private val onLongClick: (ChatMessage) -> Unit = {}
) : RecyclerView.Adapter<ChatAdapter.VH>() {

    private val list = mutableListOf<ChatMessage>()

    fun setMessages(messages: List<ChatMessage>) {
        list.clear()
        list.addAll(messages)
        notifyDataSetChanged()
    }

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val msg: TextView = v.findViewById(R.id.msg)
        val container: LinearLayout = v as LinearLayout
    }

    override fun onCreateViewHolder(p: ViewGroup, v: Int): VH {
        val view = LayoutInflater.from(p.context)
            .inflate(R.layout.item_chat, p, false)
        return VH(view)
    }

    override fun onBindViewHolder(h: VH, i: Int) {
        val item = list[i]
        h.msg.text = item.text

        val context = h.itemView.context
        if (item.isMine) {
            h.container.gravity = Gravity.END
            h.msg.setBackgroundResource(R.drawable.bg_chat_bubble_sent)
            h.msg.setTextColor(ContextCompat.getColor(context, R.color.bg_dark))
        } else {
            h.container.gravity = Gravity.START
            h.msg.setBackgroundResource(R.drawable.bg_chat_bubble_received)
            h.msg.setTextColor(ContextCompat.getColor(context, R.color.text_main))
        }

        h.itemView.setOnLongClickListener {
            onLongClick(item)
            true
        }
    }

    override fun getItemCount() = list.size
}
