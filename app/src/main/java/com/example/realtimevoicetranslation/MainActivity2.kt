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
import com.google.cloud.translate.Language
import com.google.cloud.translate.Translate
import com.google.cloud.translate.TranslateOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity2 : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityMainBinding

    private val speechRecognizer: SpeechRecognizer by lazy {
        SpeechRecognizer.createSpeechRecognizer(
            this
        )
    }

    private var userValueTranslated = ""

    private lateinit var textToSpeech: TextToSpeech

    var translateLanguageList = mutableListOf<String>()
    private var languageList = mutableListOf<Language>()

    private var outPutLanguage = ""

    private lateinit var translate: Translate

    private lateinit var adapter: ArrayAdapter<String>

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        getPermissionOverO(this)

        textToSpeech = TextToSpeech(this, this, "com.google.android.tts")

        initializeCloudTranslator()

        adapter = ArrayAdapter(
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
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val userInputValue = binding.inputTextField.text.toString()
                translateTextUsingCloudApi(userInputValue, outPutLanguage)
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
                val userInputValue = binding.inputTextField.text.toString()
                translateTextUsingCloudApi(userInputValue, outPutLanguage)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // write code to perform some action
            }
        }


    }// OnCreate ends here

    /**
     * Function to initialise Cloud translator
     */
    private fun initializeCloudTranslator() {
        showLoader()
        CoroutineScope(Dispatchers.IO).launch {
            val credentials =
                GoogleCredentials.fromStream(resources.openRawResource(R.raw.credentials))
            translate = TranslateOptions.newBuilder().setCredentials(credentials).build().service
            languageList = translate.listSupportedLanguages()
            translateLanguageList =
                languageList.map { language -> language.name }.toMutableList()
            outPutLanguage = translateLanguageList[0]

            runOnUiThread {
                adapter.addAll(translateLanguageList)
                adapter.notifyDataSetChanged()
                hideLoader()
            }
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

            override fun onError(error: Int) {
                Toast.makeText(this@MainActivity2, getErrorText(error), Toast.LENGTH_SHORT).show()
            }

            override fun onResults(bundle: Bundle?) {
                bundle?.let {
                    val result = it.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    binding.inputTextField.setText(result?.get(0))
                }
            }

            override fun onPartialResults(bundle: Bundle?) {
                bundle?.let {
                    val result = it.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    binding.inputTextField.setText(result?.get(0))
                }
            }

            override fun onEvent(p0: Int, p1: Bundle?) {

            }
        })

        speechRecognizer.startListening(intent)
    }


    /**
     * Function to get language code of selected language
     */
    private fun getLanguageCode(outPutLanguage: String): String? {
        return languageList.find { it.name == outPutLanguage }?.code
    }


    /**
     * Function to translate text to selected language using cloud translator
     */
    private fun translateTextUsingCloudApi(text: String, outPutLanguage: String) {
        try {
            if (text.isNotEmpty()) {
                showLoader()
                val sourceLang = "en" // for English
                val targetLang = getLanguageCode(outPutLanguage)

                CoroutineScope(Dispatchers.IO).launch {
                    val translation = translate.translate(
                        text, Translate.TranslateOption.targetLanguage(targetLang),
                        Translate.TranslateOption.sourceLanguage(sourceLang)
                    )
                    val output = translation.translatedText

                    runOnUiThread {
                        binding.outputTextField.setText(output)
                        hideLoader()
                        speak(output)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ERROR", "${e.printStackTrace()}")
        }
    }

    /**
     * Function to speak the text
     */
    private fun speak(text: String) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun getErrorText(error: Int): String {
        return when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No match"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
            SpeechRecognizer.ERROR_SERVER -> "error from server"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Didn't understand, please try again."
        }
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

    private fun showLoader() {
        binding.loading.visibility = View.VISIBLE
    }

    private fun hideLoader() {
        binding.loading.visibility = View.GONE
    }

}