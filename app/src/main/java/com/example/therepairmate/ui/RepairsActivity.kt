package com.example.therepairmate.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.therepairmate.data.*
import com.example.therepairmate.databinding.ActivityRepairsBinding
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class RepairsActivity : AppCompatActivity() {
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
        b.tvTitle.text = if (admin) "All repairs" else "My repairs"
        b.tvTitle.setOnClickListener { b.recycler.smoothScrollToTop() }
        b.recycler.layoutManager = LinearLayoutManager(this)
        b.recycler.adapter = adapter
        b.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = filter()
            override fun beforeTextChanged(s: CharSequence?, a: Int, c: Int, d: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, c: Int, d: Int) {}
        })
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                (if (admin) Repo.allRepairs() else Repo.myRepairs())
                    .catch { b.tvEmpty.text = "Could not load repairs: ${it.message}"; b.tvEmpty.visibility = View.VISIBLE }
                    .collect { all = it; filter() }
            }
        }
    }

    private fun filter() {
        val q = b.etSearch.text.toString().trim().lowercase()
        val list = if (q.isEmpty()) all else all.filter {
            it.ticketId.lowercase().contains(q) || it.deviceModel.lowercase().contains(q) || it.statusEnum.label.lowercase().contains(q)
        }
        adapter.submitList(list)
        if (!animated && list.isNotEmpty()) { animated = true; b.recycler.scheduleLayoutAnimation() }
        b.tvEmpty.text = "No repairs found"
        b.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun open(id: String) =
        startActivity(Intent(this, RepairDetailActivity::class.java).putExtra(RepairDetailActivity.EXTRA_ID, id))

    private fun adminMenu(r: Repair) {
        AlertDialog.Builder(this).setTitle(r.ticketId)
            .setItems(arrayOf("Open details", "Change status", "Assign technician")) { _, i ->
                when (i) { 0 -> open(r.id); 1 -> pickStatus(r); 2 -> pickTech(r) }
            }.show()
    }

    private fun pickStatus(r: Repair) {
        val statuses = RepairStatus.entries.toTypedArray()
        AlertDialog.Builder(this).setTitle("Status")
            .setSingleChoiceItems(statuses.map { it.label }.toTypedArray(), r.statusEnum.ordinal) { d, w ->
                runUpdate { Repo.updateStatus(r.id, statuses[w]) }; d.dismiss()
            }.show()
    }

    private fun pickTech(r: Repair) {
        val active = all.filter { it.statusEnum != RepairStatus.COMPLETED && it.statusEnum != RepairStatus.CANCELLED }
        val names = Repo.TECHNICIANS + "Unassigned"
        val labels = Repo.TECHNICIANS.map { t -> "$t (${active.count { it.techAssigned == t }} active)" } + "Unassigned"
        AlertDialog.Builder(this).setTitle("Assign technician")
            .setItems(labels.toTypedArray()) { _, w ->
                runUpdate { Repo.assignTech(r.id, if (names[w] == "Unassigned") null else names[w]) }
            }.show()
    }

    private fun runUpdate(block: suspend () -> Unit) = lifecycleScope.launch {
        try { block(); Toast.makeText(this@RepairsActivity, "Updated", Toast.LENGTH_SHORT).show() }
        catch (e: Exception) { Toast.makeText(this@RepairsActivity, "Failed: ${e.message}", Toast.LENGTH_LONG).show() }
    }
}
