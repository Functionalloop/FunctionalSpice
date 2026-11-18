package com.functional.spice

import android.app.Application
import com.functional.spice.data.printer.PrintQueue
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SpiceApplication : Application() {

    @Inject
    lateinit var printQueue: PrintQueue

    override fun onCreate() {
        super.onCreate()
        // Prints bills that were saved while the printer was offline, as soon as it reconnects
        printQueue.start()
    }
}
