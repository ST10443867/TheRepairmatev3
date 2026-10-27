package com.example.therepairmate.ui

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.firebase.Timestamp
import com.example.therepairmate.R
import com.example.therepairmate.data.*
import com.example.therepairmate.databinding.ActivityDetailBinding
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class RepairDetailActivity : AppCompatActivity() {
    companion object { const val EXTRA_ID = "id" }
    private lateinit var b: ActivityDetailBinding
    private var summary = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(b.root)
        addBackBar()
        val id = intent.getStringExtra(EXTRA_ID)
        if (id == null) { finish(); return }

        b.btnShare.setOnClickListener {
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, summary), "Share receipt"))
        }
        // Real-time listener: updates the moment an admin changes the status.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                Repo.observeRepair(id).catch { b.tvInfo.text = "Error: ${it.message}" }.collect { it?.let(::render) }
            }
        }
    }

    private fun fmt(t: Timestamp?) =
        t?.toDate()?.let { SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(it) } ?: "—"

    private fun render(r: Repair) {
        b.tvTicket.text = r.ticketId
        b.tvStatus.text = r.statusEnum.label
        val cur = r.statusEnum.roadmapIndex
        b.timeline.removeAllViews()
        RepairStatus.roadmap.forEachIndexed { i, s ->
            val color = when {
                r.statusEnum == RepairStatus.CANCELLED -> 0x55FFFFFF
                i < cur -> ContextCompat.getColor(this, R.color.qf_blue)
                i == cur -> ContextCompat.getColor(this, R.color.qf_purple)
                else -> 0x44FFFFFF
            }
            val dot = ContextCompat.getDrawable(this, R.drawable.dot)!!.mutate().apply { setTint(color) }
            b.timeline.addView(TextView(this).apply {
                text = s.label
                textSize = 16f
                gravity = Gravity.CENTER_VERTICAL
                compoundDrawablePadding = 24
                setPadding(0, 18, 0, 18)
                setTextColor(if (i <= cur) 0xFFFFFFFF.toInt() else 0x66FFFFFF)
                if (i == cur) setTypeface(typeface, Typeface.BOLD)
                setCompoundDrawablesRelativeWithIntrinsicBounds(dot, null, null, null)
            })
        }
        val c = r.estimatedCost
        b.tvInfo.text = """
            Device: ${r.deviceModel} (${r.deviceType})
            Issue: ${r.issueDescription}
            Technician: ${r.techAssigned ?: "Not yet assigned"}
            Appointment: ${r.scheduledSlot.date} ${r.scheduledSlot.startTime}–${r.scheduledSlot.endTime} (${r.scheduledSlot.timezone})
            Estimate: R${c.minimum} – R${c.maximum} (preliminary)
            Created: ${fmt(r.dateCreated)}
            Updated: ${fmt(r.updatedAt)}
        """.trimIndent()
        summary = "The Repairmate by QuickFix Tech Solutions\nTicket: ${r.ticketId}\n${b.tvInfo.text}\nStatus: ${r.statusEnum.label}"
    }
}
