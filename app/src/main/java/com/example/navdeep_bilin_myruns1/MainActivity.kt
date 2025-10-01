package com.example.navdeep_bilin_myruns1

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File


/*

this is the profile screen functionality as a single activity app

here we collect the profile field information, let the user take a camera pic without gallery
and save the profile using SharedPreferences. as well as, any in-progress photo URI is saved as well

patterns referenced from course notes/demo are as follows:
- camera capture with Fileprovider + activity result API
    Lecture: the phone camera and data storage + camerademokotlin
- SharedPreferences for lightweiht ersistence
    Lecture2: lifecycle + persistence
- Scrolview + platform widgets in XML for the form
    Lecture 1-3 UI notes


in the profile we have a static 3-dot menu which does not open anything but can be clicked. in the
btnMenu, it shows a toast so its there but dsn't do anything

*/

class MainActivity : ComponentActivity() {

    // UI references
    private lateinit var imgProfile: ImageView
    private lateinit var btnChangePhoto: Button
    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etClass: EditText
    private lateinit var etMajor: EditText
    private lateinit var rgGender: RadioGroup
    private lateinit var rbFemale: RadioButton
    private lateinit var rbMale: RadioButton
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    // SharedPreferences keys
    private val prefsName = "profile_prefs"
    private val KEY_NAME = "name"
    private val KEY_EMAIL = "email"
    private val KEY_PHONE = "phone"
    private val KEY_GENDER = "gender"          // 0=female, 1=male, -1 unset
    private val KEY_CLASS = "class_year"
    private val KEY_MAJOR = "major"
    private val KEY_PHOTO_URI = "photo_uri"

    // rotation state key for pending camera captures
    private val STATE_PENDING_URI = "state_pending_uri"

    // this is the photo state
    private var pendingPhotoUri: Uri? = null
    private val fileProviderAuthority by lazy { "${packageName}.fileprovider" } // <-- use packageName, no BuildConfig

