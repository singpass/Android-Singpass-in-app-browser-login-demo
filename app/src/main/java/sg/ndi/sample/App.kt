package sg.ndi.sample

import android.app.Application
import android.content.Context
import android.os.Handler
import android.widget.Toast
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.initialize

val Application.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        Firebase.initialize(this)
        Firebase.appCheck.installAppCheckProviderFactory(
            if (BuildConfig.DEBUG) DebugAppCheckProviderFactory.getInstance()
            else PlayIntegrityAppCheckProviderFactory.getInstance()
        )
    }

    companion object {

        private var generalMessage: Toast? = null

        fun toastMessage(
            message: String,
            context: Context,
            duration: Int = Toast.LENGTH_SHORT,
            forceNew: Boolean = false,
            customise: ((Toast) -> Unit)? = null
        ) {

            // Flag to force cancel and create a new message
            // Because calling this method many times will not reset the toast duration
            // Thus would fade away prematurely
            if (forceNew) {
                generalMessage?.cancel()
                generalMessage = null
            }

            val toastRunner = {
                val toastMessage = generalMessage
                if (toastMessage != null) {
                    customise?.invoke(toastMessage)
                    toastMessage.setText(message)
                    toastMessage.show()
                } else {
                    val newMessage = Toast.makeText(context, message, duration)
                    customise?.invoke(newMessage)
                    newMessage.show()
                    generalMessage = newMessage
                }
            }

            Handler(context.mainLooper).post(toastRunner)
        }
    }
}
