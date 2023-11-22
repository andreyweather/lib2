package com.enkod.enkodpushlibrary

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.util.Log
import com.enkod.enkodpushlibrary.EnkodPushLibrary.account
import com.enkod.enkodpushlibrary.EnkodPushLibrary.initRetrofit
import com.enkod.enkodpushlibrary.EnkodPushLibrary.processMessage
import com.enkod.enkodpushlibrary.EnkodPushLibrary.sessionId
import com.enkod.enkodpushlibrary.EnkodPushLibrary.token
import com.example.jetpack_new.R
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

private  val TAGL = "LoginData"
private  val ACC_TAG: String = "${TAGL}_ACC"




class EnkodPushMessagingService: FirebaseMessagingService() {


    override fun onNewToken(token: String) {
        super.onNewToken(token)

          Log.d("onNewToken", token.toString())
    }

    override fun onDeletedMessages() {

        EnkodPushLibrary.onDeletedMessage()

    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)


        val directBootContext: Context = this.createDeviceProtectedStorageContext()
// Access appDataFilename that lives in device encrypted storage
        Log.d ("message_app_test", message.data.toString())

        EnkodPushLibrary.soundOn = applicationContext.getString(R.string.sound_on)
        EnkodPushLibrary.vibrationOn = applicationContext.getString(R.string.vibration_on)
        EnkodPushLibrary.pushShowTime = applicationContext.getString(R.string.push_show_time)
        EnkodPushLibrary.ledColor = applicationContext.getString(R.string.led_color)
        EnkodPushLibrary.ledOnMs = applicationContext.getString(R.string.led_on_ms)
        EnkodPushLibrary.ledOffMs = applicationContext.getString(R.string.led_of_ms)
        EnkodPushLibrary.colorName = applicationContext.getString(R.string.color_name)
        EnkodPushLibrary.iconRes = applicationContext.getString(R.string.icon_res)
        EnkodPushLibrary.notificationPriority = applicationContext.getString(R.string.notification_priority)
        EnkodPushLibrary.bannerName = applicationContext.getString(R.string.banner_name)
        EnkodPushLibrary.intentName = applicationContext.getString(R.string.intent_name)
        EnkodPushLibrary.channelId = applicationContext.getString(R.string.channel_id)
        EnkodPushLibrary.notificationImage = applicationContext.getString(R.string.notification_image)
        EnkodPushLibrary.bigImageUrl = applicationContext.getString(R.string.big_image_url)
        EnkodPushLibrary.title = applicationContext.getString(R.string.title)
        EnkodPushLibrary.body = applicationContext.getString(R.string.body)
        EnkodPushLibrary.url = applicationContext.getString(R.string.url)
        EnkodPushLibrary.notificationImportance = applicationContext.getString(R.string.notification_importance)
        EnkodPushLibrary.actionButtonsUrl = applicationContext.getString(R.string.action_buttons_url)
        EnkodPushLibrary.actionButtonText = applicationContext.getString(R.string.action_button_text)
        EnkodPushLibrary.actionButtonIntent = applicationContext.getString(R.string.action_button_intent)
        EnkodPushLibrary.personId = applicationContext.getString(R.string.person_id)
        EnkodPushLibrary.messageId = applicationContext.getString(R.string.message_id)

        initRetrofit()

        processMessage(applicationContext, message)



    }
}



class enkodConnect (_account: String, _email: String, _phone: String) : Activity() {

    private val TAG = "EnkodPushLibrary"
    private val SESSION_ID_TAG: String = "${TAG}_SESSION_ID"
    private val EMAIL_TAG: String = "${TAG}_EMAIL"
    private val PHONE_TAG: String = "${TAG}_PHONE"

    val account: String
    val email: String
    val phone: String



    init {
        account = _account
        email = _email
        phone = _phone
    }


    fun start (context: Context) {

        Log.d("start", "ok")

        try {

            FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(
                        ContentValues.TAG,
                        "Fetching FCM registration token failed",
                        task.exception
                    )
                    return@OnCompleteListener
                }

                val token = task.result
                EnkodPushLibrary.getToken(context, token)
            })

            EnkodPushLibrary.init(context, account, email, phone, 1)

        } catch (e: Exception) {

            EnkodPushLibrary.init(context, account, email, phone, 0)

        }
    }
}

private class ForcedException : Exception() {
    fun forcedThrow() {
        throw ForcedException()
    }
}