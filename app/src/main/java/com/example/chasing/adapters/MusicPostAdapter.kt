package com.example.chasing.adapters

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chasing.R
import com.example.chasing.models.MusicPost
import com.example.chasing.models.Notification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MusicPostAdapter(private val postList: List<MusicPost>) :
    RecyclerView.Adapter<MusicPostAdapter.PostViewHolder>() {

    class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userName: TextView = view.findViewById(R.id.postUserName)
        val userPic: ImageView = view.findViewById(R.id.postUserPic)
        val title: TextView = view.findViewById(R.id.postTitle)
        val btnPlay: Button = view.findViewById(R.id.btnPlayMusic)
        val btnLike: TextView = view.findViewById(R.id.btnLike)
        val btnComment: TextView = view.findViewById(R.id.btnComment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_music_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postList[position]
        holder.userName.text = post.userName
        holder.title.text = post.title
        
        // Load user profile pic
        if (post.userProfilePic.isNotEmpty()) {
            Glide.with(holder.itemView.context).load(post.userProfilePic).circleCrop().into(holder.userPic)
        }

        // Like logic
        val likesCount = post.likes.size
        holder.btnLike.text = "❤️ Like ($likesCount)"

        holder.btnLike.setOnClickListener {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
            val db = FirebaseDatabase.getInstance()
            val ref = db.getReference("music_posts").child(post.id).child("likes")
            
            if (post.likes.containsKey(uid)) {
                ref.child(uid).removeValue()
            } else {
                ref.child(uid).setValue(true).addOnSuccessListener {
                    // Send notification to post owner
                    if (post.userId != uid) {
                        sendLikeNotification(post, uid)
                    }
                }
            }
        }

        holder.btnPlay.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(post.fileUrl)
            holder.itemView.context.startActivity(intent)
        }
    }

    private fun sendLikeNotification(post: MusicPost, fromUid: String) {
        val db = FirebaseDatabase.getInstance()
        // Get current user's name
        db.getReference("users").child(fromUid).child("name").get().addOnSuccessListener { nameSnap ->
            val fromName = nameSnap.getValue(String::class.java) ?: "Someone"
            val notifId = db.getReference("notifications").child(post.userId).push().key ?: return@addOnSuccessListener
            val notif = Notification(
                id = notifId,
                toUserId = post.userId,
                fromUserId = fromUid,
                fromUserName = fromName,
                type = "like",
                message = "liked your post: ${post.title}",
                timestamp = System.currentTimeMillis()
            )
            db.getReference("notifications").child(post.userId).child(notifId).setValue(notif)
        }
    }

    override fun getItemCount() = postList.size
}