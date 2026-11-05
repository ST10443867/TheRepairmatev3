package com.example.therepairmate.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.therepairmate.data.PhoneItem
import com.example.therepairmate.databinding.ItemPhoneSpecBinding

class PhoneSpecsAdapter(private val onClick: (PhoneItem) -> Unit) :
    ListAdapter<PhoneItem, PhoneSpecsAdapter.VH>(Diff) {

    class VH(val b: ItemPhoneSpecBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemPhoneSpecBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val phone = getItem(position)
        holder.b.tvPhoneName.text = phone.phone_name
        holder.b.tvBrand.text = phone.brand ?: "Brand"
        holder.b.ivPhone.loadUrl(phone.image)
        holder.b.root.pressEffect()
        holder.b.root.setOnClickListener { onClick(phone) }
    }

    object Diff : DiffUtil.ItemCallback<PhoneItem>() {
        override fun areItemsTheSame(oldItem: PhoneItem, newItem: PhoneItem) = oldItem.slug == newItem.slug
        override fun areContentsTheSame(oldItem: PhoneItem, newItem: PhoneItem) = oldItem == newItem
    }
}
