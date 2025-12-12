package com.lucid.app

import android.app.Application
import com.lucid.app.data.LucidPreferences

/**
 * LUCID - The Cognitive Firewall
 *
 * "We are not building a tool. We are building Digital Silence.
 * And in that silence, people can finally hear themselves think again."
 */
class LucidApplication : Application() {

    lateinit var preferences: LucidPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        preferences = LucidPreferences(this)
    }

    companion object {
        lateinit var instance: LucidApplication
            private set
    }
}
