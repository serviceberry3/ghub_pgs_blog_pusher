package me.sheimi.sgit

import android.app.Application
import android.content.Context
import android.util.Log
import io.sentry.Sentry
import io.sentry.android.AndroidSentryClientFactory
import me.sheimi.android.utils.SecurePrefsException
import me.sheimi.android.utils.SecurePrefsHelper
import me.sheimi.sgit.preference.PreferenceHelper
import org.eclipse.jgit.transport.CredentialsProvider
import timber.log.Timber
import weiner.noah.blogpusher.BuildConfig
import weiner.noah.blogpusher.R

/**
 * Custom Application Singleton
 */
open class MGitApplication : Application() {
    var securePrefsHelper: SecurePrefsHelper? = null
    var prefenceHelper: PreferenceHelper? = null

    companion object {
        private lateinit var mContext: Context
        private lateinit var mCredentialsProvider: CredentialsProvider
        val context: Context?
            get() = mContext

        @JvmStatic fun getContext(): MGitApplication {
            return mContext as MGitApplication
        }

        @JvmStatic fun getJschCredentialsProvider(): CredentialsProvider {
            return mCredentialsProvider
        }
    }

    override fun onCreate() {
        super.onCreate()
        // only init Sentry if not debug build
        if (!BuildConfig.DEBUG) {
            Sentry.init(AndroidSentryClientFactory(this))
            Log.d("SENTRY", "SENTRY Configured")
        }
        mContext = applicationContext
        setAppVersionPref()
        prefenceHelper = PreferenceHelper(this)
        try {
            securePrefsHelper = SecurePrefsHelper(this)
            mCredentialsProvider = AndroidJschCredentialsProvider(securePrefsHelper)
        } catch (e: SecurePrefsException) {
            Timber.e(e)
        }
    }

    private fun setAppVersionPref() {
        val sharedPreference = getSharedPreferences(
            getString(R.string.preference_file_key),
            Context.MODE_PRIVATE)
        val version = BuildConfig.VERSION_NAME
        sharedPreference
            .edit()
            .putString(getString(R.string.preference_key_app_version), version)
            .apply()
    }
}
