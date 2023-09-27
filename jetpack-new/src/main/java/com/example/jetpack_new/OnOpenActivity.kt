package com.enkod.enkodpushlibrary

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast

class OnOpenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
    }

    @SuppressLint("MissingSuperCall")
    override fun onNewIntent(intent: Intent?) {
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.apply {
            val bundle = getIntent().extras
            if (bundle != null) {
                Log.i("handleExtras", "${intent.getStringExtra(EnkodPushLibrary.intentName)} $${intent.getStringExtra(EnkodPushLibrary.url)}")
                EnkodPushLibrary.handleExtras(this@OnOpenActivity, bundle)
            }
        }
        finish()
    }
}
