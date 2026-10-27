package com.example.therepairmate.ui


import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.therepairmate.R
import com.example.therepairmate.data.*
import com.example.therepairmate.databinding.ActivityAdminBinding
import com.example.therepairmate.databinding.ItemBarBinding
import com.example.therepairmate.databinding.ItemRepairBinding
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.time.LocalTime

class AdminDashboardActivity : AppCompatActivity() {
    private lateinit var b: ActivityAdminBinding
    private var repairs = listOf<Repair>()
    private var firstRender = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(b.root)
        if (Repo.uid == null) {
            startActivity(Intent(this, LoginActivity::class.java)); finish(); return
        }

        b.tvAdminGreeting.text = Greeting.forHour(LocalTime.now().hour)
        b.tvAdminName.setOnClickListener { b.root.smoothScrollToTop() }
        b.tvAdminGreeting.setOnClickListener { b.root.smoothScrollToTop() }
        b.btnManage.pressEffect()
        b.btnManage.setOnClickListener {
            startActivity(Intent(this, RepairsActivity::class.java).putExtra(RepairsActivity.EXTRA_ADMIN, true))
        }
        b.iconLogout.setOnClickListener { confirmSignOut() }

        lifecycleScope.launch {
            // Client-side check is for UX only; Firestore rules enforce admin access on the server.
            val p = try { withTimeout(10_000) { Repo.profile() } } catch (e: Exception) { null }
            if (p?.second != "admin") {
                toast("Admin access required")
                startActivity(Intent(this@AdminDashboardActivity, DashboardActivity::class.java))
                finish()
                return@launch
            }
            b.tvAdminName.text = p.first.ifBlank { "Admin" }
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                Repo.allRepairs()
                    .catch { toast(friendlyError(it)) }
                    .collect { repairs = it; render() }
            }
        }
    }

    private fun render() {
        val s = Stats.compute(repairs, Repo.TECHNICIANS)
        if (firstRender) {
            firstRender = false
            b.kActive.countUp(s.active)
            b.kUnassigned.countUp(s.unassigned)
            b.kReady.countUp(s.readyForPickup)
            b.kCompleted.countUp(s.completed)
        } else {
            b.kActive.text = s.active.toString()
            b.kUnassigned.text = s.unassigned.toString()
            b.kReady.text = s.readyForPickup.toString()
            b.kCompleted.text = s.completed.toString()
        }
        b.tvPipeline.text = if (s.active == 0) "R0" else "R${s.pipelineMin} – R${s.pipelineMax}"

        b.statusList.removeAllViews()
        RepairStatus.values().forEach { st ->
            addBar(b.statusList, st.label, s.byStatus[st] ?: 0, s.total, statusColor(st))
        }

        b.techList.removeAllViews()
        val maxLoad = s.workload.values.maxOrNull() ?: 0
        s.workload.forEach { (tech, n) -> addBar(b.techList, tech, n, maxLoad, R.color.qf_blue) }

        b.unassignedList.removeAllViews()
        val pending = repairs.filter { Stats.isActive(it) && it.techAssigned.isNullOrBlank() }.take(5)
        b.tvAllAssigned.visibility = if (pending.isEmpty()) View.VISIBLE else View.GONE
        pending.forEach { r ->
            val row = ItemRepairBinding.inflate(layoutInflater, b.unassignedList, false)
            row.tvTicket.text = r.ticketId
            row.tvDevice.text = "${r.deviceModel} • ${r.issueDescription}"
            row.tvStatus.text = "${r.statusEnum.label} · tap to assign"
            row.tvTech.text = "Appointment: ${r.scheduledSlot.date} ${r.scheduledSlot.startTime}"
            row.root.pressEffect()
            row.root.setOnClickListener { assign(r) }
            b.unassignedList.addView(row.root)
        }
    }

    private fun statusColor(s: RepairStatus): Int = when (s) {
        RepairStatus.RECEIVED, RepairStatus.REPAIRING -> R.color.qf_blue
        RepairStatus.DIAGNOSING, RepairStatus.TESTING -> R.color.qf_purple
        RepairStatus.PARTS_ORDERED -> R.color.qf_warning
        RepairStatus.READY_FOR_PICKUP, RepairStatus.COMPLETED -> R.color.qf_success
        RepairStatus.CANCELLED -> R.color.qf_danger
    }

    private fun addBar(parent: LinearLayout, label: String, count: Int, max: Int, colorRes: Int) {
        val row = ItemBarBinding.inflate(layoutInflater, parent, false)
        row.tvBarLabel.text = label
        row.tvBarCount.text = count.toString()
        row.barIndicator.max = maxOf(max, 1)
        row.barIndicator.setIndicatorColor(ContextCompat.getColor(this, colorRes))
        row.barIndicator.setProgressCompat(count, false)
        parent.addView(row.root)
    }

    private fun assign(r: Repair) {
        val active = repairs.filter { Stats.isActive(it) }
        val labels = Repo.TECHNICIANS.map { t -> "$t (${active.count { it.techAssigned == t }} active)" }
        AlertDialog.Builder(this).setTitle("Assign ${r.ticketId}")
            .setItems(labels.toTypedArray()) { _, i ->
                lifecycleScope.launch {
                    try {
                        withTimeout(10_000) { Repo.assignTech(r.id.ifBlank { r.ticketId }, Repo.TECHNICIANS[i]) }
                        toast("Assigned to ${Repo.TECHNICIANS[i]}")
                    } catch (e: Exception) { toast(friendlyError(e)) }
                }
            }.show()
    }
}