package com.example.jetpack_new


import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.enkod.enkodpushlibrary.EnkodConnect
import com.enkod.enkodpushlibrary.EnkodPushLibrary
import com.enkod.enkodpushlibrary.Product
import com.example.jetpack_new.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.logOut.setOnClickListener {
            EnkodPushLibrary.logOut(this)
        }

        val product = Product("id1", "TS", 1, "1000","")

        EnkodConnect("andrey_pogodin3", false).start(this)
        EnkodPushLibrary.addContact("fcm_a32_9@gmail.com")
        EnkodPushLibrary.addToFavourite(product)

        }
    }
