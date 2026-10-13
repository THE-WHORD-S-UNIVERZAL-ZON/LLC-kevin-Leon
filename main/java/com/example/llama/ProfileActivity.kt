package com.example.llama

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val silver = getColor(R.color.uz_silver_bright)
        val muted = getColor(R.color.uz_text_secondary)
        val background = getColor(R.color.uz_background)

        fun label(text: String, size: Float, color: Int) = TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
        }

        fun field(hintText: String, multiline: Boolean = false) =
            EditText(this).apply {
                hint = hintText
                textSize = 16f
                setTextColor(silver)
                setHintTextColor(muted)
                setSingleLine(!multiline)
                if (multiline) {
                    minLines = 3
                    gravity = android.view.Gravity.TOP
                    inputType = android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                        android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                }
                setPadding(16, 14, 16, 14)
                backgroundTintList = android.content.res.ColorStateList.valueOf(
                    getColor(R.color.uz_outline)
                )
            }

        val name = field("Tu nombre")
        val preferred = field("Cómo quieres que te llame (opcional)")
        val about = field("Tus preferencias o información que quieras compartir (opcional)", true)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 32, 24, 28)
            setBackgroundColor(background)
        }

        content.addView(label("UNIVERSAL-ZON AI", 13f, muted))
        content.addView(label("Configuremos tu perfil", 27f, silver).apply {
            setPadding(0, 14, 0, 8)
        })
        content.addView(label(
            "Hola, soy Alias (Coco), 27. ¿Cómo prefieres que te llame?",
            16f, silver
        ))
        content.addView(label(
            "Estos datos se guardan en este dispositivo. Comparte solo lo que quieras; puedes dejar opcionales los demás campos.",
            14f, muted
        ).apply { setPadding(0, 12, 0, 24) })

        fun addField(title: String, edit: EditText) {
            content.addView(label(title, 14f, silver).apply {
                setPadding(0, 10, 0, 6)
            })
            content.addView(edit, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
        }

        addField("Nombre *", name)
        addField("Nombre preferido", preferred)
        addField("Sobre ti (opcional)", about)

        content.addView(label(
            "No introduzcas contraseñas, datos bancarios ni información que no quieras guardar.",
            12f, muted
        ).apply { setPadding(0, 18, 0, 18) })

        val save = MaterialButton(this).apply {
            text = "Guardar perfil y continuar"
            isEnabled = false
        }
        name.addTextChangedListener(SimpleTextWatcher { save.isEnabled = name.text.toString().trim().isNotEmpty() })
        save.setOnClickListener {
            val userName = name.text.toString().trim()
            if (userName.isBlank()) {
                name.error = "Escribe tu nombre para continuar"
                return@setOnClickListener
            }
            UserProfile.save(
                this,
                userName,
                preferred.text.toString(),
                about.text.toString()
            )
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        content.addView(save, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        val scroll = ScrollView(this).apply {
            addView(content)
            isFillViewport = true
        }
        setContentView(scroll)
    }
}
