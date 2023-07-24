package com.khush.workmanager.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.khush.workmanager.viewmodel.MainViewModel
import com.khush.workmanager.databinding.FragmentMainBinding

class MainFragment: Fragment() {

    private lateinit var fragmentMainBinding: FragmentMainBinding
    private lateinit var adapter: FilesAdapter
    private val viewModel by lazy {
        ViewModelProvider(this)[MainViewModel::class.java]
    }

    companion object {
        fun newInstance(): MainFragment {
            val args = Bundle()
            val fragment = MainFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        observer()
        fragmentMainBinding = FragmentMainBinding.inflate(inflater)
        return fragmentMainBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = FilesAdapter(object : FilesAdapter.FileClickListener{
            override fun onFileClick(uri: Uri?) {
                if(uri != null) {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, requireContext().contentResolver.getType(uri))
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    try {
                        startActivity(intent)
                    } catch (e: Exception) {

                    }

                }
            }
        })
        fragmentMainBinding.recyclerView.adapter = adapter

        fragmentMainBinding.recyclerView.layoutManager = LinearLayoutManager(this.context, LinearLayoutManager.VERTICAL, false)
        fragmentMainBinding.recyclerView.addItemDecoration(DividerItemDecoration(this.context, DividerItemDecoration.VERTICAL))
        fragmentMainBinding.button.setOnClickListener {
            val url = fragmentMainBinding.editTextText.text
            viewModel.startDownloading((url as CharSequence).toString())
        }
    }

    private fun observer() {
        viewModel.getDownloadItemsLiveData().observe(viewLifecycleOwner, Observer {
            if(it.size > adapter.itemCount) {
                adapter.submitList(it)
                fragmentMainBinding.recyclerView.smoothScrollToPosition(0)
            } else {
                adapter.submitList(it)
            }
        })
    }
}