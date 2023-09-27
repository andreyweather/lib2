package com.enkod.enkodpushlibrary

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.os.Bundle
import android.util.Log
import com.example.jetpack_new.BuildConfig
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseInstanceIDService : FirebaseMessagingService() {


    override fun onNewToken(token: String) {
        super.onNewToken(token)

        val token = FirebaseMessaging.getInstance().getToken();
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {

        if (remoteMessage.getNotification() != null) {

            enkodConnect("andrey_p_client", "good.pogodin1@enkod.ru").start(this)

        }
    }
}

class enkodConnect(_account: String, _email: String) : Activity() {

    val account: String
    val email: String

    init {
        account = _account
        email = _email
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


    }

    fun start(context: Context) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(ContentValues.TAG, "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result

            Log.d("token", token)
        })

        val anyArray = arrayOf<Any>()
        val arrInt = arrayOf<Int>()
        val hash = hashMapOf<String, Any?>()


        EnkodPushLibrary.Builder()
            .setOnTokenListener {}
            .setOnMessageReceiveListener {}
            .setOnDeletedMessageListener {}
            .setOnBaseUrlCheckerListener { BuildConfig.DEV_BASE_URL }
            .setOnDynamicLinkClickListener { string -> }
            .setOnPushClickListener { bundle: Bundle, str: String -> }
            .build(context, account)

        CoroutineScope(Dispatchers.IO).launch {
            Thread.sleep(1000)
            EnkodPushLibrary.Subscriber().Subscribe(
                context, SubscriberInfo(
                    email,
                    "8906118345",
                    "",
                    "",
                    anyArray,
                    arrInt,
                    hash,
                    ""
                )
            )
        }
    }
}