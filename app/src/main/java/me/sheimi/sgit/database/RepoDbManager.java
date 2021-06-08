package me.sheimi.sgit.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import me.sheimi.android.utils.BasicFunctions;

import static me.sheimi.sgit.database.RepoContract.RepoEntry.TABLE_NAME;

/**
 * Manage entries in the persisted database tracking local repo metadata.
 */
public class RepoDbManager {
    //we always only want to have one instance of this object
    private static RepoDbManager mInstance;

    private SQLiteDatabase mWritableDatabase;
    private SQLiteDatabase mReadableDatabase;
    private RepoDbHelper mDbHelper;

    private static Map<String, Set<RepoDbObserver>> mObservers = new HashMap<String, Set<RepoDbObserver>>();

    private RepoDbManager(Context context) {
        mDbHelper = new RepoDbHelper(context);

        //it doesn't seem like it's very necessary to have the two separate SQLiteDatabase objs here
        mWritableDatabase = mDbHelper.getWritableDatabase();
        mReadableDatabase = mDbHelper.getReadableDatabase();
    }

    private static RepoDbManager getInstance() {
        //make sure we just maintain one instance of this obj (it's a "manager")
        if (mInstance == null) {
            mInstance = new RepoDbManager(BasicFunctions.getActiveActivity());
        }
        return mInstance;
    }

    public static void registerDbObserver(String table, RepoDbObserver observer) {
        Set<RepoDbObserver> set = mObservers.get(table);
        if (set == null) {
            set = new HashSet<RepoDbObserver>();
            mObservers.put(table, set);
        }
        set.add(observer);
    }

    public static void unregisterDbObserver(String table,
            RepoDbObserver observer) {
        Set<RepoDbObserver> set = mObservers.get(table);
        if (set == null)
            return;
        set.remove(observer);
    }

    public static void notifyObservers(String table) {
        //get the observers of this table
        Set<RepoDbObserver> set = mObservers.get(table);

        //no observers of this table
        if (set == null)
            return;

        //notify each observer that the table has changed
        for (RepoDbObserver observer : set) {
            observer.nofityChanged();
        }
    }

    public static void persistCredentials(long repoId, String username, String password) {
        //This class is used to store a set of values that the ContentResolver can process.
        ContentValues values = new ContentValues();

        //add the username and passwd to the ContentValues as long as they exist
        if (username != null && password != null) {
            values.put(RepoContract.RepoEntry.COLUMN_NAME_USERNAME, username);
            values.put(RepoContract.RepoEntry.COLUMN_NAME_PASSWORD, password);
        } else {
            values.put(RepoContract.RepoEntry.COLUMN_NAME_USERNAME, "");
            values.put(RepoContract.RepoEntry.COLUMN_NAME_PASSWORD, "");
        }

        updateRepo(repoId, values);
    }

    public static interface RepoDbObserver {
        public void nofityChanged();
    }

    public static Cursor searchRepo(String query) {
        return getInstance()._searchRepo(query);
    }

    private Cursor _searchRepo(String query) {
        String selection = RepoContract.RepoEntry.COLUMN_NAME_LOCAL_PATH
                + " LIKE ? OR " + RepoContract.RepoEntry.COLUMN_NAME_REMOTE_URL
                + " LIKE ? OR "
                + RepoContract.RepoEntry.COLUMN_NAME_LATEST_COMMITTER_UNAME
                + " LIKE ? OR "
                + RepoContract.RepoEntry.COLUMN_NAME_LATEST_COMMIT_MSG
                + " LIKE ?";
        query = "%" + query + "%";
        String[] selectionArgs = { query, query, query, query };
        Cursor cursor = mReadableDatabase.query(true,
                TABLE_NAME,
                RepoContract.RepoEntry.ALL_COLUMNS, selection, selectionArgs,
                null, null, null, null);
        return cursor;
    }

    public static Cursor queryAllRepo() {
        return getInstance()._queryAllRepo();
    }

    private Cursor _queryAllRepo() {
        Cursor query = mReadableDatabase.query(true,
                TABLE_NAME,
                RepoContract.RepoEntry.ALL_COLUMNS, null, null, null, null,
                null, null);
        return query;
    }

    public static long getNumRowsInTab() {
        return getInstance()._getNumRowsInTab();
    }

    public long _getNumRowsInTab() {
        return DatabaseUtils.queryNumEntries(mReadableDatabase, TABLE_NAME);
    }

    public static Cursor getRepoById(long id) {
        return getInstance()._getRepoById(id);
    }

    private Cursor _getRepoById(long id) {
        Cursor cursor = mReadableDatabase.query(true,
                TABLE_NAME,
                RepoContract.RepoEntry.ALL_COLUMNS, RepoContract.RepoEntry._ID
                        + "= ?", new String[] { String.valueOf(id) }, null,
                null, null, null);
        if (cursor.getCount() < 1) {
            cursor.close();
            return null;
        }
        return cursor;
    }

    public static long importRepo(String localPath, String status) {
        return createRepo(localPath, "", status);
    }

    public static void setLocalPath(long repoId, String path) {
        ContentValues values = new ContentValues();
        values.put(RepoContract.RepoEntry.COLUMN_NAME_LOCAL_PATH, path);
        updateRepo(repoId, values);
    }

    public static long createRepo(String localPath, String remoteURL, String status) {
        ContentValues values = new ContentValues();

        //make sure none of the values are null, otherwise will cause probs
        values.put(RepoContract.RepoEntry.COLUMN_NAME_LOCAL_PATH, localPath);
        values.put(RepoContract.RepoEntry.COLUMN_NAME_REMOTE_URL, remoteURL);
        values.put(RepoContract.RepoEntry.COLUMN_NAME_REPO_STATUS, status);

        long id = getInstance().mWritableDatabase.insert(TABLE_NAME, null, values);
        notifyObservers(TABLE_NAME);

        Log.i("RepoDbManager", "createRepo(): id is " + id);
        return id;
    }

    public static void updateRepo(long id, ContentValues values) {
        //select the appropriate repo by passing ID to sqlite cmd
        String selection = RepoContract.RepoEntry._ID + " = ?";

        //argument for sqlite cmd (the repo id number)
        String[] selectionArgs = { String.valueOf(id) };

        //update the database wth the new ContentValues
        getInstance().mWritableDatabase.update(TABLE_NAME, values, selection, selectionArgs);

        notifyObservers(TABLE_NAME);
    }

    public static void deleteRepo(long id) {
        getInstance()._deleteRepo(id);
    }

    private void _deleteRepo(long id) {
        String selection = RepoContract.RepoEntry._ID + " = ?";
        String[] selectionArgs = { String.valueOf(id) };
        mWritableDatabase.delete(TABLE_NAME, selection, selectionArgs);
        notifyObservers(TABLE_NAME);
    }

}
