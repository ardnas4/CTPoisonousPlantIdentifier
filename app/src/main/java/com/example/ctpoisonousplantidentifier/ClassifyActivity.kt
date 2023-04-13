package com.example.ctpoisonousplantidentifier

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import java.io.ByteArrayOutputStream
import java.net.Socket
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import java.io.File
import kotlin.math.min


class ClassifyActivity : AppCompatActivity() {

    private val serverIP = "137.99.130.184" // Server's IP address: 137.99.130.184  Uconn test address: 137.99.128.35
    private val serverPort = 5984 // 5984 has been unblocked
    private val SELECT_IMAGE_REQUEST_CODE = 10000
    private val prefix = 1

    private var selectedImage: Bitmap? = null
    private val CAMERA_PERMISSION_CODE = 1000
    private val IMAGE_CAPTURE_CODE = 1001
    private var imageUri1: Uri? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_classify)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        val selectImageButton = findViewById<ImageButton>(R.id.select_image_button)
        val sendImageButton = findViewById<Button>(R.id.send_image_button)
        val takePicButton = findViewById<ImageButton>(R.id.take_picture_button)
        val goBackButton = findViewById<ImageButton>(R.id.go_back_button)

        selectImageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, SELECT_IMAGE_REQUEST_CODE)
        }

        sendImageButton.setOnClickListener {
            selectedImage?.let { image ->
                sendImage(serverIP, serverPort, image)
            }
        }

        goBackButton.setOnClickListener {
            finish()
        }

        takePicButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                if (checkSelfPermission(Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_DENIED ||
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_DENIED){
                    val permission = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    requestPermissions(permission, CAMERA_PERMISSION_CODE)
                }
                else {
                    openCamera()


                }
            }
            else{
                openCamera()

            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SELECT_IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val imageUri = data.data
            val imageFile = File(imageUri?.getPath()!!)
            val inputStream = imageUri?.let { contentResolver.openInputStream(it) }
            selectedImage = BitmapFactory.decodeStream(inputStream)

            // Get the orientation of the image and rotate the bitmap if required
            val orientation = getImageOrientation(imageUri!!)
            if (orientation != ExifInterface.ORIENTATION_UNDEFINED) {
                selectedImage = rotateBitmap(selectedImage!!, orientation)
            }

            // Resize the image
            selectedImage = resizeImage(selectedImage!!, 224, 224)

            val imageView = findViewById<ImageView>(R.id.selected_image_view)
            imageView.setImageBitmap(selectedImage)

            // Hide the TextView when a new image is selected
            findViewById<TextView>(R.id.received_info_textview).visibility = View.GONE
        }
        if (requestCode == IMAGE_CAPTURE_CODE && resultCode == Activity.RESULT_OK) {
            imageUri1?.let {
                val inputStream = contentResolver.openInputStream(it)
                selectedImage = BitmapFactory.decodeStream(inputStream)

                // Get the orientation of the image and rotate the bitmap if required
                val orientation = getImageOrientation(it)
                if (orientation != ExifInterface.ORIENTATION_UNDEFINED) {
                    selectedImage = rotateBitmap(selectedImage!!, orientation)
                }

                // Resize the image
                selectedImage = resizeImage(selectedImage!!, 224, 224)

                val imageView = findViewById<ImageView>(R.id.selected_image_view)
                imageView.setImageBitmap(selectedImage)

                // Hide the TextView when a new image is taken
                findViewById<TextView>(R.id.received_info_textview).visibility = View.GONE
            }
        }

    }

    private fun openCamera() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "New Picture")
        values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera")
        imageUri1 = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        val camIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        camIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri1)
        startActivityForResult(camIntent, IMAGE_CAPTURE_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            CAMERA_PERMISSION_CODE-> {
                if (grantResults.size > 0 && grantResults[0]==
                    PackageManager.PERMISSION_GRANTED){
                    openCamera()

                }
                else{
                    Toast.makeText(this, "Denied", Toast.LENGTH_SHORT).show()
                }

            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun getImageOrientation(imageUri: Uri): Int {
        val inputStream = contentResolver.openInputStream(imageUri)
        val exifInterface = inputStream?.let { ExifInterface(it) }
        return exifInterface?.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        ) ?: ExifInterface.ORIENTATION_UNDEFINED
    }

    private fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90F)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180F)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270F)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }


    private fun resizeImage(image: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        return Bitmap.createScaledBitmap(image, targetWidth, targetHeight, true)
    }

    private fun sendImage(serverIP: String, serverPort: Int, image: Bitmap) {
        Log.d("sendImage", "sendImage Called")

        // Resize the image
        val targetWidth = 224
        val targetHeight = 224
        val resizedImage = Bitmap.createScaledBitmap(image, targetWidth, targetHeight, true)

        val byteArrayOutputStream = ByteArrayOutputStream()
        resizedImage.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        val eoiMarker = byteArrayOf(0xFF.toByte(), 0xD9.toByte())
        val message = ByteArray(byteArray.size + eoiMarker.size + 1)
        message[0] = prefix.toByte()
        System.arraycopy(byteArray, 0, message, 1, byteArray.size)
        System.arraycopy(eoiMarker, 0, message, byteArray.size + 1, eoiMarker.size)
        Log.d("Image Size", "Image size (in bytes): ${byteArray.size}")


        Thread {
            Log.d("Thread", "Thread Initialized")
            try {
                Socket(serverIP, serverPort).use { socket ->
                    Log.d("sendImage", "Initializing Socket")
                    val outputStream = socket.getOutputStream()
                    var sentBytes = 0
                    while (sentBytes < message.size) {
                        Log.d("Preparing image to send", "Compiling Bytes...")
                        val bytesToSend = min(4096, message.size - sentBytes)
                        outputStream.write(message, sentBytes, bytesToSend)
                        sentBytes += bytesToSend
                    }
                    outputStream.flush()

                    Log.d("sendImage", "Image output written")

                    val inputStream = socket.getInputStream()
                    val buffer = ByteArray(1048576)
                    var readBytes: Int

                    Log.d("sendImage", "Waiting for Server Response")
                    while (inputStream.read(buffer).also { readBytes = it } != -1) {
                        Log.d("sendImage", "Getting Result")
                        val response = ByteArray(readBytes)
                        System.arraycopy(buffer, 0, response, 0, readBytes)
                        val responseStr = String(response)

                        Log.d("ServerResponse", "$responseStr")
                        Log.d("sendImage", "Splitting message")
                        val parts = responseStr.split(",")

                        when (parts[0]) {
                            "00" -> {
                                // Connection terminated
                                Log.d("sendImage", "Terminate Connection")
                                break
                            }
                            "01" -> {
                                // Request to resend the image due to low accuracy
                                Log.d("sendImage", "Error 01: not high enough accuracy")
                                runOnUiThread {
                                    AlertDialog.Builder(this@ClassifyActivity)
                                        .setTitle("Low Prediction Accuracy")
                                        .setMessage("The prediction accuracy is too low. Please retake the image and try again.")
                                        .setPositiveButton("OK") { _, _ ->
                                            // You may want to perform any necessary action when the user clicks "OK"
                                        }
                                        .show()
                                }
                            }
                            "10" -> {
                                Log.d("sendImage", "preparing to output results to text view")
                                val plantType = parts[2]
                                val predictionAccuracy = parts[1].toDouble()

                                runOnUiThread {
                                    val prettyPlantType = if (plantType == "Not") {
                                        "Not Poisonous"
                                    } else {
                                        plantType.replace('_', ' ')
                                    }

                                    val resultText = "Result: $prettyPlantType\nConfidence: ${"%.2f".format(predictionAccuracy)}%"
                                    findViewById<TextView>(R.id.received_info_textview).apply {
                                        text = resultText
                                        visibility = View.VISIBLE
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

}