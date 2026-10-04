package com.humaira.app

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.humaira.app.databinding.ItemMessageBinding

class MessageAdapter(private var items: MutableList<Message>) :
    RecyclerView.Adapter<MessageAdapter.VH>() {

    class VH(val b: ItemMessageBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val message = items[position]
        val isUser = message.role == "user"
        holder.b.bubble.text = message.content
        holder.b.bubble.setBackgroundResource(
            if (isUser) R.drawable.bg_bubble_user else R.drawable.bg_bubble_ai
        )
        val lp = holder.b.bubble.layoutParams as LinearLayout.LayoutParams
        lp.gravity = if (isUser) Gravity.END else Gravity.START
        holder.b.bubble.layoutParams = lp
    }

    fun setItems(newItems: MutableList<Message>) {
        items = newItems
        notifyDataSetChanged()
    }
}
