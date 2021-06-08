package me.sheimi.sgit.preference;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;

import timber.log.Timber;
import weiner.noah.blogpusher.R;

/**
 * Clean, central access to getting/setting user preferences
 */

public class PreferenceHelper {

    private static final int DEFAULT_INT = 0;
    private static final boolean DEFAULT_BOOLEAN = false;
    private static final String DEFAULT_STRING = "";

    private static final String PRIVACY_ACCEPTED_KEY = "privacy.accepted";
    private static final int PRIVACY_ACCEPTED_VERSION = 1;

    private final Context mContext;

    public PreferenceHelper(Context context) {
        mContext = context.getApplicationContext();
    }

    /**
     * Returns the root directory on device storage in which new local repos will be stored
     *
     * @return null if the custom repo location user preference is *not* set
     */
    public File getRepoRoot() {
        String repoRootDir = getString(mContext.getString(R.string.pref_key_repo_root_location));
        if (repoRootDir != null && !repoRootDir.isEmpty()) {
            return new File(repoRootDir);
        } else {
            return null;
        }
    }

    public void setRepoRoot(String repoRootPath) {
        edit(mContext.getString(R.string.pref_key_repo_root_location), repoRootPath);
        Timber.d("set root:"+repoRootPath);
    }

    public void setPrivacyAccepted() {
        edit(PRIVACY_ACCEPTED_KEY, PRIVACY_ACCEPTED_VERSION);
        Timber.d("Privacy policy accepted version:"+PRIVACY_ACCEPTED_VERSION);
    }


    protected SharedPreferences getSharedPrefs() {
        return mContext.getSharedPreferences(
            mContext.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
    }


    private void edit(String name, String value) {
        SharedPreferences.Editor editor = getSharedPrefs().edit();
        editor.putString(name, value);
        editor.apply();
    }

    private void edit(String name, int value) {
        SharedPreferences.Editor editor =  getSharedPrefs().edit();
        editor.putInt(name, value);
        editor.apply();
    }

    private void edit(String name, boolean value) {
        SharedPreferences.Editor editor =  getSharedPrefs().edit();
        editor.putBoolean(name, value);
        editor.apply();
    }

    private String getString(String name) {
        return getSharedPrefs().getString(name, DEFAULT_STRING);
    }

    private int getInt(String name) {
        return  getSharedPrefs().getInt(name, DEFAULT_INT);
    }

    private boolean getBoolean(String name) {
        return  getSharedPrefs().getBoolean(name, DEFAULT_BOOLEAN);
    }

    public boolean isPrivacyAccepted() {
        return getSharedPrefs().getInt(PRIVACY_ACCEPTED_KEY, 0) == PRIVACY_ACCEPTED_VERSION;
    }
}
