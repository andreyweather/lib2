package com.example.jetpack_new

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import com.enkod.enkodpushlibrary.EnkodPushLibrary

class NetworkService : Service() {

    @RequiresApi(Build.VERSION_CODES.O)

    override fun onCreate() {
        super.onCreate()

        Log.d("network_service", "start")

        startForeground(1, EnkodPushLibrary.createdNotificationForNetworkService(applicationContext))

        EnkodPushLibrary.startServiceObserver.value = true

        EnkodPushLibrary.notificationCreatedObserver.observable.subscribe {

            if (it) {

                Log.d("network_service", "stop_notification_show")
                stopSelf()

            }
        }

        EnkodPushLibrary.tokenUpdateObserver.observable.subscribe {

            if (it) {

                Log.d("network_service", "stop_token_update")
                stopSelf()
            }
        }

        EnkodPushLibrary.refreshAppInMemoryObserver.observable.subscribe {

            if (it) {

                Log.d("network_service", "stop_refresh_app")
                stopSelf()
            }
        }
    }
    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }
}


