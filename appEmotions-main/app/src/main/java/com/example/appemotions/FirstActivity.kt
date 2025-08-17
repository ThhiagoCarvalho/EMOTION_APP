package com.example.appemotions

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

class FirstActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private lateinit var textViewResult: TextView
    private val REQUEST_IMAGE_PICK = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.tela_principal)

        imageView = findViewById(R.id.imageViewFoto)
        textViewResult = findViewById(R.id.labelResult)

        findViewById<Button>(R.id.btnEnviar).setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"                  // <- só imagens
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            startActivityForResult(intent, REQUEST_IMAGE_PICK)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK && data?.data != null) {
            runFaceDetection(data.data!!)
        }
    }

    private fun runFaceDetection(imageUri: Uri) {
        // Lê o bitmap
        val inputStream = contentResolver.openInputStream(imageUri)
        val original = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        if (original == null) {
            textViewResult.text = "Não foi possível carregar a imagem."
            return
        }

        // Opcional: desenhar por cima -> bitmap mutável + canvas
        val mutable = original.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutable)
        val paint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 6f
            color = Color.RED
            isAntiAlias = true
        }

        val image = InputImage.fromBitmap(original, 0)

        // Configuração do detector (rápido + landmarks/classificações)
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .enableTracking() // se quiser id de tracking
            .build()

        val detector = FaceDetection.getClient(options)

        detector.process(image)
            .addOnSuccessListener { faces ->
                // Desenha caixas e monta texto de resultado
                val sb = StringBuilder()

                val textPaint = Paint().apply {
                    color = Color.WHITE
                    textSize = 36f
                    isAntiAlias = true
                }
                val bgPaint = Paint().apply {
                    color = Color.argb(160, 0, 0, 0) // fundo p/ legibilidade
                    style = Paint.Style.FILL
                }

                faces.forEachIndexed { index, face ->
                    val box = face.boundingBox
                    canvas.drawRect(box, paint)

                    val smileF = face.smilingProbability
                    val leftF  = face.leftEyeOpenProbability
                    val rightF = face.rightEyeOpenProbability

                    val emocao = classificarEmocao(face)

                    sb.appendLine("• Emoção: $emocao")

                    // Desenha o rótulo na imagem
                    val label = "R${index + 1}: $emocao"
                    val x = box.left.toFloat()
                    val y = (box.top - 12).coerceAtLeast(30).toFloat()
                    val pad = 8f
                    val textWidth = textPaint.measureText(label)
                    val textHeight = textPaint.fontMetrics.run { bottom - top }
                    canvas.drawRect(x - pad, y - textHeight - pad, x + textWidth + pad, y + pad, bgPaint)
                    canvas.drawText(label, x, y - 4f, textPaint)
                }

                imageView.setImageBitmap(mutable)
                textViewResult.text = if (faces.isEmpty()) "Nenhum rosto detectado." else sb.toString()
            }
            .addOnFailureListener { e ->
                textViewResult.text = "Erro: ${e.message}"
            }
    }

    private fun classificarEmocao(face: Face): String {
        val smile = face.smilingProbability ?: -1f
        val lEye = face.leftEyeOpenProbability ?: -1f
        val rEye = face.rightEyeOpenProbability ?: -1f

        val olhosAbertos = (lEye >= 0.5f && rEye >= 0.5f)
        val olhosFechados = (lEye in 0f..0.3f && rEye in 0f..0.3f)

        return when {
            smile >= 0.7f && olhosAbertos -> "Feliz 😀"
            olhosFechados && smile < 0.3f -> "Sonolento 😴"
            smile in 0.4f..0.7f -> "Levemente feliz 🙂"
            else -> "Neutro 😐"
        }
    }
}