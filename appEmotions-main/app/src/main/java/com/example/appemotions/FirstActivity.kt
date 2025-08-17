package com.example.appemotions

import android.app.Activity
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

class FirstActivity : AppCompatActivity() {
    private lateinit var fotoView: ImageView
    private lateinit var resultadoTexto: TextView
    private val PICK_IMAGE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tela_principal)

        fotoView = findViewById(R.id.imageViewFoto)
        resultadoTexto = findViewById(R.id.labelResult)

        val botaoEnviar = findViewById<Button>(R.id.btnEnviar)
        botaoEnviar.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            startActivityForResult(intent, PICK_IMAGE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK && data?.data != null) {
            processarRosto(data.data!!)
        }
    }

    private fun processarRosto(uri: Uri) {
        val inputStream = contentResolver.openInputStream(uri)
        val original = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        if (original == null) {
            resultadoTexto.text = "Erro ao carregar imagem."
            return
        }

        val bitmapEditavel = original.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(bitmapEditavel)
        val paintBox = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 5f
            color = Color.CYAN
            isAntiAlias = true
        }

        val image = InputImage.fromBitmap(original, 0)

        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()

        val detector = FaceDetection.getClient(options)

        detector.process(image)
            .addOnSuccessListener { faces ->
                val sb = StringBuilder()

                val textPaint = Paint().apply {
                    color = Color.YELLOW
                    textSize = 34f
                    isAntiAlias = true
                }

                val bgPaint = Paint().apply {
                    color = Color.argb(150, 0, 0, 0)
                    style = Paint.Style.FILL
                }

                faces.forEachIndexed { index, face ->
                    val box = face.boundingBox
                    canvas.drawRect(box, paintBox)

                    val emocao = analisarEmocao(face)
                    sb.appendLine("Rosto ${index + 1}: $emocao")

                    val label = "$emocao"
                    val x = box.left.toFloat()
                    val y = (box.top - 12).coerceAtLeast(40).toFloat()
                    val pad = 6f
                    val largura = textPaint.measureText(label)
                    val altura = textPaint.fontMetrics.run { bottom - top }
                    canvas.drawRect(x - pad, y - altura - pad, x + largura + pad, y + pad, bgPaint)
                    canvas.drawText(label, x, y - 4f, textPaint)
                }

                fotoView.setImageBitmap(bitmapEditavel)
                resultadoTexto.text = if (faces.isEmpty()) "Nenhum rosto detectado 😕" else sb.toString()
                if (faces.isEmpty()) Toast.makeText(this, "Tente outra foto!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                resultadoTexto.text = "Erro: ${e.message}"
            }
    }

    private fun analisarEmocao(face: Face): String {
        val sorriso = face.smilingProbability ?: -1f
        val olhoE = face.leftEyeOpenProbability ?: -1f
        val olhoD = face.rightEyeOpenProbability ?: -1f

        return when {
            sorriso > 0.7f && olhoE > 0.5f && olhoD > 0.5f -> "Feliz 😀"
            sorriso < 0.3f && olhoE < 0.3f && olhoD < 0.3f -> "Com sono 😴"
            sorriso in 0.4f..0.7f -> "Contente 🙂"
            else -> "Neutro 😐"
        }
    }
}
