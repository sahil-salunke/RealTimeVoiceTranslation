package com.example.realtimevoicetranslation

//import com.google.mlkit.nl.translate.Translation

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.translate.Translate
import com.google.cloud.translate.TranslateOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class TranslatorActivity : AppCompatActivity() {

    private lateinit var textToSpeech: TextToSpeech
    private lateinit var editText: EditText
    private lateinit var speakButton: Button
    private val API_KEY = "AIzaSyBtYYf_g1h6443vMCGtuWHVVoKrAzr-I3Y"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_translator)

        editText = findViewById(R.id.editfield)
        speakButton = findViewById(R.id.speakButton)

        speakButton.setOnClickListener {
            val text = editText.text.toString().trim()
            if (text.isNotEmpty()) {
                val sourceLang = "en" // for English
                val targetLang = "mr" // for Marathi
                val credentials =
                    GoogleCredentials.fromStream(resources.openRawResource(R.raw.credentials))

                CoroutineScope(Dispatchers.IO).launch {
                    val translate =
                        TranslateOptions.newBuilder().setCredentials(credentials).build().service
                    val translation = translate.translate(
                        text, Translate.TranslateOption.targetLanguage(targetLang),
                        Translate.TranslateOption.sourceLanguage(sourceLang)
                    )
                    val output = translation.translatedText

                    runOnUiThread {
                        editText.setText(output)
                    }
                }
            }
        }
    }


}