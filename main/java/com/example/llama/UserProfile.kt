package com.example.llama

import android.content.Context

object UserProfile {
    private const val PREFS = "uz_user_profile"
    private const val NAME = "name"
    private const val PREFERRED_NAME = "preferred_name"
    private const val ABOUT = "about"

    fun hasProfile(context: Context): Boolean =
        name(context).isNotBlank()

    fun name(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(NAME, "").orEmpty()

    fun preferredName(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(PREFERRED_NAME, "").orEmpty()

    fun about(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(ABOUT, "").orEmpty()

    fun save(
        context: Context,
        name: String,
        preferredName: String,
        about: String
    ) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(NAME, name.trim())
            .putString(PREFERRED_NAME, preferredName.trim())
            .putString(ABOUT, about.trim())
            .apply()
    }

    fun systemPrompt(context: Context): String {
        val fullName = name(context)
        val nickname = preferredName(context).ifBlank { fullName }
        val details = about(context).ifBlank { "No ha compartido información adicional." }

        return """
            Eres Alias (Coco), de identidad de marca de UNIVERSAL-ZON AI.
            Tu presentación establecida es "Alias (Coco), 27".
            Habla en español salvo que el usuario prefiera otro idioma.
            Sé amable, claro y honesto. No afirmes ser una persona humana.

            PERFIL DEL USUARIO DE ESTE DISPOSITIVO:
            Nombre: $fullName
            Nombre preferido: $nickname
            Información que decidió compartir: $details
            Usa estos datos cuando sean relevantes. No inventes otros datos personales.

            INFORMACIÓN OFICIAL DE LA COMPAÑÍA:
            Nombre: THE WHORD'S & UNIVERSAL-ZON LLC
            Nombre de la aplicación: UNIVERSAL-ZON AI
            Correo: Kether@the-univerzal-zon.company
            Sitio web: the-univerzal-zon.company
            WhatsApp Business: +1 408-627-9350
            Propietario indicado por el usuario: Kevin León Atiles
            No inventes direcciones registradas, servicios, políticas ni hechos no incluidos aquí.
            La conexión con proveedores de IA en línea y la sincronización en la nube aún no están implementadas.
            Distingue siempre la información personal del usuario de la información de la compañía.
        """.trimIndent()
    }
}
