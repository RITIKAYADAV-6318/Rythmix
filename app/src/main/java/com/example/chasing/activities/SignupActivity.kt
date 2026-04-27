package com.example.chasing.activities

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.chasing.MainActivity
import com.example.chasing.R
import com.example.chasing.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SignupActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance()

        val nameField = findViewById<EditText>(R.id.signupName)
        val emailField = findViewById<EditText>(R.id.signupEmail)
        val collegeIdField = findViewById<EditText>(R.id.signupCollegeId)
        val passwordField = findViewById<EditText>(R.id.signupPassword)
        val roleGroup = findViewById<RadioGroup>(R.id.signupRoleGroup)
        val signupBtn = findViewById<Button>(R.id.signupBtn)

        signupBtn.setOnClickListener {
            val name = nameField.text.toString().trim()
            val email = emailField.text.toString().trim()
            val collegeId = collegeIdField.text.toString().trim()
            val password = passwordField.text.toString().trim()
            val selectedRoleId = roleGroup.checkedRadioButtonId

            if (name.isEmpty() || email.isEmpty() || collegeId.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 🔥 Logic Change: Users are signed up as "student" by default.
            // If they pick "society_member", they are still signed up but restricted until approved.
            val selectedRole = if (selectedRoleId == R.id.signupStudentRadio) "student" else "pending_member"

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid ?: ""
                        val user = User(userId, name, collegeId, email, selectedRole)

                        db.getReference("users").child(userId).setValue(user)
                            .addOnSuccessListener {
                                if (selectedRole == "pending_member") {
                                    Toast.makeText(this, "Request sent for Society Member approval", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show()
                                }
                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                            }
                    } else {
                        Toast.makeText(this, "Auth Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}