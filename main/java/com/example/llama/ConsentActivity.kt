package com.example.llama

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ConsentActivity : AppCompatActivity() {
    private lateinit var termsCheckbox: CheckBox
    private lateinit var privacyCheckbox: CheckBox
    private lateinit var continueButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_consent)

        termsCheckbox = findViewById(R.id.consent_terms_checkbox)
        privacyCheckbox = findViewById(R.id.consent_privacy_checkbox)
        continueButton = findViewById(R.id.consent_continue)

        // Never pre-check legal consent.
        termsCheckbox.isChecked = false
        privacyCheckbox.isChecked = false
        continueButton.isEnabled = false

        findViewById<MaterialButton>(R.id.consent_view_terms).setOnClickListener {
            showDocument(getString(R.string.consent_terms_title), LegalDocuments.TERMS)
        }
        findViewById<MaterialButton>(R.id.consent_view_privacy).setOnClickListener {
            showDocument(getString(R.string.consent_privacy_title), LegalDocuments.PRIVACY)
        }

        val updateButton = {
            continueButton.isEnabled = termsCheckbox.isChecked && privacyCheckbox.isChecked
        }
        termsCheckbox.setOnCheckedChangeListener { _, _ -> updateButton() }
        privacyCheckbox.setOnCheckedChangeListener { _, _ -> updateButton() }

        continueButton.setOnClickListener {
            if (!termsCheckbox.isChecked || !privacyCheckbox.isChecked) {
                updateButton()
                return@setOnClickListener
            }

            LegalConsent.saveConsent(this)
            val destination = if (UserProfile.hasProfile(this)) {
                MainActivity::class.java
            } else {
                ProfileActivity::class.java
            }
            startActivity(Intent(this, destination))
            finish()
        }
    }

    private fun showDocument(title: String, content: String) {
        val text = TextView(this).apply {
            text = content.trimIndent()
            textSize = 14f
            setTextColor(getColor(R.color.uz_white))
            setPadding(24, 16, 24, 16)
            setTextIsSelectable(true)
        }
        val scroll = ScrollView(this).apply {
            addView(
                text,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setView(scroll)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}
