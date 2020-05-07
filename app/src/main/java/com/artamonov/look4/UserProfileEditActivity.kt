package com.artamonov.look4

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_user_profile_edit.*
import kotlinx.android.synthetic.main.activity_welcome.*

class UserProfileEditActivity : AppCompatActivity() {

    var newImage: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile_edit)

        checkForPermissions()

        user_edit_submit_button.setOnClickListener {
            if (!fieldsAreValid()) {
                Snackbar.make(findViewById(android.R.id.content), "Please, don't leave fields blank", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener }
            val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
            val editor = sharedPref.edit()
            val isSaved = editor
                .putString(USER_NAME, user_edit_name.text.toString())
                .putString(USER_PHONE_NUMBER, user_edit_phone_number.text.toString())
                .putString(USER_IMAGE_URI, newImage.toString())
                .commit()
            if (isSaved) { finish() }
        }

        user_edit_add_image.setOnClickListener {
            dispatchTakePictureIntent() }
    }

    private fun populateData() {
        user_edit_name.setText(PreferenceManager.getDefaultSharedPreferences(this).getString(USER_NAME, null))
        user_edit_phone_number.setText(PreferenceManager.getDefaultSharedPreferences(this).getString(
            USER_PHONE_NUMBER, null))
        val imageString = PreferenceManager.getDefaultSharedPreferences(this).getString(USER_IMAGE_URI, null)
        imageString?.let {
            val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, Uri.parse(imageString))
            Glide.with(this).load(bitmap).apply(RequestOptions.circleCropTransform()).into(user_edit_add_image)
        }
    }

    private fun fieldsAreValid(): Boolean {
        return !user_edit_name.text?.trim().isNullOrEmpty() && !user_edit_phone_number.text?.trim().isNullOrEmpty()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == TAKE_IMAGE_REQUEST && resultCode == RESULT_OK) {
            newImage = data?.data
            val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, newImage)
            Glide.with(this).load(bitmap).apply(RequestOptions.circleCropTransform()).into(user_edit_add_image)
        }
    }

    private fun dispatchTakePictureIntent() {
        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                startActivityForResult(takePictureIntent, TAKE_IMAGE_REQUEST)
            }
        }
    }

    private fun checkForPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE)
            } else {
            populateData()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    populateData()
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    finish()
                    Snackbar.make(findViewById(android.R.id.content), "You need to grant the permission to read your profile picture",
                        Snackbar.LENGTH_SHORT).show()
                }
                return
            }

            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }
}
