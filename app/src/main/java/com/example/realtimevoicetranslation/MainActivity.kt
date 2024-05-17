package com.example.realtimevoicetranslation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.example.realtimevoicetranslation.databinding.ActivityMainBinding
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.translate.Translate
import com.google.cloud.translate.TranslateOptions
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityMainBinding

    private val speechRecognizer: SpeechRecognizer by lazy {
        SpeechRecognizer.createSpeechRecognizer(
            this
        )
    }

    private var userValueTranslated = ""

    private lateinit var textToSpeech: TextToSpeech

    val translateLanguageList =
        arrayListOf("MARATHI", "HINDI", "TELUGU", "KANNADA", "GUJARATI", "TAMIL")

    private var outPutLanguage = translateLanguageList[0]

    private lateinit var translators: Translator

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        getPermissionOverO(this)

        textToSpeech = TextToSpeech(this, this, "com.google.android.tts")

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item, translateLanguageList
        )
        binding.spinner.adapter = adapter


        binding.mike.setOnTouchListener { _, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_UP -> {
                    speechRecognizer.stopListening()
                    binding.mike.setImageResource(R.drawable.ic_mic_black_off)
                    return@setOnTouchListener true
                }

                MotionEvent.ACTION_DOWN -> {
                    binding.mike.setImageResource(R.drawable.ic_mic_red_on)
                    startListen()
                    return@setOnTouchListener true
                }

                else -> {
                    return@setOnTouchListener true
                }
            }
        }


        binding.inputTextField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val userInputValue = binding.inputTextField.text.toString()
//                translateTextToSelectedLanguage(translators, userInputValue)
                translateTextUsingCloudApi(userInputValue, outPutLanguage)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })

        binding.speak.setOnClickListener {
            Log.e("onTranslateSuccess", userValueTranslated)
            speak(userValueTranslated)
        }


        binding.outputLanguage.setOnClickListener {
            binding.spinner.performClick()
        }

        binding.spinner.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View, position: Int, id: Long
            ) {
                binding.outputLanguage.text = translateLanguageList[position]
                outPutLanguage = translateLanguageList[position]
                translators = translatorFn(outPutLanguage)

                val userInputValue = binding.inputTextField.text.toString()
//                translateTextToSelectedLanguage(translators, userInputValue)
                translateTextUsingCloudApi(userInputValue, outPutLanguage)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // write code to perform some action
            }
        }


    }// OnCreate ends here


    /**
     * Function to initialize translator with selected language
     */
    private fun translatorFn(toTranslateLanguage: String): Translator {
        val lang = getLanguageParam(toTranslateLanguage)
        // Creating an translator
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(Locale.ENGLISH.toString())
            .setTargetLanguage(lang)
            .build()
        return Translation.getClient(options)
    }

    private fun getLanguageParam(toTranslateLanguage: String) = when (toTranslateLanguage) {
        "HINDI" -> TranslateLanguage.HINDI
        "TELUGU" -> TranslateLanguage.TELUGU
        "KANNADA" -> TranslateLanguage.KANNADA
        "GUJARATI" -> TranslateLanguage.GUJARATI
        "MARATHI" -> TranslateLanguage.MARATHI
        "TAMIL" -> TranslateLanguage.TAMIL
        else -> {
            TranslateLanguage.MARATHI
        }
    }

    /**
     * Function to initiate listen functionality
     */
    private fun startListen() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p0: Bundle?) {

            }

            override fun onBeginningOfSpeech() {
                binding.inputTextField.hint = "Listening..."
            }

            override fun onRmsChanged(p0: Float) {

            }

            override fun onBufferReceived(p0: ByteArray?) {

            }

            override fun onEndOfSpeech() {

            }

            override fun onError(p0: Int) {

            }

            override fun onResults(bundle: Bundle?) {
                bundle?.let {
                    val result = it.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    binding.inputTextField.setText(result?.get(0))
                }
            }

            override fun onPartialResults(p0: Bundle?) {

            }

            override fun onEvent(p0: Int, p1: Bundle?) {

            }
        })

        speechRecognizer.startListening(intent)
    }



    private fun translateTextUsingCloudApi(text: String, outPutLanguage: String){
        if (text.isNotEmpty()) {
//            val sourceLang = "en" // for English
            val targetLang = getLanguageParam(outPutLanguage)
            val credentials =
                GoogleCredentials.fromStream(resources.openRawResource(R.raw.credentials))

            CoroutineScope(Dispatchers.IO).launch {
                val translate =
                    TranslateOptions.newBuilder().setCredentials(credentials).build().service
                val translation = translate.translate(
                    text, Translate.TranslateOption.targetLanguage(targetLang),
                    Translate.TranslateOption.sourceLanguage(TranslateLanguage.ENGLISH)
                )
                val output = translation.translatedText

                runOnUiThread {
                    binding.outputTextField.setText(output)
                    speak(output)
                }
            }
        }
    }





    /**
     * Function to translate and display text
     */
    private fun translateTextToSelectedLanguage(translators: Translator, userInputValue: String) {
        binding.loading.visibility = View.VISIBLE
        val conditions = DownloadConditions.Builder().requireWifi().build()
        translators.downloadModelIfNeeded(conditions).addOnSuccessListener {
            // Model downloaded successfully. Okay to start translating.
            // (Set a flag, unhidden the translation UI, etc.)
            translators.translate(userInputValue).addOnSuccessListener { translatedText ->
                // Translation successful
                binding.loading.visibility = View.GONE
                userValueTranslated = translatedText
                binding.outputTextField.setText(userValueTranslated)
                Log.d("onTranslateSuccess", userValueTranslated)
                CoroutineScope(Dispatchers.Main).launch {
                    delay(1000)
                    speak(translatedText)
                }
            }.addOnFailureListener { exception ->
                // Error
                binding.loading.visibility = View.GONE
                exception.localizedMessage?.let { it1 ->
                    Log.d(
                        "onTranslateErrors", it1
                    )
                }
            }
        }.addOnFailureListener { exception ->
            binding.loading.visibility = View.GONE
            exception.localizedMessage?.let { it1 ->
                Log.d(
                    "onTranslateErrors2", it1
                )
            }
        }
    }

    /**
     * Function to speak the text
     */
    private fun speak(text: String) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }


    /**
     * Function to get required permissions
     */
    private fun getPermissionOverO(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
            } else {
                allowPermission.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private val allowPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            it?.let {
                if (it) {
                    Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
                }
            }
        }

    /**
     * Function to initialize text to speech listener
     */
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Language data is missing or the language is not supported.
                Toast.makeText(this, "Language not supported", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Initialization failed.
            Toast.makeText(this, "Something is wrong", Toast.LENGTH_SHORT).show()
        }
    }

}