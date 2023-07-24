package com.khush.workmanager.ui

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.khush.workmanager.R
import com.khush.workmanager.bean.DownloadItem

class FilesAdapter(private val listener: FileClickListener): ListAdapter<DownloadItem, FilesAdapter.ViewHolder>(
    DiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.fileName?.text = getItem(position).fileName
        holder.fileStatus?.text = getItem(position).status
        holder.itemView.setOnClickListener {
            listener.onFileClick(getItem(position).fileUri)
        }
    }

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        var fileName: TextView? = null
        var fileStatus: TextView? = null
        init {
            fileName = itemView.findViewById(R.id.fileName)
            fileStatus = itemView.findViewById(R.id.fileStatus)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<DownloadItem>() {
        override fun areItemsTheSame(oldItem: DownloadItem, newItem: DownloadItem): Boolean {
            return oldItem.fileName == newItem.fileName
        }

        override fun areContentsTheSame(oldItem: DownloadItem, newItem: DownloadItem): Boolean {
            return (oldItem == newItem)
        }

    }

    interface FileClickListener {
        fun onFileClick(uri: Uri?)
    }

}