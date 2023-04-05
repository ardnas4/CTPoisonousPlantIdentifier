package com.example.ctpoisonousplantidentifier

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val ivyButton = findViewById<ImageButton>(R.id.ivy_info_button)
        ivyButton.setOnClickListener {
            val intent = Intent(this, IvyActivity::class.java)
            startActivity(intent)
        }

        val oakButton = findViewById<ImageButton>(R.id.oak_info_button)
        oakButton.setOnClickListener {
            val intent = Intent(this, OakActivity::class.java)
            startActivity(intent)
        }

        val sumacButton = findViewById<ImageButton>(R.id.sumac_info_button)
        sumacButton.setOnClickListener {
            val intent = Intent(this, SumacActivity::class.java)
            startActivity(intent)
        }

        val treatmentsButton = findViewById<ImageButton>(R.id.treatments_info_button)
        treatmentsButton.setOnClickListener {
            val intent = Intent(this, TreatmentsActivity::class.java)
            startActivity(intent)
        }

        val classifyButton = findViewById<Button>(R.id.classify_plants_button)
        classifyButton.setOnClickListener {
            val intent = Intent(this, ClassifyActivity::class.java)
            startActivity(intent)
        }
    }
}