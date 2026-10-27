package com.example.therepairmate.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.therepairmate.R
import com.example.therepairmate.data.*
import com.example.therepairmate.databinding.ActivityDashboardBinding
import com.example.therepairmate.databinding.ItemRepairBinding
import com.example.therepairmate.databinding.ItemServiceBinding
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.time.LocalTime


class DashboardActivity : AppCompatActivity() {
    private lateinit var b: ActivityDashboardBinding
    private var heroId: String? = null

    private val serviceIcons = mapOf(
        "Screen" to R.drawable.ic_phone_android, "Battery" to R.drawable.ic_battery,
        "Charging" to R.drawable.ic_bolt, "Performance" to R.drawable.ic_chart,
        "Software" to R.drawable.ic_code, "Water damage" to R.drawable.ic_water, "Other" to R.drawable.ic_build
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(b.root)
        if (Repo.uid == null) { toLogin(); return }

        b.tvGreeting.text = Greeting.forHour(LocalTime.now().hour)
        b.tvName.setOnClickListener { b.scrollView.smoothScrollToTop() }
        b.tvGreeting.setOnClickListener { b.scrollView.smoothScrollToTop() }
        wireClicks()
        b.tvHeroStatus.pulse()
        buildServices()
        loadProfile()
        observeRepairs()
    }

    private fun wireClicks() {
        val book = { startActivity(Intent(this, BookRepairActivity::class.java)) }
        val repairs = { startActivity(Intent(this, RepairsActivity::class.java)) }
        b.iconBook.setOnClickListener { book() }
        b.navBook.setOnClickListener { book() }
        b.btnHeroBook.setOnClickListener { book() }
        b.iconRepairs.setOnClickListener { repairs() }
        b.navRepairs.setOnClickListener { repairs() }
        b.tvSeeAll.setOnClickListener { repairs() }
        b.iconCall.setOnClickListener { call() }
        b.btnCall.setOnClickListener { call() }
        b.btnWhatsapp.setOnClickListener { whatsapp() }
        b.iconMail.setOnClickListener { mail() }
        b.btnMail.setOnClickListener { mail() }
        b.navAccount.setOnClickListener { confirmSignOut() }
        b.btnHeroTrack.setOnClickListener { heroId?.let { openRepair(it) } }
        listOf(b.btnHeroBook, b.btnHeroTrack, b.btnCall, b.btnWhatsapp, b.btnMail, b.navHome, b.navBook, b.navRepairs, b.navAccount)
            .forEach { it.pressEffect() }
    }

    private fun loadProfile() {
        lifecycleScope.launch {
            val p = try { withTimeout(10_000) { Repo.ensureProfile(); Repo.profile() } } catch (e: Exception) { null }
            if (p?.second == "admin") {
                startActivity(Intent(this@DashboardActivity, AdminDashboardActivity::class.java))
                finish()
                return@launch
            }
            val name = listOf(p?.first, Repo.email?.substringBefore('@')).firstOrNull { !it.isNullOrBlank() } ?: "there"
            b.tvName.text = name
            b.tvAvatar.text = name.take(1).uppercase()
        }
    }

    private fun observeRepairs() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                Repo.myRepairs().catch { renderRepairs(emptyList()) }.collect { renderRepairs(it) }
            }
        }
    }

    private fun renderRepairs(list: List<Repair>) {
        val active = list.firstOrNull { Stats.isActive(it) }
        b.cardActive.visibility = if (active != null) View.VISIBLE else View.GONE
        b.cardEmpty.visibility = if (active == null) View.VISIBLE else View.GONE
        if (active != null) {
            heroId = active.id.ifBlank { active.ticketId }
            val step = Stats.stepOf(active)
            b.tvHeroTicket.text = active.ticketId
            b.tvHeroDevice.text = "${active.deviceModel} • ${active.issueDescription}"
            b.tvHeroStatus.text = active.statusEnum.label
            b.heroProgress.setProgressCompat(step * 100 / 6, true)
            b.tvHeroStep.text = "Step $step of 6 • ${active.techAssigned?.let { "Technician: $it" } ?: "Technician not assigned yet"}"
        } else heroId = null

        b.recentList.removeAllViews()
        list.take(3).forEach { r ->
            val row = ItemRepairBinding.inflate(layoutInflater, b.recentList, false)
            row.tvTicket.text = r.ticketId
            row.tvDevice.text = "${r.deviceModel} • ${r.issueDescription}"
            row.tvStatus.text = r.statusEnum.label
            row.tvTech.text = "Technician: ${r.techAssigned ?: "Unassigned"}"
            row.root.pressEffect()
            row.root.setOnClickListener { openRepair(r.id.ifBlank { r.ticketId }) }
            b.recentList.addView(row.root)
        }
        b.tvNoRecent.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun buildServices() {
        EstimateService.ISSUES.forEach { issue ->
            val s = ItemServiceBinding.inflate(layoutInflater, b.servicesRow, false)
            s.tvServiceName.text = issue
            s.tvServicePrice.text = "Est. from R${EstimateService.get("Smartphone", issue).min}"
            s.ivService.setImageResource(serviceIcons[issue] ?: R.drawable.ic_build)
            s.root.pressEffect()
            s.root.setOnClickListener {
                startActivity(Intent(this, BookRepairActivity::class.java).putExtra(BookRepairActivity.EXTRA_ISSUE, issue))
            }
            b.servicesRow.addView(s.root)
        }
        b.servicesRow.children.toList().staggerIn()
    }

    private fun openRepair(id: String) =
        startActivity(Intent(this, RepairDetailActivity::class.java).putExtra(RepairDetailActivity.EXTRA_ID, id))

    private fun call() = safeStart(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Config.SUPPORT_PHONE}")))
    private fun whatsapp() = safeStart(Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=27661614699&text=Hello%20The%20Repairmate,%20I%20need%20help%20with%20my%20repair.")))
    private fun mail() = safeStart(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${Config.SUPPORT_EMAIL}")))

    private fun safeStart(i: Intent) {
        try { startActivity(i) } catch (e: ActivityNotFoundException) { toast("No app available to do that") }
    }

    private fun toLogin() { startActivity(Intent(this, LoginActivity::class.java)); finish() }
}