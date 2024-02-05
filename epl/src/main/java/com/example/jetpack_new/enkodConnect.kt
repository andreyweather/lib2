package com.enkod.enkodpushlibrary

import android.content.ContentValues
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.enkod.enkodpushlibrary.EnkodPushLibrary.isAppInforegrounded
import com.example.jetpack_new.NetworkService
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import java.util.concurrent.TimeUnit


private val TAG = "EnkodPushLibrary"
private val WORKER_TAG: String = "${TAG}_WORKER"
private val ACCOUNT_TAG: String = "${TAG}_ACCOUNT"


class enkodConnect(_account: String, _tokenUpdate: Boolean = true)  {


    val account: String
    val tokenUpdate: Boolean

    init {

        account = _account
        tokenUpdate = _tokenUpdate

    }


    fun start(context: Context) {

        val preferences = context.getSharedPreferences(TAG, MODE_PRIVATE)

        fun refreshInMemoryWorker() {

            val workRequest =
                PeriodicWorkRequestBuilder<RefreshAppInMemoryWorkManager>(12, TimeUnit.HOURS)
                    .build()

            WorkManager

                .getInstance(context)
                .enqueueUniquePeriodicWork(
                    "refreshInMemoryWorker",
                    ExistingPeriodicWorkPolicy.UPDATE,
                    workRequest
                )


            preferences.edit()
                .putString(WORKER_TAG, "start")
                .apply()

        }



        fun startOneTimeWorkerForTokenUpdate() {

            Log.d("doWork", "startOneTime")
            val workRequest = OneTimeWorkRequestBuilder<OneTimeWorkManager>()
                //.setInitialDelay(336, TimeUnit.HOURS)
                .build()

            WorkManager

                .getInstance(context)
                .enqueue(workRequest)

        }


        var preferencesWorker: String? = preferences.getString(WORKER_TAG, null)

        if (preferencesWorker == null) {

            refreshInMemoryWorker()

        }

        if (preferencesWorker == null && tokenUpdate) {

            startOneTimeWorkerForTokenUpdate()

        }

        if (EnkodPushLibrary.isOnline(context)) {

            EnkodPushLibrary.isOnlineStatus(1)

            try {

                Log.d("new_token", "token")

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

                    EnkodPushLibrary.init(context,account,token)

                })

            } catch (e: Exception) {

                EnkodPushLibrary.init(context, account)

            }

        } else {
            EnkodPushLibrary.isOnlineStatus(0)
            Log.d("Internet", "Интернет отсутствует")
        }
    }
}

class RefreshAppInMemoryWorkManager(context: Context, workerParameters: WorkerParameters) :
    Worker(context, workerParameters) {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun doWork(): Result {

        if (!isAppInforegrounded()) {

            applicationContext.startForegroundService(
                Intent(
                    applicationContext,
                    NetworkService::class.java
                )
            )

            EnkodPushLibrary.tokenUpdateObserver.value = true
        }

        Log.d("doWork", "refreshInMemory")

        return Result.success()
    }
}

class UpdateTokenWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun doWork(): Result {

        Log.d("doWork", "updateToken")


        val preferences = applicationContext.getSharedPreferences(TAG, Context.MODE_PRIVATE)
        var preferencesAcc = preferences.getString(ACCOUNT_TAG, null)


        Log.d("doWork", preferencesAcc.toString())

        if (preferencesAcc != null) {

            Log.d("doWork", "preferencesAcc")

            try {

                if (!isAppInforegrounded()) {
                    applicationContext.startForegroundService(
                        Intent(
                            applicationContext,
                            NetworkService::class.java
                        )
                    )
                }

                Log.d("doWork", "inBlock")

                EnkodPushLibrary.initRetrofit()

                FirebaseMessaging.getInstance().deleteToken().addOnCompleteListener { task ->

                    if (task.isSuccessful) {

                        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->

                            if (task.isSuccessful) {

                                val token = task.result
                                Log.d("doWork", token.toString())
                                EnkodPushLibrary.init(applicationContext, preferencesAcc, token)

                            }
                            else {
                                Log.d("doWork", "error_token_receiving")
                            }
                        }

                    } else {
                        Log.d("doWork", "error_token_delete")
                    }
                }

            } catch (e: Exception) {

                Log.d("doWork", "error_fcm_request")

            }
        }

        return Result.success()

    }
}

class OneTimeWorkManager (context: Context, workerParameters: WorkerParameters) :
    Worker(context, workerParameters) {

    fun refreshTokenWorker() {


        val constrains = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest =

            PeriodicWorkRequestBuilder<UpdateTokenWorker>(15, TimeUnit.MINUTES)
                //.setConstraints(constrains)
                .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "refreshToken", ExistingPeriodicWorkPolicy.UPDATE, workRequest
        );
    }
    override fun doWork(): Result {

        Log.d("doWork", "WorkManagerWork")

        refreshTokenWorker()

        return Result.success()

    }
}