package com.example.llama

import android.content.Intent
import android.os.Bundle
import com.google.android.material.button.MaterialButton
import androidx.appcompat.app.AppCompatActivity

class WelcomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        findViewById<MaterialButton>(R.id.welcome_continue).setOnClickListener {
            val destination = when {
                !LegalConsent.hasCurrentConsent(this) -> ConsentActivity::class.java
                !UserProfile.hasProfile(this) -> ProfileActivity::class.java
                else -> MainActivity::class.java
            }
            startActivity(Intent(this, destination))
        }
    }
}
