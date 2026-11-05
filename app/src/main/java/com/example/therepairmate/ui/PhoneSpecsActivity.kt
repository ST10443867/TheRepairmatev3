package com.example.therepairmate.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.therepairmate.data.PhoneItem
import com.example.therepairmate.data.PhoneSpecsService
import com.example.therepairmate.databinding.ActivityPhoneSpecsBinding
import kotlinx.coroutines.launch

class PhoneSpecsActivity : BaseActivity() {
    private lateinit var b: ActivityPhoneSpecsBinding
    private val adapter = PhoneSpecsAdapter { phone -> showPhoneDetail(phone) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityPhoneSpecsBinding.inflate(layoutInflater)
        setContentView(b.root)
        addBackBar()

        b.recycler.layoutManager = LinearLayoutManager(this)
        b.recycler.adapter = adapter

        b.btnSearch.setOnClickListener { search() }
        b.etSearch.setOnEditorActionListener { _, _, _ -> search(); true }

        loadLatest()
    }

    private fun loadLatest() {
        b.progress.visibility = View.VISIBLE
        b.tvEmpty.visibility = View.GONE
        lifecycleScope.launch {
            try {
                val res = PhoneSpecsService.instance.getLatestPhones()
                val list = res.data?.phones ?: emptyList()
                adapter.submitList(list)
                if (list.isEmpty()) {
                    b.tvEmpty.visibility = View.VISIBLE
                    b.tvEmpty.text = "No latest phones found from API source."
                } else {
                    b.tvEmpty.visibility = View.GONE
                }
            } catch (e: Exception) {
                adapter.submitList(emptyList())
                b.tvEmpty.visibility = View.VISIBLE
                b.tvEmpty.text = "Failed to load from API: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                b.progress.visibility = View.GONE
            }
        }
    }

    private fun search() {
        val query = b.etSearch.text.toString().trim()
        if (query.isEmpty()) {
            loadLatest()
            return
        }
        b.progress.visibility = View.VISIBLE
        b.tvEmpty.visibility = View.GONE
        lifecycleScope.launch {
            try {
                val res = PhoneSpecsService.instance.searchPhones(query)
                val list = res.data?.phones ?: emptyList()
                adapter.submitList(list)
                if (list.isEmpty()) {
                    b.tvEmpty.visibility = View.VISIBLE
                    b.tvEmpty.text = "No phones found for '$query'"
                } else {
                    b.tvEmpty.visibility = View.GONE
                }
            } catch (e: Exception) {
                adapter.submitList(emptyList())
                b.tvEmpty.visibility = View.VISIBLE
                b.tvEmpty.text = "Search error: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                b.progress.visibility = View.GONE
            }
        }
    }

    private fun showPhoneDetail(phone: PhoneItem) {
        b.progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val res = PhoneSpecsService.instance.getPhoneDetail(phone.slug)
                val detail = res.data
                if (detail != null) {
                    val sb = StringBuilder()
                    sb.append("Brand: ${detail.brand}\n")
                    sb.append("Model: ${detail.phone_name}\n")
                    sb.append("Released: ${detail.release_date ?: "N/A"}\n")
                    sb.append("OS: ${detail.os ?: "N/A"}\n")
                    sb.append("Storage: ${detail.storage ?: "N/A"}\n")
                    sb.append("Dimensions: ${detail.dimension ?: "N/A"}\n\n")

                    detail.specifications?.forEach { group ->
                        sb.append("【 ${group.title} 】\n")
                        group.specs?.forEach { spec ->
                            val vals = spec.val_?.joinToString(", ") ?: ""
                            sb.append("• ${spec.key}: $vals\n")
                        }
                        sb.append("\n")
                    }

                    AlertDialog.Builder(this@PhoneSpecsActivity)
                        .setTitle(detail.phone_name)
                        .setMessage(sb.toString().trim())
                        .setPositiveButton("Close", null)
                        .show()
                } else {
                    toast("Could not retrieve specifications from source.")
                }
            } catch (e: Exception) {
                toast("Error loading details: ${e.localizedMessage}")
            } finally {
                b.progress.visibility = View.GONE
            }
        }
    }
}
