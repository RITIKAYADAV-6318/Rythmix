package com.example.chasing.adapters

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chasing.R
import com.example.chasing.activities.ProfileActivity
import com.example.chasing.models.ChatMessage
import com.example.chasing.models.Notification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ChatAdapter(private val chatList: List<ChatMessage>, private val societyId: String) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val container: LinearLayout = view.findViewById(R.id.chatMessageContainer)
        val senderName: TextView = view.findViewById(R.id.chatSenderName)
        val messageText: TextView = view.findViewById(R.id.chatMessageText)
        val profilePic: ImageView = view.findViewById(R.id.chatProfilePic)
        val bubbleCard: CardView = view.findViewById(R.id.chatBubbleCard)
        val profilePicCard: CardView = view.findViewById(R.id.profilePicCard)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chat = chatList[position]
        val context = holder.itemView.context
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        
        holder.senderName.text = chat.senderName
        
        if (chat.senderProfilePic.isNotEmpty()) {
            Glide.with(context).load(chat.senderProfilePic).circleCrop().into(holder.profilePic)
        } else {
            holder.profilePic.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        if (chat.senderId == currentUserId) {
            holder.container.gravity = Gravity.END
            holder.bubbleCard.setCardBackgroundColor(context.getColor(R.color.chatMyBubble))
            holder.messageText.setTextColor(context.getColor(R.color.white))
            holder.profilePicCard.visibility = View.GONE
            holder.senderName.visibility = View.GONE
        } else {
            holder.container.gravity = Gravity.START
            holder.bubbleCard.setCardBackgroundColor(context.getColor(R.color.chatOthersBubble))
            holder.messageText.setTextColor(context.getColor(R.color.black))
            holder.profilePicCard.visibility = View.VISIBLE
            holder.senderName.visibility = View.VISIBLE
        }

        holder.itemView.setOnLongClickListener {
            showDeleteOptions(context, chat)
            true
        }

        val openProfile = View.OnClickListener {
            val intent = Intent(it.context, ProfileActivity::class.java)
            intent.putExtra("VIEW_USER_ID", chat.senderId)
            it.context.startActivity(intent)
        }
        holder.profilePic.setOnClickListener(openProfile)
        holder.senderName.setOnClickListener(openProfile)

        val message = chat.message
        val spannable = SpannableString(message)
        val words = message.split(" ")
        var currentPos = 0
        
        val db = FirebaseDatabase.getInstance()
        
        for (word in words) {
            if (word.startsWith("@") && word.length > 1) {
                val taggedName = word.substring(1)
                val start = message.indexOf(word, currentPos)
                if (start != -1) {
                    val end = start + word.length
                    spannable.setSpan(
                        ForegroundColorSpan(context.getColor(R.color.tagColor)),
                        start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    
                    // 🔥 Logic to send cloud notification to database when tagged
                    // We search for users with this name
                    db.getReference("users").orderByChild("name").equalTo(taggedName)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                for (userSnap in snapshot.children) {
                                    val taggedUserId = userSnap.key ?: continue
                                    if (taggedUserId != chat.senderId) {
                                        sendTagNotificationToDb(taggedUserId, chat)
                                    }
                                }
                            }
                            override fun onCancelled(error: DatabaseError) {}
                        })
                }
            }
            currentPos += word.length + 1
        }
        holder.messageText.text = spannable
    }

    private fun sendTagNotificationToDb(toUserId: String, chat: ChatMessage) {
        val db = FirebaseDatabase.getInstance()
        val notifId = db.getReference("notifications").child(toUserId).push().key ?: return
        val notif = Notification(
            id = notifId,
            toUserId = toUserId,
            fromUserId = chat.senderId,
            fromUserName = chat.senderName,
            type = "tag",
            message = "tagged you in a chat: ${chat.message}",
            timestamp = System.currentTimeMillis()
        )
        db.getReference("notifications").child(toUserId).child(notifId).setValue(notif)
    }

    private fun showDeleteOptions(context: Context, chat: ChatMessage) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        
        FirebaseDatabase.getInstance().getReference("users").child(currentUserId).child("role")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val role = snapshot.getValue(String::class.java)
                    if (chat.senderId == currentUserId || role == "society_member") {
                        AlertDialog.Builder(context)
                            .setTitle("Delete Message?")
                            .setMessage("Are you sure you want to delete this message?")
                            .setPositiveButton("Delete") { _, _ ->
                                FirebaseDatabase.getInstance().getReference("chats")
                                    .child(societyId).child(chat.id).removeValue()
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    override fun getItemCount() = chatList.size
}