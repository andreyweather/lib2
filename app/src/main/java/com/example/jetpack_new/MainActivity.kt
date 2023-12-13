package com.example.jetpack_new

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.annotation.RequiresApi
import com.enkod.enkodpushlibrary.EnkodPushLibrary
import com.enkod.enkodpushlibrary.enkodConnect
import com.example.jetpack_new.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enkodConnect("andrey_pogodin3").start(this)
        binding.addCont.setOnClickListener {
            EnkodPushLibrary.addContact("newlibrarytest7@gmail.com")
            binding.contactIndecator.text = "contact on"
        }
    }
}