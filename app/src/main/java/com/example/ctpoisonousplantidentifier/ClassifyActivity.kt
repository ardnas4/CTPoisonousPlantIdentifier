package com.example.ctpoisonousplantidentifier

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import java.io.ByteArrayOutputStream
import java.net.Socket
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class ClassifyActivity : AppCompatActivity() {

    private val serverIP = "137.99.129.212" // Change to the server's IP address 137.99.130.184
    private val serverPort = 5984 // Change to the server's actual port number
    private val encryptionKey = "oKIuIzA0WPTi59K0yPYz0WAD04AkOvGoJd0hCabO6Ng=" // Change to the actual encryption key used in the server script
    private val SELECT_IMAGE_REQUEST_CODE = 100
    private var selectedImage: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_classify)

        val selectImageButton = findViewById<Button>(R.id.select_image_button)
        val sendImageButton = findViewById<Button>(R.id.send_image_button)
        val backButton = findViewById<Button>(R.id.back_button)

        selectImageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, SELECT_IMAGE_REQUEST_CODE)
        }

        sendImageButton.setOnClickListener {
            selectedImage?.let { image ->
                sendImage(serverIP, serverPort, image, encryptionKey)
            }
        }

        backButton.setOnClickListener {
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SELECT_IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val imageUri = data.data
            val inputStream = imageUri?.let { contentResolver.openInputStream(it) }
            selectedImage = BitmapFactory.decodeStream(inputStream)

            val imageView = findViewById<ImageView>(R.id.selected_image_view)
            imageView.setImageBitmap(selectedImage)
        }
    }

    private fun encryptMessage(message: ByteArray, key: String): ByteArray {
        Log.d("encryptMessage", "encrypt called")
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        val secretKeySpec = SecretKeySpec(key.toByteArray(), "AES")
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec)
        Log.d("encryptMessage", "encrypt done")
        return cipher.doFinal(message)
    }

    private fun decryptMessage(message: ByteArray, key: String): ByteArray {
        Log.d("decryptMessage", "decrypt called")
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        val secretKeySpec = SecretKeySpec(key.toByteArray(), "AES")
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec)
        Log.d("decryptMessage", "decrypt done")
        return cipher.doFinal(message)
    }

//    private fun updateReceivedInfo(plantType: String, accuracy: Float) {
//        runOnUiThread {
//            val infoText = "Plant type: $plantType\nPrediction accuracy: ${accuracy * 100}%"
//            received_info_textview.text = infoText
//        }
//    }

    private fun sendImage(serverIP: String, serverPort: Int, image: Bitmap, key: String) {
        Log.d("sendImage", "sendImage started")
        Thread {
            Log.d("sendImage", "thread started")
            val socket = Socket(serverIP, serverPort)
            Log.d("sendImage", "socket Initiated")

            Log.d("sendImage", "Compressing Image")
            val byteArrayOutputStream = ByteArrayOutputStream()
            image.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
            val imageData = byteArrayOutputStream.toByteArray()
            Log.d("sendImage", "Image compressed")

            Log.d("sendImage", "Encrypting data")
            val encryptedData = encryptMessage(byteArrayOf(1) + imageData, key)
            socket.getOutputStream().write(encryptedData)
            Log.d("sendImage", "Encrypted Data sent")

            Log.d("sendImage", "Recieving Response")
            val response = ByteArray(1024)
            val responseSize = socket.getInputStream().read(response)
            val decryptedResponse = decryptMessage(response.sliceArray(0 until responseSize), key)
            val responseType = decryptedResponse[0].toInt()
            Log.d("sendImage", "Response Recieved")

            when (responseType) {
                0 -> {
                    runOnUiThread {
                        // Handle connection termination by the server
                        Log.d("sendImage", "server code 0: terminated by server")
                    }
                }
                1 -> {
                    runOnUiThread {
                        // Handle server requesting to resend the image
                        Log.d("sendImage", "server code 1: resend image")
                    }
                }
                10 -> {
                    val delimiter = "0/"
                    val responseString = decryptedResponse.toString(Charsets.UTF_8)
                    val splitResponse = responseString.split(delimiter)
                    val predPlantBytes = splitResponse[0].toByteArray(Charsets.UTF_8)
                    val predAccBytes = splitResponse[1].toByteArray(Charsets.UTF_8)
                    val imgData = if (splitResponse.size > 2) splitResponse[2].toByteArray(Charsets.UTF_8) else ByteArray(0)

                    val predPlant = String(predPlantBytes)
                    val predAcc = predAccBytes.toString(Charsets.UTF_8).toFloat()

                    runOnUiThread {
                        // Update the UI with the received information
                        Log.d("sendImage", "server code 10: Update UI with received info")
                    }

                    if (imgData.isNotEmpty()) {
                        val receivedImage = BitmapFactory.decodeByteArray(imgData, 0, imgData.size)
                        runOnUiThread {
                            // Update the UI with the received image
                            Log.d("sendImage", "server code 10: update UI with received image")
                        }
                    }
                }
            }

            socket.close()
        }.start()
    }
}

