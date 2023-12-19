package com.enkod.enkodpushlibrary

import android.app.Activity
import android.app.ActivityManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.enkod.enkodpushlibrary.EnkodPushLibrary.initLateInit
import com.enkod.enkodpushlibrary.EnkodPushLibrary.initRetrofit
import com.enkod.enkodpushlibrary.EnkodPushLibrary.isOnlineStatus
import com.enkod.enkodpushlibrary.EnkodPushLibrary.processMessage
import com.example.enkodpushlibrary.MyService
import com.example.jetpack_new.R
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import rx.Observable
import rx.Observer
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.util.concurrent.Callable


private val TAGL = "LoginData"
private val ACC_TAG: String = "${TAGL}_ACC"


class EnkodPushMessagingService : FirebaseMessagingService() {

    private val TAG = "EnkodPushLibrary"
    private val EXIT_TAG: String = "${TAG}_EXIT"


    override fun onNewToken(token: String) {
        super.onNewToken(token)

        Log.d("onNewToken", token.toString())

    }

    override fun onDeletedMessages() {


        EnkodPushLibrary.onDeletedMessage()

    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onMessageReceived(message: RemoteMessage) {


        Log.d("onMessageReceived", "connect")

        super.onMessageReceived(message)

        // EXIT_TAG - preferences с содержанием поля "noexit" открывают доступ к методу startForeground() в Myservice

        val preferences = applicationContext.getSharedPreferences(TAG, Context.MODE_PRIVATE)
        preferences.edit()

            .putString(EXIT_TAG, "noexit")
            .apply()


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



        fun isAppInforegrounded(): Boolean {
            val appProcessInfo = ActivityManager.RunningAppProcessInfo();
            ActivityManager.getMyMemoryState(appProcessInfo);
            return (appProcessInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND ||
                    appProcessInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE)
        }


        if (!isAppInforegrounded()) {

            val service = Intent(this, MyService::class.java)

            this.startForegroundService(service)

            imageCheckAndStartLoad(message)

        }

        else {

            imageCheckAndStartLoad(message)

        }

        initRetrofit()
        initLateInit(this)

    }


    fun imageCheckAndStartLoad(message: RemoteMessage) {

        if (message.data["image"].isNullOrEmpty()) {

            processMessage(this, message, null)

        } else {

            downloadImageToPush(this, message)

        }
    }


    fun downloadImageToPush(context: Context, message: RemoteMessage) {

        val userAgent = System.getProperty("http.agent")

        val url = GlideUrl(

            message.data["image"], LazyHeaders.Builder()
                .addHeader(
                    "User-Agent",
                    userAgent
                )
                .build()

        )

        Observable.fromCallable(object : Callable<Bitmap?> {
            override fun call(): Bitmap? {
                val future = Glide.with(context).asBitmap()
                    .timeout(30000)
                    .load(url).submit()
                return future.get()
            }
        }).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<Bitmap?> {

                override fun onCompleted() {

                }

                override fun onError(e: Throwable) {

                    Log.d("onError", e.message.toString())
                    processMessage(context, message, null)

                }

                override fun onNext(t: Bitmap?) {
                    processMessage(context, message, t!!)
                    Log.d("onNext", t.toString())
                    EnkodPushLibrary.exitSelf()

                }
            })
    }
}


class enkodConnect(_account: String) : Activity() {

    val account: String

    init {
        account = _account
    }


    fun start(context: Context) {

        if (EnkodPushLibrary.isOnline(context)) {

            isOnlineStatus(1)
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

                EnkodPushLibrary.init(context, account, 1)

            } catch (e: Exception) {

                EnkodPushLibrary.init(context, account, 0)

            }
        }else {
            isOnlineStatus(0)
            Log.d("Internet", "Интернет отсутствует")
        }
    }
}

