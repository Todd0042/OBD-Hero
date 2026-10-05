package com.obdhero.app

import android.app.Application
import com.obdhero.app.domain.BmwSleepLockManager
import com.obdhero.app.obd.ObdConnectionManager

class ObdHeroApp : Application() {

    lateinit var obdConnectionManager: ObdConnectionManager
        private set

    lateinit var bmwSleepLockManager: BmwSleepLockManager
        private set

    override fun onCreate() {
        super.onCreate()
        obdConnectionManager = ObdConnectionManager()
        bmwSleepLockManager = BmwSleepLockManager(this)
    }
}