    // Activity Result APIs
    // TakePicture launcher - writes into the uri we pass it (insipiration from course camera lec)
    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                // if the camera succeeds, show the image immeedaityl
                pendingPhotoUri?.let { imgProfile.setImageURI(it) }
            } else {
                Toast.makeText(this, "Camera canceled", Toast.LENGTH_SHORT).show()
            }
        }

    // permission launcher - request camera and read media
    private val requestPerms =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // layout is based on a Scrollview and LinearLayour form the lab specificcations
        setContentView(R.layout.activity_profile)

        // static 3-dot menu
        findViewById<Button?>(R.id.btnMenu)?.setOnClickListener {
            Toast.makeText(this, "Menu clicked", Toast.LENGTH_SHORT).show()
        }

        bindViews()
        addLightValidation()
        requestFirstRunPermissions()

        // if during camera capture the phone is rotated, it should restore the uri and preview
        if (savedInstanceState != null) {
            pendingPhotoUri = savedInstanceState.getString(STATE_PENDING_URI)?.let(Uri::parse)
            pendingPhotoUri?.let { imgProfile.setImageURI(it) }
        } else {
            // if the profile wasnt saved, then go back to previous first creation or start again
            // cold starts
            loadProfile()
        }

        btnChangePhoto.setOnClickListener { ensureCameraReadyThenLaunch() }
        btnSave.setOnClickListener {
            if (validateAndSave()) Toast.makeText(this, "Profile saved", Toast.LENGTH_SHORT).show()
        }
        btnCancel.setOnClickListener {
            // this allows for reloading saved profile options
            pendingPhotoUri = null
            loadProfile()
            Toast.makeText(this, "Changes discarded", Toast.LENGTH_SHORT).show()
        }
    }

    // -------- helpers --------

    // we kept this as private to support proper readability as well as to keep it all in one spot
    private fun bindViews() {
        imgProfile = findViewById(R.id.imgProfile)
        btnChangePhoto = findViewById(R.id.btnChangePhoto)
        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        etClass = findViewById(R.id.etClass)
        etMajor = findViewById(R.id.etMajor)
        rgGender = findViewById(R.id.rgGender)
        rbFemale = findViewById(R.id.rbFemale)
        rbMale = findViewById(R.id.rbMale)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)
    }


    /*

    what we do here is that we ask for the runtime permissions on the first run
    camera permissions, files permissions are here.
    course guidance was used around the media access however the FileProvider itself doesn't
    require storage permission to read or write the app file
     */
    private fun requestFirstRunPermissions() {
        val wants = mutableListOf(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= 33) {
            wants += Manifest.permission.READ_MEDIA_IMAGES
        } else {
            wants += Manifest.permission.READ_EXTERNAL_STORAGE
        }
        val need = wants.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (need.isNotEmpty()) requestPerms.launch(need.toTypedArray())
    }

    /*

    this makes sure that the permissions are met before launcghing the camera
    similar pattern to camera lecture where:
    - create a pre-existing file
    - pass its content uri via EXTRA_OUTPUT
    - using ActivityResultContracts.TakePicture()
     */
    private fun fileForTempPhoto(): File {
        val dir = File(getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), "profile")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "profile_temp.jpg")
    }

    private fun ensureCameraReadyThenLaunch() {
        val need = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) need += Manifest.permission.CAMERA

        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED
            ) need += Manifest.permission.READ_MEDIA_IMAGES
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) need += Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (need.isNotEmpty()) requestPerms.launch(need.toTypedArray())

        // Create a non-null uri and pass that to the launcher
        val photoFile = fileForTempPhoto()
        val photoUri = FileProvider.getUriForFile(this, fileProviderAuthority, photoFile)
        pendingPhotoUri = photoUri
        takePicture.launch(photoUri) // launcher requires a non-null uri
    }

    // load the last saved profile instance from the SharedPreferences and show ir
    private fun loadProfile() {
        val sp = getSharedPreferences(prefsName, Context.MODE_PRIVATE)

        etName.setText(sp.getString(KEY_NAME, "") ?: "")
        etEmail.setText(sp.getString(KEY_EMAIL, "") ?: "")
        etPhone.setText(sp.getString(KEY_PHONE, "") ?: "")
        etClass.setText(sp.getInt(KEY_CLASS, 0).takeIf { it > 0 }?.toString() ?: "")
        etMajor.setText(sp.getString(KEY_MAJOR, "") ?: "")

        when (sp.getInt(KEY_GENDER, -1)) {
            0 -> rgGender.check(R.id.rbFemale)
            1 -> rgGender.check(R.id.rbMale)
            else -> rgGender.clearCheck()
        }

        val savedUri = sp.getString(KEY_PHOTO_URI, null)?.let(Uri::parse)
        if (savedUri != null) {
            imgProfile.setImageURI(savedUri)
        } else {
            // make it a small icon so the frame looks full
            imgProfile.setImageResource(R.mipmap.ic_launcher)
        }
    }

    /*

    validate any inputs lightly
    map gender to integers
    persist with sharedpreferences

    note: the course notes suggests using a helper like saveProfile(), however this is my implementation
     */
    private fun validateAndSave(): Boolean {
        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val major = etMajor.text.toString().trim()
        val classStr = etClass.text.toString().trim()

        // keeps the validation simple as per lab guidelines
        if (phone.isNotEmpty() && !phone.all { it.isDigit() }) {
            etPhone.error = "Numbers only"
            return false
        }

        val classYear = if (classStr.isEmpty()) 0 else try {
            classStr.toInt()
        } catch (_: NumberFormatException) {
            etClass.error = "Enter a number"
            return false
        }

        val gender = when (rgGender.checkedRadioButtonId) {
            R.id.rbFemale -> 0
            R.id.rbMale -> 1
            else -> -1
        }

        val editor = getSharedPreferences(prefsName, Context.MODE_PRIVATE).edit()
        editor.putString(KEY_NAME, name)
        editor.putString(KEY_EMAIL, email)
        editor.putString(KEY_PHONE, phone)
        editor.putInt(KEY_CLASS, classYear)
        editor.putString(KEY_MAJOR, major)
        editor.putInt(KEY_GENDER, gender)
        pendingPhotoUri?.let { editor.putString(KEY_PHOTO_URI, it.toString()) }
        editor.apply()            // this actually saves / writes to disk

        pendingPhotoUri = null
        return true
    }

    /*

    since this specific lab does not require full validations, we did it on a lighter scale
     */

    private fun addLightValidation() {
        val digitsOnlyWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s == null) return
                val clean = s.filter { it.isDigit() }.toString()
                if (clean != s.toString()) {
                    val pos = clean.length
                    s.replace(0, s.length, clean)
                    (currentFocus as? EditText)?.setSelection(pos.coerceAtMost(clean.length))
                }
            }
        }
        etPhone.addTextChangedListener(digitsOnlyWatcher)
        etClass.addTextChangedListener(digitsOnlyWatcher)
    }

    /*
    save the transiet state so rotation wont lose the just created photo uri
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_PENDING_URI, pendingPhotoUri?.toString())
    }
}
