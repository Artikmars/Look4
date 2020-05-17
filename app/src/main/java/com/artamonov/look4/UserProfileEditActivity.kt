package com.artamonov.look4

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.artamonov.look4.data.prefs.PreferenceHelper
import com.artamonov.look4.utils.PostTextChangeWatcher
import com.artamonov.look4.utils.isValidPhoneNumber
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_user_profile_edit.*
import org.koin.android.ext.android.inject

class UserProfileEditActivity : AppCompatActivity() {

    var newImage: Uri? = null
    private var enteredPhoneNumber: String? = null
    private val preferenceHelper: PreferenceHelper by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile_edit)

        checkForPermissions()

        user_edit_phone_number.addTextChangedListener(PostTextChangeWatcher { phoneNumberChanged(it) })

        profile_edit_back.setOnClickListener { onBackPressed() }

        user_edit_submit_button.setOnClickListener {
            if (!fieldsAreValid()) {
                Snackbar.make(findViewById(android.R.id.content), "Please, don't leave fields blank", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener }
            val isSaved =
                preferenceHelper.updateUserProfile(name = user_edit_name.text.toString(), phoneNumber =
                user_edit_phone_number.text.toString(), imagePath = newImage?.toString())
            if (isSaved) { finish() }
        }

        user_edit_add_image.setOnClickListener {
            dispatchTakePictureIntent() }
    }

    private fun populateData() {
        user_edit_name.setText(preferenceHelper.getUserProfile()?.name)
        user_edit_phone_number.setText(preferenceHelper.getUserProfile()?.phoneNumber)
        val imageString = preferenceHelper.getUserProfile()?.imagePath
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
//        if (requestCode == TAKE_IMAGE_REQUEST && resultCode == RESULT_OK) {
//            newImage = data?.data
//            Log.v("Look4", "newImage.path: ${newImage?.path}")
//            val file = File(newImage?.path!!)
//            Log.v("Look4", "file.exist : ${file.exists()}")
//            Log.v("Look4", "file.path : ${file.path}")
//            Log.v("Look4", "file.absolutePath : ${file.absolutePath}")
//
//            val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, newImage)
//          //  val bytes = ByteArrayOutputStream()
//          //  bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
//          //  val path: String = MediaStore.Images.Media.insertImage(contentResolver, bitmap, "Title", null)
//          //  newImage =  Uri.parse(path)
//            Log.v("Look4", "bitmap uri : $newImage")
//            Log.v("Look4", "bitmap uri path : ${newImage?.path}")
//          //  val inputStream = contentResolver.openInputStream(newImage!!)
//           // val newFile = File(newImage?.path!!)
//
//            Glide.with(this).load(bitmap).apply(RequestOptions.circleCropTransform()).into(user_edit_add_image)
//        }

        when (resultCode) {
            Activity.RESULT_OK -> {
                newImage = data?.data
                Log.v("Look4", "uri : $newImage")
                Log.v("Look4", "uri.path : ${newImage?.path}")
                Glide.with(this).load(newImage).apply(RequestOptions.circleCropTransform()).into(user_edit_add_image)
            }
            ImagePicker.RESULT_ERROR -> {
                Toast.makeText(this, ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
            }
            else -> {
                //   Toast.makeText(this, "Task Cancelled", Toast.LENGTH_SHORT).show()
            }
        }
        }

    private fun dispatchTakePictureIntent() {
//        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).also { takePictureIntent ->
//            takePictureIntent.resolveActivity(packageManager)?.also {
//                startActivityForResult(takePictureIntent, TAKE_IMAGE_REQUEST)
//            }
//        }
        ImagePicker.with(this)
            .cropSquare()
            .compress(1024)
            .maxResultSize(300, 300)
            .start()
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

    private fun phoneNumberChanged(newText: String?) {
        enteredPhoneNumber = newText?.trim()
        validatePhoneNumber()
    }

    private fun isPhoneNumberValid(phoneNumber: String?): Boolean {
        return phoneNumber?.isValidPhoneNumber() ?: false
    }

    private fun validatePhoneNumber() {
        return if (isPhoneNumberValid(enteredPhoneNumber)) {
            showPhoneNumberWarning(false)
        } else {
            showPhoneNumberWarning(true)
        }
    }

    private fun showPhoneNumberWarning(state: Boolean) {
        if (state) {
            user_edit_phone_number_layout.error = resources.getString(R.string.welcome_phone_number_warning)
        } else {
            user_edit_phone_number_layout.error = null
        }
    }
}
