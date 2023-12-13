package com.example.enkodpushlibrary

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.enkod.enkodpushlibrary.EnkodPushLibrary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.system.exitProcess


class MyService():Service() {

                private val TAG = "EnkodPushLibrary"
                private  val EXIT_TAG: String = "${TAG}_EXIT"

                @SuppressLint("NotificationId0")

                override fun onCreate() {

                    super.onCreate()

                    Thread.setDefaultUncaughtExceptionHandler { paramThread, paramThrowable -> //Catch your exception

                        System.exit(0)
                    }

                    try {

                        if (Build.VERSION.SDK_INT >= 26) {


                            val CHANNEL_ID = "my_channel_service"
                            val channel = NotificationChannel(
                                CHANNEL_ID,
                                "Channel human readable title",
                                NotificationManager.IMPORTANCE_DEFAULT
                            )
                            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                                channel
                            )
                            val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
                                .setContentTitle("")
                                .setContentText("").build()

                            CoroutineScope(Dispatchers.IO).launch {

                                delay(3400)

                                if (EnkodPushLibrary.exit == 1) {

                                    val preferences = applicationContext.getSharedPreferences(TAG, Context.MODE_PRIVATE)
                                    preferences.edit()
                                        .putString(EXIT_TAG, "exit")
                                        .apply()

                                    val exitPref = preferences.getString(EXIT_TAG, null)
                                    Log.d ("exit_exit_pref", exitPref.toString())

                                    delay(200)
                                    exitProcess(0)

                                }

                                val preferences = applicationContext.getSharedPreferences(TAG, Context.MODE_PRIVATE)
                                val exitPref = preferences.getString(EXIT_TAG, null)

                                if (exitPref.toString() == "exit") {

                                    Log.d ("stopSelf","yes")
                                    exitProcess(0)
                                    stopSelf()

                                }

                                if (exitPref.toString() != "exit") {

                                    Log.d ("exit_exit_pref", exitPref.toString())
                                    startForeground(1, notification)

                                    var seflJob = true
                                    while (seflJob) {

                                        delay (100)

                                        if (EnkodPushLibrary.exitSelf == 1) {
                                            stopSelf()
                                            seflJob = false
                                        }
                                     }
                                }
                            }
                            CoroutineScope(Dispatchers.IO).launch {
                                delay(15000)
                                stopSelf()
                            }
                        }
                    }catch (e: Exception) {

                    }

    }




    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }



    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        return super.onStartCommand(intent, flags, startId)

    }
}



