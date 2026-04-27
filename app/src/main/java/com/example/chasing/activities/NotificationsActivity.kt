package com.example.chasing.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chasing.R
import com.example.chasing.adapters.NotificationAdapter
import com.example.chasing.models.Notification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class NotificationsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseDatabase
    private lateinit var adapter: NotificationAdapter
    private val notifList = mutableListOf<Notification>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance()

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.notifToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        val recyclerView = findViewById<RecyclerView>(R.id.notificationsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = NotificationAdapter(notifList)
        recyclerView.adapter = adapter

        fetchNotifications()
    }

    private fun fetchNotifications() {
        val uid = auth.currentUser?.uid ?: return
        db.getReference("notifications").child(uid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                notifList.clear()
                for (notifSnap in snapshot.children) {
                    val notif = notifSnap.getValue(Notification::class.java)
                    if (notif != null) {
                        notifList.add(notif)
                    }
                }
                notifList.reverse()
                adapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}