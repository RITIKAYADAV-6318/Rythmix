package com.example.chasing

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chasing.activities.ChatActivity
import com.example.chasing.activities.LoginActivity
import com.example.chasing.activities.NotificationsActivity
import com.example.chasing.activities.ProfileActivity
import com.example.chasing.activities.SocietyActivity
import com.example.chasing.adapters.EventAdapter
import com.example.chasing.adapters.MusicPostAdapter
import com.example.chasing.models.Event
import com.example.chasing.models.MusicPost
import com.example.chasing.models.User
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseDatabase
    private lateinit var storage: FirebaseStorage
    
    private lateinit var eventAdapter: EventAdapter
    private val eventList = mutableListOf<Event>()
    
    private lateinit var musicAdapter: MusicPostAdapter
    private val musicList = mutableListOf<MusicPost>()

    private lateinit var homeLayout: View
    private lateinit var eventsLayout: LinearLayout
    private lateinit var uploadLayout: LinearLayout
    private lateinit var addEventFab: FloatingActionButton
    private lateinit var btnExploreSocieties: Button
    
    private val PICK_MEDIA_REQUEST = 99
    private val STORAGE_PERMISSION_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()

        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        initViews()
        setupNav()
        fetchEvents()
        fetchMusicPosts()
        
        checkStoragePermission()
    }

    private fun initViews() {
        homeLayout = findViewById(R.id.home_layout)
        eventsLayout = findViewById(R.id.events_layout)
        uploadLayout = findViewById(R.id.upload_layout)
        addEventFab = findViewById(R.id.addEventFab)
        btnExploreSocieties = findViewById(R.id.btnExploreSocieties)
        
        findViewById<RecyclerView>(R.id.eventsRecyclerView).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            eventAdapter = EventAdapter(eventList)
            adapter = eventAdapter
        }

        findViewById<RecyclerView>(R.id.musicPostsRecyclerView).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            musicAdapter = MusicPostAdapter(musicList)
            adapter = musicAdapter
        }

        btnExploreSocieties.setOnClickListener {
            startActivity(Intent(this, SocietyActivity::class.java))
        }

        addEventFab.setOnClickListener { showAddEventDialog() }
        
        findViewById<Button>(R.id.btnAddNewUpload).setOnClickListener {
            showUploadDialog()
        }

        findViewById<ImageView>(R.id.btnBellNotifications).setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }
    }

    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VIDEO), STORAGE_PERMISSION_CODE)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), STORAGE_PERMISSION_CODE)
            }
        }
    }

    private fun setupNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_home
        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> showLayout(homeLayout)
                R.id.nav_events -> {
                    showLayout(eventsLayout)
                    checkAndShowFab()
                }
                R.id.nav_upload -> showLayout(uploadLayout)
                R.id.nav_profile -> startActivity(Intent(this, ProfileActivity::class.java))
            }
            true
        }
    }

    private fun showLayout(layout: View) {
        homeLayout.visibility = View.GONE
        eventsLayout.visibility = View.GONE
        uploadLayout.visibility = View.GONE
        layout.visibility = View.VISIBLE
        if (layout != eventsLayout) addEventFab.hide()
    }

    private fun checkAndShowFab() {
        db.getReference("users").child(auth.currentUser!!.uid).child("role")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(s: DataSnapshot) {
                    if (s.getValue(String::class.java) == "society_member") addEventFab.show()
                }
                override fun onCancelled(e: DatabaseError) {}
            })
    }

    private fun fetchEvents() {
        db.getReference("events").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                eventList.clear()
                s.children.forEach { it.getValue(Event::class.java)?.let { eventList.add(it) } }
                eventList.reverse()
                eventAdapter.notifyDataSetChanged()
            }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    private fun fetchMusicPosts() {
        db.getReference("music_posts").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                musicList.clear()
                s.children.forEach { it.getValue(MusicPost::class.java)?.let { musicList.add(it) } }
                musicList.reverse()
                musicAdapter.notifyDataSetChanged()
            }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    private fun showUploadDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_upload_media, null)
        val etTitle = view.findViewById<EditText>(R.id.etUploadTitle)
        val rgType = view.findViewById<RadioGroup>(R.id.rgMediaType)
        
        AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton("Choose File") { _, _ ->
                val title = etTitle.text.toString().trim()
                val type = if (rgType.checkedRadioButtonId == R.id.rbAudio) "audio" else "video"
                
                if (title.isNotEmpty()) {
                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply { 
                        this.type = if (type == "audio") "audio/*" else "video/*" 
                        addCategory(Intent.CATEGORY_OPENABLE)
                    }
                    startActivityForResult(Intent.createChooser(intent, "Select $type"), PICK_MEDIA_REQUEST)
                    getSharedPreferences("Temp", MODE_PRIVATE).edit()
                        .putString("title", title)
                        .putString("type", type)
                        .apply()
                } else {
                    Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_MEDIA_REQUEST && resultCode == Activity.RESULT_OK && data?.data != null) {
            val prefs = getSharedPreferences("Temp", MODE_PRIVATE)
            val title = prefs.getString("title", "Untitled") ?: "Untitled"
            val type = prefs.getString("type", "audio") ?: "audio"
            uploadMediaFile(data.data!!, title, type)
        }
    }

    private fun uploadMediaFile(fileUri: Uri, title: String, type: String) {
        val uid = auth.currentUser!!.uid
        val postId = db.getReference("music_posts").push().key ?: return
        
        // 🛠️ Robust Storage Path: We use a simple path to avoid character issues
        val extension = if (type == "audio") "mp3" else "mp4"
        val storageRef = storage.reference.child("media/$postId.$extension")

        val progressDialog = AlertDialog.Builder(this)
            .setMessage("Uploading $type... Please wait.")
            .setCancelable(false)
            .show()

        // 🔥 UPLOAD PROCESS
        storageRef.putFile(fileUri)
            .addOnSuccessListener { taskSnapshot ->
                // Once upload is complete, get the download URL safely
                taskSnapshot.metadata?.reference?.downloadUrl?.addOnSuccessListener { downloadUri ->
                    db.getReference("users").child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(s: DataSnapshot) {
                            val user = s.getValue(User::class.java)
                            val post = MusicPost(
                                id = postId,
                                userId = uid,
                                userName = user?.name ?: "Artist",
                                userProfilePic = user?.profilePic ?: "",
                                title = title,
                                fileUrl = downloadUri.toString(),
                                fileType = type,
                                timestamp = System.currentTimeMillis()
                            )
                            db.getReference("music_posts").child(postId).setValue(post)
                                .addOnSuccessListener {
                                    progressDialog.dismiss()
                                    Toast.makeText(this@MainActivity, "Shared Successfully!", Toast.LENGTH_SHORT).show()
                                }
                        }
                        override fun onCancelled(e: DatabaseError) { progressDialog.dismiss() }
                    })
                }?.addOnFailureListener {
                    progressDialog.dismiss()
                    Toast.makeText(this, "Failed to get download link", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showAddEventDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_event, null)
        val etTitle = dialogView.findViewById<EditText>(R.id.etEventTitle)
        val etDate = dialogView.findViewById<EditText>(R.id.etEventDate)
        val etDesc = dialogView.findViewById<EditText>(R.id.etEventDesc)
        val etLocation = dialogView.findViewById<EditText>(R.id.etEventLocation)

        AlertDialog.Builder(this)
            .setTitle("New Event")
            .setView(dialogView)
            .setPositiveButton("Post") { _, _ ->
                val title = etTitle.text.toString().trim()
                val date = etDate.text.toString().trim()
                val desc = etDesc.text.toString().trim()
                val loc = etLocation.text.toString().trim()

                if (title.isNotEmpty() && date.isNotEmpty()) {
                    val eventId = db.getReference("events").push().key ?: return@setPositiveButton
                    val event = Event(eventId, title, date, desc, loc, auth.currentUser!!.uid)
                    db.getReference("events").child(eventId).setValue(event)
                        .addOnSuccessListener { Toast.makeText(this@MainActivity, "Event Posted!", Toast.LENGTH_SHORT).show() }
                } else {
                    Toast.makeText(this, "Title and Date are required", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}