package com.example.therepairmate.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.therepairmate.R
import com.example.therepairmate.data.*
import com.example.therepairmate.databinding.ActivityRepairsBinding
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class RepairsActivity : BaseActivity() {
    companion object { const val EXTRA_ADMIN = "admin" }

    private lateinit var b: ActivityRepairsBinding
    private var all = listOf<Repair>()
    private var animated = false
    private val admin by lazy { intent.getBooleanExtra(EXTRA_ADMIN, false) }
    private val adapter = RepairAdapter { r -> if (admin) adminMenu(r) else open(r.id) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityRepairsBinding.inflate(layoutInflater)
        setContentView(b.root)
        addBackBar()
        b.tvTitle.text = if (admin) getString(R.string.all_repairs) else getString(R.string.my_repairs)
        b.tvTitle.setOnClickListener { b.recycler.smoothScrollToTop() }
        b.recycler.layoutManager = LinearLayoutManager(this)
        b.recycler.adapter = adapter

        if (admin) {
            b.llAdminFilters.visibility = View.VISIBLE
            val filterOptions = arrayOf("All", "Active", "Unassigned") + RepairStatus.values().map { it.label(this) }
            val sortOptions = arrayOf("Newest first", "Oldest first", "Ticket ID")
            b.spinnerFilter.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, filterOptions)
            b.spinnerSort.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sortOptions)

            val filterStatus = intent.getStringExtra("filter_status")
            val filterType = intent.getStringExtra("filter_type")

            when {
                filterType == "active" -> b.spinnerFilter.setSelection(1)
                filterType == "unassigned" -> b.spinnerFilter.setSelection(2)
                filterStatus != null -> {
                    val idx = RepairStatus.values().indexOfFirst { it.key == filterStatus }
                    if (idx >= 0) b.spinnerFilter.setSelection(idx + 3)
                }
            }

            val listener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) = filter()
                override fun onNothingSelected(p0: AdapterView<*>?) {}
            }
            b.spinnerFilter.onItemSelectedListener = listener
            b.spinnerSort.onItemSelectedListener = listener
        }

        b.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = filter()
            override fun beforeTextChanged(s: CharSequence?, a: Int, c: Int, d: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, c: Int, d: Int) {}
        })

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                (if (admin) Repo.allRepairs() else Repo.myRepairs())
                    .catch { b.tvEmpty.text = getString(R.string.load_repairs_error, it.message); b.tvEmpty.visibility = View.VISIBLE }
                    .collect { all = it; filter() }
            }
        }
    }

    private fun filter() {
        val q = b.etSearch.text.toString().trim().lowercase()
        val filterSelection = if (admin) b.spinnerFilter.selectedItemPosition else 0
        val sortSelection = if (admin) b.spinnerSort.selectedItemPosition else 0
        val filterTech = intent.getStringExtra("filter_tech")

        var list = all.filter { r ->
            val matchSearch = q.isEmpty() || r.ticketId.lowercase().contains(q) || r.deviceModel.lowercase().contains(q) || r.statusEnum.label(this).lowercase().contains(q)
            val matchTech = filterTech == null || r.techAssigned == filterTech
            val matchFilter = when (filterSelection) {
                1 -> Stats.isActive(r) // Active
                2 -> Stats.isActive(r) && r.techAssigned.isNullOrBlank() // Unassigned
                else -> {
                    if (filterSelection >= 3) {
                        val statusIdx = filterSelection - 3
                        val status = RepairStatus.values().getOrNull(statusIdx)
                        status == null || r.statusEnum == status
                    } else true
                }
            }
            matchSearch && matchTech && matchFilter
        }

        list = when (sortSelection) {
            1 -> list.sortedBy { it.dateCreated?.seconds ?: 0 } // Oldest first
            2 -> list.sortedBy { it.ticketId } // Ticket ID
            else -> list.sortedByDescending { it.dateCreated?.seconds ?: Long.MAX_VALUE } // Newest first
        }

        adapter.submitList(list)
        if (!animated && list.isNotEmpty()) { animated = true; b.recycler.scheduleLayoutAnimation() }
        b.tvEmpty.text = getString(R.string.no_repairs_found)
        b.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun open(id: String) =
        startActivity(Intent(this, RepairDetailActivity::class.java).putExtra(RepairDetailActivity.EXTRA_ID, id))

    private fun adminMenu(r: Repair) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle(r.ticketId)
            .setItems(arrayOf("Open details", "Change status", "Assign technician")) { _, i ->
                when (i) { 0 -> open(r.id); 1 -> pickStatus(r); 2 -> pickTech(r) }
            }.show()
    }

    private fun pickStatus(r: Repair) {
        val statuses = RepairStatus.entries.toTypedArray()
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle("Status")
            .setSingleChoiceItems(statuses.map { it.label(this) }.toTypedArray(), r.statusEnum.ordinal) { d, w ->
                runUpdate { Repo.updateStatus(r.id, statuses[w]) }; d.dismiss()
            }.show()
    }

    private fun pickTech(r: Repair) {
        val active = all.filter { it.statusEnum != RepairStatus.COMPLETED && it.statusEnum != RepairStatus.CANCELLED }
        val names = Repo.TECHNICIANS + "Unassigned"
        val labels = Repo.TECHNICIANS.map { t -> "$t (${active.count { it.techAssigned == t }} active)" } + "Unassigned"
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle("Assign technician")
            .setItems(labels.toTypedArray()) { _, w ->
                runUpdate { Repo.assignTech(r.id, if (names[w] == "Unassigned") null else names[w]) }
            }.show()
    }

    private fun runUpdate(block: suspend () -> Unit) = lifecycleScope.launch {
        try { block(); Toast.makeText(this@RepairsActivity, "Updated", Toast.LENGTH_SHORT).show() }
        catch (e: Exception) { Toast.makeText(this@RepairsActivity, "Failed: ${e.message}", Toast.LENGTH_LONG).show() }
    }
}
