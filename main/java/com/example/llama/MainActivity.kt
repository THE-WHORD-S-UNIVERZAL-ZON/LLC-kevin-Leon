package com.example.llama

import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.ImageButton
import android.speech.tts.TextToSpeech
import android.media.MediaPlayer
import java.net.URLEncoder
import java.util.Locale
import com.arm.aichat.AiChat
import com.arm.aichat.InferenceEngine
import com.arm.aichat.gguf.GgufMetadata
import com.arm.aichat.gguf.GgufMetadataReader
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class MainActivity : AppCompatActivity() {

    // Android views
    private lateinit var ggufTv: TextView
    private lateinit var messagesRv: RecyclerView
    private lateinit var userInputEt: EditText
    private lateinit var userActionFab: FloatingActionButton
    private lateinit var btnMic: ImageButton
    private lateinit var drawerLayout: DrawerLayout

    // Arm AI Chat inference engine
    private lateinit var engine: InferenceEngine
    private var generationJob: Job? = null
    private var tts: TextToSpeech? = null
    private var mediaPlayer: MediaPlayer? = null
    private var selectedVoice: String = "es-PR-VictorNeural"

    // Conversation states
    private var isModelReady = false
    private val messages = mutableListOf<Message>()
    private val lastAssistantMsg = StringBuilder()
    private val messageAdapter = MessageAdapter(messages)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        // View model boilerplate and state management is out of this basic sample's scope
        onBackPressedDispatcher.addCallback { Log.w(TAG, "Ignore back press for simplicity") }

        // Find views
        ggufTv = findViewById(R.id.gguf)
        messagesRv = findViewById(R.id.messages)
        messagesRv.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        messagesRv.adapter = messageAdapter
        userInputEt = findViewById(R.id.user_input)
        userActionFab = findViewById(R.id.fab)
        btnMic = findViewById(R.id.btn_mic)
        btnMic.setOnClickListener { iniciarDictadoPorVoz() }
        drawerLayout = findViewById(R.id.drawer_layout)
        configurarMenuLateral()
        // Inicializar voz en español de Coco (Text-to-Speech)
        mediaPlayer = MediaPlayer()
        val prefsVoz = getSharedPreferences("coco_prefs", MODE_PRIVATE)
        selectedVoice = prefsVoz.getString("selected_voice", "es-PR-VictorNeural") ?: "es-PR-VictorNeural"

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("es", "ES")
                tts?.setSpeechRate(1.0f)
            }
        }

        val nombreUsuario = UserProfile.preferredName(this)
            .ifBlank { UserProfile.name(this) }
        if (nombreUsuario.isNotBlank()) {
            messages.add(
                Message(
                    UUID.randomUUID().toString(),
                    "¡Hola, $nombreUsuario! Soy Alias (Coco), 27, tu asistente de UNIVERSAL-ZON AI. ¿En qué puedo ayudarte hoy?",
                    false
                )
            )
            messageAdapter.notifyItemInserted(messages.lastIndex)
            messagesRv.scrollToPosition(messages.lastIndex)
        }

        // Arm AI Chat initialization
        lifecycleScope.launch(Dispatchers.Default) {
            engine = AiChat.getInferenceEngine(applicationContext)
        }

        // Upon CTA button tapped
        userActionFab.setOnClickListener {
            if (isModelReady) {
                // If model is ready, validate input and send to engine
                handleUserInput()
            } else {
                // Otherwise, prompt user to select a GGUF metadata on the device
                getContent.launch(arrayOf("*/*"))
            }
        }
    }


    private fun configurarMenuLateral() {
        findViewById<android.widget.ImageButton>(R.id.menu_button)
            .setOnClickListener {
                drawerLayout.openDrawer(Gravity.START)
            }

        findViewById<MaterialButton>(R.id.nav_new_chat).setOnClickListener {
            drawerLayout.closeDrawer(Gravity.START)
            if (messages.isEmpty()) {
                Toast.makeText(this, "La conversación ya está vacía.", Toast.LENGTH_SHORT).show()
            } else {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Nueva conversación")
                    .setMessage("¿Quieres borrar los mensajes de esta conversación? Esta acción no se puede deshacer.")
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton("Borrar mensajes") { _, _ ->
                        generationJob?.cancel()
                        messages.clear()
                        lastAssistantMsg.clear()
                        messageAdapter.notifyDataSetChanged()
                        userInputEt.isEnabled = isModelReady
                        userActionFab.isEnabled = true
                        Toast.makeText(this, "Conversación nueva.", Toast.LENGTH_SHORT).show()
                    }
                    .show()
            }
        }

        findViewById<MaterialButton>(R.id.nav_storage).setOnClickListener {
            drawerLayout.closeDrawer(Gravity.START)
            MaterialAlertDialogBuilder(this)
                .setTitle("Almacenamiento del historial")
                .setMessage("Por ahora, los mensajes se conservan solo mientras esta sesión de la aplicación permanece activa. El historial permanente y la sincronización en la nube todavía no están implementados.")
                .setPositiveButton("Entendido", null)
                .show()
        }

        findViewById<MaterialButton?>(R.id.nav_voice_selector)?.setOnClickListener {
            drawerLayout.closeDrawer(Gravity.START)
            mostrarSelectorVoz()
        }

        findViewById<android.widget.ImageButton?>(R.id.btn_mic)?.setOnLongClickListener {
            mostrarSelectorVoz()
            true
        }

        findViewById<MaterialButton>(R.id.nav_online_mode).setOnClickListener {
            drawerLayout.closeDrawer(Gravity.START)
            MaterialAlertDialogBuilder(this)
                .setTitle("Modo local y en línea")
                .setMessage("El modo local utiliza el modelo GGUF seleccionado en el dispositivo. La conexión con proveedores de IA en línea aún no está configurada; esta versión no envía tus mensajes a un servicio de IA en la nube.")
                .setPositiveButton("Entendido", null)
                .show()
        }

        findViewById<MaterialButton>(R.id.nav_about).setOnClickListener {
            drawerLayout.closeDrawer(Gravity.START)
            MaterialAlertDialogBuilder(this)
                .setTitle("UNIVERSAL-ZON AI")
                .setMessage(
                    "UNIVERSAL-ZON AI, by THE WHORD'S & UNIVERSAL-ZON LLC\n\n" +
                    "Desarrollado para ofrecer una experiencia de IA local.\n\n" +
                    "Correo: Kether@the-univerzal-zon.company\n" +
                    "Web: the-univerzal-zon.company\n" +
                    "WhatsApp Business: +1 408-627-9350\n\n" +
                    "Los documentos legales requieren revisión profesional."
                )
                .setPositiveButton("Cerrar", null)
                .show()
        }
    }

    private val getContent = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        Log.i(TAG, "Selected file uri:\n $uri")
        uri?.let { handleSelectedModel(it) }
    }

    /**
     * Handles the file Uri from [getContent] result
     */
    private fun handleSelectedModel(uri: Uri) {
        // Update UI states
        userActionFab.isEnabled = false
        userInputEt.hint = "Parsing GGUF..."
        ggufTv.text = "Parsing metadata from selected file \n$uri"

        lifecycleScope.launch(Dispatchers.IO) {
            // Parse GGUF metadata
            Log.i(TAG, "Parsing GGUF metadata...")
            contentResolver.openInputStream(uri)?.use {
                GgufMetadataReader.create().readStructuredMetadata(it)
            }?.let { metadata ->
                // Update UI to show GGUF metadata to user
                Log.i(TAG, "GGUF parsed: \n$metadata")
                withContext(Dispatchers.Main) {
                    ggufTv.text = metadata.toString()
                }

                // Ensure the model file is available
                val modelName = metadata.filename() + FILE_EXTENSION_GGUF
                contentResolver.openInputStream(uri)?.use { input ->
                    ensureModelFile(modelName, input)
                }?.let { modelFile ->
                    loadModel(modelName, modelFile)

                    withContext(Dispatchers.Main) {
                        isModelReady = true
                        userInputEt.hint = "Type and send a message!"
                        userInputEt.isEnabled = true
                        userActionFab.setImageResource(R.drawable.outline_send_24)
                        userActionFab.isEnabled = true
                    }
                }
            }
        }
    }

    /**
     * Prepare the model file within app's private storage
     */
    private suspend fun ensureModelFile(modelName: String, input: InputStream) =
        withContext(Dispatchers.IO) {
            File(ensureModelsDirectory(), modelName).also { file ->
                // Copy the file into local storage if not yet done
                if (!file.exists()) {
                    Log.i(TAG, "Start copying file to $modelName")
                    withContext(Dispatchers.Main) {
                        userInputEt.hint = "Copying file..."
                    }

                    FileOutputStream(file).use { input.copyTo(it) }
                    Log.i(TAG, "Finished copying file to $modelName")
                } else {
                    Log.i(TAG, "File already exists $modelName")
                }
            }
        }

    /**
     * Load the model file from the app private storage
     */
    private suspend fun loadModel(modelName: String, modelFile: File) =
        withContext(Dispatchers.IO) {
            Log.i(TAG, "Loading model $modelName")
            withContext(Dispatchers.Main) {
                userInputEt.hint = "Loading model..."
            }
            engine.loadModel(modelFile.path)
            engine.setSystemPrompt(UserProfile.systemPrompt(this@MainActivity))
        }

    /**
     * Validate and send the user message into [InferenceEngine]
     */
    private fun handleUserInput() {
        userInputEt.text.toString().also { userMsg ->
            if (userMsg.isEmpty()) {
                Toast.makeText(this, "Input message is empty!", Toast.LENGTH_SHORT).show()
            } else {
                userInputEt.text = null
                userInputEt.isEnabled = false
                userActionFab.isEnabled = false

                // Update message states
                messages.add(Message(UUID.randomUUID().toString(), userMsg, true))
                lastAssistantMsg.clear()
                messages.add(Message(UUID.randomUUID().toString(), lastAssistantMsg.toString(), false))

                generationJob = lifecycleScope.launch(Dispatchers.Default) {
                    engine.sendUserPrompt(userMsg)
                        .onCompletion {
                            withContext(Dispatchers.Main) {
                                userInputEt.isEnabled = true
                                userActionFab.isEnabled = true
                                if (lastAssistantMsg.isNotBlank()) {
                                    reproducirAudioNeuronal(lastAssistantMsg.toString())
                                }
                            }
                        }.collect { token ->
                            withContext(Dispatchers.Main) {
                                val messageCount = messages.size
                                check(messageCount > 0 && !messages[messageCount - 1].isUser)

                                messages.removeAt(messageCount - 1).copy(
                                    content = lastAssistantMsg.append(token).toString()
                                ).let { messages.add(it) }

                                messageAdapter.notifyItemChanged(messages.size - 1)
                            }
                        }
                }
            }
        }
    }

    /**
     * Run a benchmark with the model file
     */
    @Deprecated("This benchmark doesn't accurately indicate GUI performance expected by app developers")
    private suspend fun runBenchmark(modelName: String, modelFile: File) =
        withContext(Dispatchers.Default) {
            Log.i(TAG, "Starts benchmarking $modelName")
            withContext(Dispatchers.Main) {
                userInputEt.hint = "Running benchmark..."
            }
            engine.bench(
                pp=BENCH_PROMPT_PROCESSING_TOKENS,
                tg=BENCH_TOKEN_GENERATION_TOKENS,
                pl=BENCH_SEQUENCE,
                nr=BENCH_REPETITION
            ).let { result ->
                messages.add(Message(UUID.randomUUID().toString(), result, false))
                withContext(Dispatchers.Main) {
                    messageAdapter.notifyItemChanged(messages.size - 1)
                }
            }
        }

    /**
     * Create the `models` directory if not exist.
     */
    private fun ensureModelsDirectory() =
        File(filesDir, DIRECTORY_MODELS).also {
            if (it.exists() && !it.isDirectory) { it.delete() }
            if (!it.exists()) { it.mkdir() }
        }

    override fun onStop() {
        generationJob?.cancel()
        tts?.stop()
        super.onStop()
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        mediaPlayer?.release()
        mediaPlayer = null
        engine.destroy()
        super.onDestroy()
    }

    
    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                val current = userInputEt.text.toString()
                val finalMsg = if (current.isBlank()) spokenText else "$current $spokenText"
                userInputEt.setText(finalMsg)
                userInputEt.setSelection(finalMsg.length)
            }
        }
    }

    private fun iniciarDictadoPorVoz() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla ahora con Alias (Coco)...")
        }
        try {
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "El dictado por voz no está disponible en este dispositivo.", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName

        private const val DIRECTORY_MODELS = "models"
        private const val FILE_EXTENSION_GGUF = ".gguf"

        private const val BENCH_PROMPT_PROCESSING_TOKENS = 512
        private const val BENCH_TOKEN_GENERATION_TOKENS = 128
        private const val BENCH_SEQUENCE = 1
        private const val BENCH_REPETITION = 3
    }
}

