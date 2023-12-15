package com.example.jetpack_new

import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.enkod.enkodpushlibrary.EnkodPushLibrary
import com.enkod.enkodpushlibrary.enkodConnect
import com.example.jetpack_new.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enkodConnect("andrey_pogodin3").start(this)
        }

            //EnkodPushLibrary.addContact("newlibrarytest15@gmail.com")

        binding.addCont.setOnClickListener {
            EnkodPushLibrary.addContact("newlibrarytest17@gmail.com")
            binding.contactIndecator.text = "contact on"
        }
    }
}
