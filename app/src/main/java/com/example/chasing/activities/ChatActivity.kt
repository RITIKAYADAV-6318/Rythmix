package com.example.chasing.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chasing.R
import com.example.chasing.adapters.ChatAdapter
import com.example.chasing.models.ChatMessage
import com.example.chasing.models.Notification
import com.example.chasing.models.Society
import com.example.chasing.models.User
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ChatActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseDatabase
    private lateinit var chatAdapter: ChatAdapter
    private val chatList = mutableListOf<ChatMessage>()
    private var societyId: String = ""
    private var societyName: String = ""
    private var societyCreatorId: String = ""
    private var userName: String = "Anonymous"
    private var userProfilePic: String = ""
    private var userRole: String = "student"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance()

        societyId = intent.getStringExtra("SOCIETY_ID") ?: "general"
        societyName = intent.getStringExtra("SOCIETY_NAME") ?: "Society Chat"

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.chatToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = societyName
        
        toolbar.popupTheme = R.style.Theme_Chasing_PopupOverlay

        val recyclerView = findViewById<RecyclerView>(R.id.chatRecyclerView)
        val etMessage = findViewById<EditText>(R.id.etChatMessage)
        val btnSend = findViewById<ImageButton>(R.id.btnSendChat)

        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        recyclerView.layoutManager = layoutManager
        chatAdapter = ChatAdapter(chatList, societyId)
        recyclerView.adapter = chatAdapter

        fetchSocietyDetails()
        fetchUserData()
        listenForMessages()

        btnSend.setOnClickListener {
            val messageText = etMessage.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText)
                etMessage.setText("")
            }
        }
    }

    private fun fetchSocietyDetails() {
        db.getReference("societies").child(societyId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val society = snapshot.getValue(Society::class.java)
                    if (society != null) {
                        societyCreatorId = society.createdBy
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun fetchUserData() {
        val uid = auth.currentUser?.uid ?: return
        db.getReference("users").child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                if (user != null) {
                    userName = user.name
                    userProfilePic = user.profilePic
                    userRole = user.role
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.chat_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_view_members -> {
                showMembersDialog()
                true
            }
            R.id.action_delete_chat -> {
                if (userRole == "society_member") confirmDeleteChat()
                else Toast.makeText(this, "Only members can clear chat", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_delete_society -> {
                if (auth.currentUser?.uid == societyCreatorId) confirmDeleteSociety()
                else Toast.makeText(this, "Only the creator can delete this society", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showMembersDialog() {
        db.getReference("societies").child(societyId).child("members")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val memberIds = snapshot.children.mapNotNull { it.key }
                    if (memberIds.isEmpty()) {
                        Toast.makeText(this@ChatActivity, "No members yet", Toast.LENGTH_SHORT).show()
                        return
                    }

                    val membersList = mutableListOf<Pair<String, String>>()
                    var processedCount = 0
                    for (uid in memberIds) {
                        db.getReference("users").child(uid).child("name")
                            .get().addOnSuccessListener { nameSnap ->
                                val name = nameSnap.getValue(String::class.java) ?: "Unknown"
                                membersList.add(uid to name)
                                processedCount++
                                if (processedCount == memberIds.size) {
                                    membersList.sortBy { it.second }
                                    val namesArray = membersList.map { it.second }.toTypedArray()
                                    MaterialAlertDialogBuilder(this@ChatActivity)
                                        .setTitle("$societyName Members")
                                        .setItems(namesArray) { _, which ->
                                            val selectedUid = membersList[which].first
                                            val intent = Intent(this@ChatActivity, ProfileActivity::class.java)
                                            intent.putExtra("VIEW_USER_ID", selectedUid)
                                            startActivity(intent)
                                        }
                                        .setPositiveButton("Close", null)
                                        .show()
                                }
                            }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun confirmDeleteChat() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Clear Chat?")
            .setMessage("This will delete all messages for everyone in this society.")
            .setPositiveButton("Clear All") { _, _ ->
                db.getReference("chats").child(societyId).removeValue()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDeleteSociety() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Society?")
            .setMessage("This will permanently remove '$societyName'.")
            .setPositiveButton("Delete") { _, _ ->
                db.getReference("chats").child(societyId).removeValue()
                db.getReference("societies").child(societyId).removeValue()
                    .addOnSuccessListener { finish() }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun listenForMessages() {
        db.getReference("chats").child(societyId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    chatList.clear()
                    for (chatSnapshot in snapshot.children) {
                        val message = chatSnapshot.getValue(ChatMessage::class.java)
                        if (message != null) {
                            chatList.add(message)
                        }
                    }
                    chatAdapter.notifyDataSetChanged()
                    if (chatList.isNotEmpty()) {
                        findViewById<RecyclerView>(R.id.chatRecyclerView).smoothScrollToPosition(chatList.size - 1)
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun sendMessage(text: String) {
        val msgRef = db.getReference("chats").child(societyId).push()
        val msgId = msgRef.key ?: ""
        val chatMessage = ChatMessage(
            id = msgId,
            senderId = auth.currentUser?.uid ?: "",
            senderName = userName,
            senderProfilePic = userProfilePic,
            message = text,
            timestamp = System.currentTimeMillis()
        )

        msgRef.setValue(chatMessage)
        
        // 🔥 Trigger Tag Notifications if anyone is @mentioned
        val words = text.split(" ")
        for (word in words) {
            if (word.startsWith("@") && word.length > 1) {
                val taggedName = word.substring(1)
                db.getReference("users").orderByChild("name").equalTo(taggedName)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            for (userSnap in snapshot.children) {
                                val toUid = userSnap.key ?: continue
                                if (toUid != auth.currentUser?.uid) {
                                    sendTagNotif(toUid, text)
                                }
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })
            }
        }
    }

    private fun sendTagNotif(toUserId: String, message: String) {
        val notifId = db.getReference("notifications").child(toUserId).push().key ?: return
        val notif = Notification(
            id = notifId,
            toUserId = toUserId,
            fromUserId = auth.currentUser?.uid ?: "",
            fromUserName = userName,
            type = "tag",
            message = "tagged you in $societyName: $message",
            timestamp = System.currentTimeMillis()
        )
        db.getReference("notifications").child(toUserId).child(notifId).setValue(notif)
    }
}