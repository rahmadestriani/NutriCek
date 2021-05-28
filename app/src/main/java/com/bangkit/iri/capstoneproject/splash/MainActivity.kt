package com.bangkit.iri.capstoneproject.splash

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.bangkit.iri.capstoneproject.HomeActivity
import com.bangkit.iri.capstoneproject.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.hide()

        Handler(Looper.getMainLooper()).postDelayed({
            intent = Intent(this@MainActivity, HomeActivity::class.java)
            startActivity(intent)
        }, 1500)
    }
}