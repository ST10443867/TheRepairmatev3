package com.example.therepairmate.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.example.therepairmate.data.*
import com.example.therepairmate.databinding.ActivityBookBinding
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZonedDateTime

class
BookRepairActivity : AppCompatActivity() {

    companion object { const val EXTRA_ISSUE = "issue" }
    private lateinit var b: ActivityBookBinding
    private var estimate = EstimateService.get("Laptop", "Screen")
    private var selected: Slot? = null
    private var date: LocalDate? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityBookBinding.inflate(layoutInflater)
        setContentView(b.root)
        addBackBar()

        b.spDevice.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, EstimateService.DEVICES)
        b.spIssue.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, EstimateService.ISSUES)
        b.etModel.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, EstimateService.PHONE_MODELS))
        b.etModel.threshold = 1
        intent.getStringExtra(EXTRA_ISSUE)?.let { issue ->
            val idx = EstimateService.ISSUES.indexOf(issue)
            if (idx >= 0) b.spIssue.setSelection(idx)
        }
        val listener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) = updateEstimate()
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        b.spDevice.onItemSelectedListener = listener
        b.spIssue.onItemSelectedListener = listener
        updateEstimate()

        b.btnDate.setOnClickListener { pickDate() }
        b.btnSubmit.setOnClickListener { submit() }
    }

    private fun updateEstimate() {
        estimate = EstimateService.get(b.spDevice.selectedItem.toString(), b.spIssue.selectedItem.toString())
        b.tvEstimate.text = "Estimated: R${estimate.min} – R${estimate.max} • ${estimate.duration}\n" +
                "Preliminary only. Final price is confirmed after diagnosis."
    }

    private fun pickDate() {
        val now = LocalDate.now(SlotGenerator.ZONE)
        DatePickerDialog(this, { _, y, m, d -> date = LocalDate.of(y, m + 1, d); loadSlots() },
            now.year, now.monthValue - 1, now.dayOfMonth).apply {
            datePicker.minDate = System.currentTimeMillis() - 1000
        }.show()
    }

    private fun loadSlots() {
        val d = date ?: return
        b.btnDate.text = d.toString()
        selected = null
        b.chipSlots.removeAllViews()
        lifecycleScope.launch {
            try {
                val booked = Repo.bookedSlotIds(d.toString())
                val slots = SlotGenerator.slotsFor(d, ZonedDateTime.now(SlotGenerator.ZONE), booked)
                b.tvSlotsHint.text = if (slots.isEmpty()) "Closed on weekends. Pick a weekday." else "Select a time (greyed = unavailable)"
                slots.forEach { s ->
                    b.chipSlots.addView(Chip(this@BookRepairActivity).apply {
                        text = "${s.start}–${s.end}"
                        isCheckable = true
                        isEnabled = s.available
                        setOnCheckedChangeListener { _, checked -> selected = if (checked) s else if (selected == s) null else selected }
                    })
                }
            } catch (e: Exception) { b.tvError.text = "Could not load slots: ${e.message}" }
        }
    }

    private fun submit() {
        val model = b.etModel.text.toString().trim()
        val issue = b.etDescription.text.toString().trim()
        val slot = selected
        b.tvError.text = ""
        if (model.isEmpty()) return err("Enter your device model")
        if (issue.isEmpty()) return err("Describe the issue")
        if (slot == null) return err("Choose a date and time slot")
        val type = b.spDevice.selectedItem.toString()
        val issueType = b.spIssue.selectedItem.toString()
        lifecycleScope.launch {
            b.progress.visibility = View.VISIBLE; b.btnSubmit.isEnabled = false
            try {
                val r = Repo.book(model, type, issue, Diagnostic(type, issueType, mapOf("device" to type, "symptom" to issueType)),
                    estimate, slot, b.etNotes.text.toString().trim())
                when (r) {
                    is BookResult.Success -> {
                        Toast.makeText(this@BookRepairActivity, "Booked ${r.ticketId}", Toast.LENGTH_LONG).show()
                        startActivity(Intent(this@BookRepairActivity, RepairDetailActivity::class.java)
                            .putExtra(RepairDetailActivity.EXTRA_ID, r.ticketId))
                        finish()
                    }
                    BookResult.SlotTaken -> { err("That slot was just taken. Please choose another."); loadSlots() }
                }
            } catch (e: Exception) { err(e.message ?: "Booking failed") }
            finally { b.progress.visibility = View.GONE; b.btnSubmit.isEnabled = true }
        }
    }

    private fun err(m: String) { b.tvError.text = m }
}