fun GgufMetadata.filename() = when {
    basic.name != null -> {
        basic.name?.let { name ->
            basic.sizeLabel?.let { size ->
                "$name-$size"
            } ?: name
        }
    }
    architecture?.architecture != null -> {
        architecture?.architecture?.let { arch ->
            basic.uuid?.let { uuid ->
                "$arch-$uuid"
            } ?: "$arch-${System.currentTimeMillis()}"
        }
    }
    else -> {
        "model-${System.currentTimeMillis().toHexString()}"
    }

    private fun mostrarSelectorVoz() {
        val opciones = arrayOf("Víctor (Masculino - Puerto Rico)", "Karina (Femenino - Puerto Rico)")
        val codigos = arrayOf("es-PR-VictorNeural", "es-PR-KarinaNeural")
        val prefs = getSharedPreferences("coco_prefs", MODE_PRIVATE)
        val actual = codigos.indexOf(prefs.getString("selected_voice", "es-PR-VictorNeural"))
        val seleccionado = if (actual >= 0) actual else 0

        MaterialAlertDialogBuilder(this)
            .setTitle("Voz de Alias Coco")
            .setSingleChoiceItems(opciones, seleccionado) { dialog, which ->
                val nuevaVoz = codigos[which]
                prefs.edit().putString("selected_voice", nuevaVoz).apply()
                selectedVoice = nuevaVoz
                Toast.makeText(this, "Voz seleccionada: " + opciones[which], Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun reproducirAudioNeuronal(texto: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val prefs = getSharedPreferences("coco_prefs", MODE_PRIVATE)
                val voz = prefs.getString("selected_voice", "es-PR-VictorNeural") ?: "es-PR-VictorNeural"
                val encoded = java.net.URLEncoder.encode(texto, "UTF-8")
                val url = "http://127.0.0.1:8000/api/tts?texto=$encoded&voz=$voz"

                withContext(Dispatchers.Main) {
                    try {
                        mediaPlayer?.reset()
                        mediaPlayer?.setDataSource(url)
                        mediaPlayer?.setOnErrorListener { _, _, _ ->
                            tts?.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "coco_voice")
                            true
                        }
                        mediaPlayer?.setOnPreparedListener { mp ->
                            mp.start()
                        }
                        mediaPlayer?.prepareAsync()
                    } catch (e: Exception) {
                        Log.e("AliasCoco", "Fallo MediaPlayer, usando TTS respaldo", e)
                        tts?.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "coco_voice")
                    }
                }
            } catch (e: Exception) {
                Log.e("AliasCoco", "Fallo red/servidor, usando TTS respaldo", e)
                withContext(Dispatchers.Main) {
                    tts?.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "coco_voice")
                }
            }
        }
    }
}
