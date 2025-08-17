package com.example.appemotions

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val botaoComecar = findViewById<Button>(R.id.btnIniciar)

        botaoComecar.setOnClickListener {
            val intent = Intent(this, FirstActivity::class.java)
            startActivity(intent)
            // animação simples para diferenciar
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }
    }
}
