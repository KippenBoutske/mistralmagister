package nl.mistralmagister

import android.app.Application
import nl.mistralmagister.data.Container
import nl.mistralmagister.notify.Notifier
import nl.mistralmagister.work.SyncWorker

class MagisterApp : Application() {
    lateinit var container: Container
        private set

    override fun onCreate() {
        super.onCreate()
        container = Container(this)
        Notifier.ensureChannels(this)
        SyncWorker.enqueue(this)
    }
}

fun android.content.Context.magisterApp(): MagisterApp = applicationContext as MagisterApp
