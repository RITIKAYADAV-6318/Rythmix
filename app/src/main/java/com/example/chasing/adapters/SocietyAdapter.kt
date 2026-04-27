package com.example.chasing.adapters

import android.content.Intent
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.chasing.R
import com.example.chasing.activities.ChatActivity
import com.example.chasing.models.Society
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SocietyAdapter(private val societyList: List<Society>) :
    RecyclerView.Adapter<SocietyAdapter.SocietyViewHolder>() {

    class SocietyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tvSocietyName)
        val desc: TextView = view.findViewById(R.id.tvSocietyDesc)
        val btnJoin: Button = view.findViewById(R.id.btnJoinSociety)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SocietyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_society, parent, false)
        return SocietyViewHolder(view)
    }

    override fun onBindViewHolder(holder: SocietyViewHolder, position: Int) {
        val society = societyList[position]
        holder.name.text = society.name
        holder.desc.text = society.description

        val context = holder.itemView.context
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // Always set button background to black as requested
        holder.btnJoin.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.black))
        holder.btnJoin.setTextColor(ContextCompat.getColor(context, R.color.white))

        // Check if user is a member
        if (society.members.containsKey(uid)) {
            holder.btnJoin.text = "Joined (Open Chat)"
            holder.btnJoin.setOnClickListener {
                val intent = Intent(context, ChatActivity::class.java)
                intent.putExtra("SOCIETY_ID", society.id)
                intent.putExtra("SOCIETY_NAME", society.name)
                context.startActivity(intent)
            }
        } else {
            holder.btnJoin.text = "Join Society"
            holder.btnJoin.setOnClickListener {
                joinSociety(uid, society, holder.btnJoin)
            }
        }
    }

    private fun joinSociety(uid: String, society: Society, button: Button) {
        val db = FirebaseDatabase.getInstance()
        // Update user's joined societies
        db.getReference("users").child(uid).child("joinedSocieties").child(society.id).setValue(true)
        // Update society's members
        db.getReference("societies").child(society.id).child("members").child(uid).setValue(true)
            .addOnSuccessListener {
                Toast.makeText(button.context, "Welcome to ${society.name}!", Toast.LENGTH_SHORT).show()
                // The UI will refresh automatically due to the ValueEventListener in SocietyActivity
            }
    }

    override fun getItemCount() = societyList.size
}