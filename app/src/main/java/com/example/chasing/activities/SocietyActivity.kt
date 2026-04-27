package com.example.chasing.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chasing.R
import com.example.chasing.adapters.SocietyAdapter
import com.example.chasing.models.Society
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class SocietyActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseDatabase
    private lateinit var adapter: SocietyAdapter
    private val societyList = mutableListOf<Society>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_society)

        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance()

        setupToolbar()
        setupRecyclerView()
        checkUserRoleAndSetupFab()
        fetchSocieties()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.societyToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.societiesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = SocietyAdapter(societyList)
        recyclerView.adapter = adapter
    }

    private fun checkUserRoleAndSetupFab() {
        val fab = findViewById<FloatingActionButton>(R.id.addSocietyFab)
        val uid = auth.currentUser?.uid ?: return

        db.getReference("users").child(uid).child("role")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.getValue(String::class.java) == "society_member") {
                        fab.visibility = View.VISIBLE
                        fab.setOnClickListener { showCreateSocietyDialog() }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun fetchSocieties() {
        db.getReference("societies").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                societyList.clear()
                for (socSnapshot in snapshot.children) {
                    val society = socSnapshot.getValue(Society::class.java)
                    if (society != null) {
                        societyList.add(society)
                    }
                }
                adapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun showCreateSocietyDialog() {
        // 🔥 Using a dedicated layout for society creation
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_society, null)
        val etName = dialogView.findViewById<EditText>(R.id.etSocName)
        val etCollege = dialogView.findViewById<EditText>(R.id.etSocCollege)
        val etDesc = dialogView.findViewById<EditText>(R.id.etSocDesc)

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Create") { _, _ ->
                val name = etName.text.toString().trim()
                val college = etCollege.text.toString().trim()
                val desc = etDesc.text.toString().trim()
                if (name.isNotEmpty() && college.isNotEmpty()) {
                    createSociety(name, college, desc)
                } else {
                    Toast.makeText(this, "Name and College are required", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createSociety(name: String, college: String, desc: String) {
        val uid = auth.currentUser?.uid ?: return
        val socId = db.getReference("societies").push().key ?: return
        
        val society = Society(
            id = socId,
            name = name,
            description = desc,
            collegeName = college,
            createdBy = uid,
            members = mapOf(uid to true)
        )

        db.getReference("societies").child(socId).setValue(society)
            .addOnSuccessListener {
                db.getReference("users").child(uid).child("joinedSocieties").child(socId).setValue(true)
                Toast.makeText(this, "Society '$name' Created!", Toast.LENGTH_SHORT).show()
            }
    }
}