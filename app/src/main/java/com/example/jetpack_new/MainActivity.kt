package com.example.jetpack_new

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.enkod.enkodpushlibrary.EnkodPushLibrary
import com.enkod.enkodpushlibrary.enkodConnect

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        enkodConnect("andrey_p_client","test_newlibrary123@enkod.io").push(this)

    }
}