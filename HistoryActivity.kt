package com.humaira.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.humaira.app.databinding.ActivityHistoryBinding
import com.humaira.app.databinding.ItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryActivity : AppCompatActivity() {

    private lateinit var b: ActivityHistoryBinding
    private lateinit var storage: Storage
    private lateinit var adapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(b.root)
        storage = Storage(this)

        b.btnBack.setOnClickListener { finish() }

        adapter = HistoryAdapter(
            onOpen = { c ->
                storage.currentId = c.id
                finish()
            },
            onDelete = { c -> confirmDelete(c) }
        )
        b.list.layoutManager = LinearLayoutManager(this)
        b.list.adapter = adapter
        refresh()
    }

    private fun refresh() {
        val items = storage.loadAll()
            .filter { it.messages.isNotEmpty() }
            .sortedByDescending { it.updated }
        adapter.submit(items)
        b.empty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun confirmDelete(c: Conversation) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete this conversation?")
            .setMessage(c.title)
            .setPositiveButton("Delete") { _, _ ->
                storage.delete(c.id)
                if (storage.currentId == c.id) storage.currentId = ""
                refresh()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

class HistoryAdapter(
    private val onOpen: (Conversation) -> Unit,
    private val onDelete: (Conversation) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.VH>() {

    private var items: List<Conversation> = emptyList()
    private val format = SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault())

    class VH(val b: ItemHistoryBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = items[position]
        holder.b.title.text = c.title
        holder.b.date.text = "${format.format(Date(c.updated))}  ·  ${c.messages.size} messages"
        holder.b.root.setOnClickListener { onOpen(c) }
        holder.b.btnDelete.setOnClickListener { onDelete(c) }
    }

    fun submit(list: List<Conversation>) {
        items = list
        notifyDataSetChanged()
    }
}
