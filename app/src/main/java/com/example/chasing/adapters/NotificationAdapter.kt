package com.example.chasing.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.chasing.R
import com.example.chasing.models.Notification
import com.google.firebase.database.FirebaseDatabase

class NotificationAdapter(private val notifList: List<Notification>) :
    RecyclerView.Adapter<NotificationAdapter.NotifViewHolder>() {

    class NotifViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val message: TextView = view.findViewById(R.id.tvNotifMessage)
        val actionsLayout: LinearLayout = view.findViewById(R.id.layoutApprovalActions)
        val btnApprove: Button = view.findViewById(R.id.btnApprove)
        val btnReject: Button = view.findViewById(R.id.btnReject)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotifViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotifViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotifViewHolder, position: Int) {
        val notif = notifList[position]
        holder.message.text = "${notif.fromUserName} ${notif.message}"

        if (notif.type == "approval_request" && notif.status == "pending") {
            holder.actionsLayout.visibility = View.VISIBLE
            holder.btnApprove.setOnClickListener { approveUser(notif) }
            holder.btnReject.setOnClickListener { rejectUser(notif) }
        } else {
            holder.actionsLayout.visibility = View.GONE
        }
    }

    private fun approveUser(notif: Notification) {
        val db = FirebaseDatabase.getInstance()
        // 1. Update user role to society_member
        db.getReference("users").child(notif.fromUserId).child("role").setValue("society_member")
        // 2. Update notification status
        db.getReference("notifications").child(notif.toUserId).child(notif.id).child("status").setValue("approved")
        // 3. Send back an "approved" notification to the student
        val replyId = db.getReference("notifications").child(notif.fromUserId).push().key ?: return
        val reply = Notification(
            id = replyId,
            toUserId = notif.fromUserId,
            fromUserId = notif.toUserId,
            fromUserName = "System",
            type = "approved",
            message = "Your request to be a Society Member has been approved!",
            timestamp = System.currentTimeMillis()
        )
        db.getReference("notifications").child(notif.fromUserId).child(replyId).setValue(reply)
    }

    private fun rejectUser(notif: Notification) {
        val db = FirebaseDatabase.getInstance()
        db.getReference("notifications").child(notif.toUserId).child(notif.id).child("status").setValue("rejected")
    }

    override fun getItemCount() = notifList.size
}