package com.example.chasing.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.chasing.R
import com.example.chasing.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseDatabase
    private lateinit var storage: FirebaseStorage
    private lateinit var profileImage: ImageView
    private val PICK_IMAGE_REQUEST = 71
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()

        profileImage = findViewById(R.id.profileImage)
        val profileName = findViewById<TextView>(R.id.profileName)
        val profileCollegeId = findViewById<TextView>(R.id.profileCollegeId)
        val profileRole = findViewById<TextView>(R.id.profileRole)
        val profileJoinedSocieties = findViewById<TextView>(R.id.profileJoinedSocieties)
        val logoutBtn = findViewById<Button>(R.id.logoutBtn)
        val cardProfileImage = findViewById<View>(R.id.cardProfileImage)

        val userId = intent.getStringExtra("VIEW_USER_ID") ?: auth.currentUser?.uid

        if (userId != null) {
            // Fetch basic profile info
            db.getReference("users").child(userId)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val user = snapshot.getValue(User::class.java)
                        if (user != null) {
                            profileName.text = user.name
                            profileCollegeId.text = "ID: ${user.collegeId}"
                            profileRole.text = "Role: ${user.role.replace("_", " ").uppercase()}"
                            
                            if (user.profilePic.isNotEmpty()) {
                                Glide.with(this@ProfileActivity)
                                    .load(user.profilePic)
                                    .placeholder(android.R.drawable.ic_menu_gallery)
                                    .circleCrop()
                                    .into(profileImage)
                            }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })

            // 🔥 FIXED: Fetch actual Society NAMES instead of encrypted IDs
            db.getReference("users").child(userId).child("joinedSocieties")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val societyIds = snapshot.children.mapNotNull { it.key }
                        if (societyIds.isEmpty()) {
                            profileJoinedSocieties.text = "Not joined any society yet"
                            return
                        }

                        val namesList = mutableListOf<String>()
                        var processedCount = 0
                        for (socId in societyIds) {
                            db.getReference("societies").child(socId).child("name")
                                .get().addOnSuccessListener { nameSnap ->
                                    nameSnap.getValue(String::class.java)?.let { namesList.add(it) }
                                    processedCount++
                                    if (processedCount == societyIds.size) {
                                        profileJoinedSocieties.text = namesList.joinToString(", ")
                                    }
                                }.addOnFailureListener {
                                    processedCount++
                                    if (processedCount == societyIds.size) {
                                        profileJoinedSocieties.text = namesList.joinToString(", ")
                                    }
                                }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
        }

        if (userId == auth.currentUser?.uid) {
            val clickListener = View.OnClickListener { chooseImage() }
            profileImage.setOnClickListener(clickListener)
            cardProfileImage.setOnClickListener(clickListener)
        }

        if (userId != auth.currentUser?.uid) {
            logoutBtn.visibility = View.GONE
        }

        logoutBtn.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun chooseImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            imageUri = data.data
            uploadImage()
        }
    }

    private fun uploadImage() {
        if (imageUri != null) {
            val uid = auth.currentUser?.uid ?: return
            
            val progressDialog = AlertDialog.Builder(this)
                .setMessage("Uploading Profile Picture...")
                .setCancelable(false)
                .show()

            // 🔥 Use a timestamp or keep it simple but ensure the path is solid
            // Adding a small delay or ensuring metadata is handled can help with the "object not found" error
            val storageRef = storage.reference.child("profile_pics/$uid.jpg")
            
            storageRef.putFile(imageUri!!)
                .continueWithTask { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let { throw it }
                    }
                    storageRef.downloadUrl
                }
                .addOnCompleteListener { task ->
                    progressDialog.dismiss()
                    if (task.isSuccessful) {
                        val downloadUri = task.result
                        db.getReference("users").child(uid).child("profilePic").setValue(downloadUri.toString())
                            .addOnSuccessListener {
                                Toast.makeText(this, "Profile Picture Updated!", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this, "Upload failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }
}
