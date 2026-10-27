package com.example.therepairmate.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.therepairmate.data.Repair
import com.example.therepairmate.databinding.ItemRepairBinding

class RepairAdapter(private val onClick: (Repair) -> Unit) :
    ListAdapter<Repair, RepairAdapter.VH>(Diff) {

    class VH(val b: ItemRepairBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(ItemRepairBinding.inflate(LayoutInflater.from(p.context), p, false))

    override fun onBindViewHolder(h: VH, i: Int) {
        val r = getItem(i)
        h.b.tvTicket.text = r.ticketId
        h.b.tvDevice.text = "${r.deviceModel} • ${r.issueDescription}"
        h.b.tvStatus.text = r.statusEnum.label
        h.b.tvTech.text = "Technician: ${r.techAssigned ?: "Unassigned"}"
        h.b.root.setOnClickListener { onClick(r) }
    }

    object Diff : DiffUtil.ItemCallback<Repair>() {
        override fun areItemsTheSame(a: Repair, b: Repair) = a.id == b.id
        override fun areContentsTheSame(a: Repair, b: Repair) = a == b
    }
}
