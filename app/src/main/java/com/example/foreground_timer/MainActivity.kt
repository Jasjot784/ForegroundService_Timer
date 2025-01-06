package com.example.foreground_timer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        if(ContextCompat.checkSelfPermission(this@MainActivity,Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.POST_NOTIFICATIONS),1)
        }
        if(ContextCompat.checkSelfPermission(this@MainActivity,Manifest.permission.FOREGROUND_SERVICE) == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.FOREGROUND_SERVICE),2)
        }
        var button = findViewById<Button>(R.id.btClick)
        button.setOnClickListener {
            val intent = Intent(this, TimerService::class.java)
            intent.putExtra("time", "demo")
            startForegroundService(intent)
        }

    }
}