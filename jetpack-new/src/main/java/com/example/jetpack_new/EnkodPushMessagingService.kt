package com.enkod.enkodpushlibrary

import android.widget.Toast
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

internal class EnkodPushMessagingService: FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        //EnkodPushLibrary.onNewToken(applicationContext, token)
    }

    override fun onDeletedMessages() {
        EnkodPushLibrary.onDeletedMessage()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        //super.onMessageReceived(message)
        EnkodPushLibrary.processMessage(applicationContext, message)
        Toast.makeText(applicationContext, message.toString(), Toast.LENGTH_SHORT).show()
    }
}

