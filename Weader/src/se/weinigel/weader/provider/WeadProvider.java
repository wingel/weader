package se.weinigel.weader.provider;

import java.util.ArrayList;

import se.weinigel.weader.contract.WeadContract;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.mfavez.android.feedgoal.storage.DbFeedAdapter;

public class WeadProvider extends ContentProvider {
	static final String LOG_TAG = WeadProvider.class.getSimpleName();

	private ArrayList<SimpleProvider> mColumnProviders;
	private UriMatcher mUriMatcher;

	private DbHelper mDbHelper;

	private SQLiteDatabase mDb;

	@Override
	public boolean onCreate() {
		Log.d(LOG_TAG, Helper.getMethodName());

		// This is done to let FeedGoal create the database structure on the
		// first open
		DbFeedAdapter dbFeedAdapter = new DbFeedAdapter(getContext());
		dbFeedAdapter.open();
		dbFeedAdapter.close();

		mDbHelper = new DbHelper(getContext());
		mDbHelper.hack();
		mDb = mDbHelper.getWritableDatabase();

		mColumnProviders = new ArrayList<SimpleProvider>();
		mUriMatcher = new UriMatcher(-1);

		addProvider(WeadContract.Feed.BASE_NAME, new FeedProvider(this));
		addProvider(WeadContract.Article.BASE_NAME, new ArticleProvider(this));

		return true;
	}

	private void addProvider(String path, SimpleProvider provider) {
		int id = mColumnProviders.size();
		mUriMatcher.addURI(WeadContract.AUTHORITY, path, id);
		mColumnProviders.add(provider);
	}

	private SimpleProvider getProvider(Uri uri) {
		int id = mUriMatcher.match(uri);
		if (id == -1)
			return new SimpleProvider(this);
		Log.d(LOG_TAG, Helper.getMethodName() + " " + uri + " " + id);
		return mColumnProviders.get(id);
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		Log.d(LOG_TAG, Helper.getMethodName() + " " + uri);
		try {
			return getProvider(uri).query(uri, projection, selection,
					selectionArgs, sortOrder);
		} catch (Exception e) {
			Log.d(LOG_TAG, "exception in query", e);
			return null;
		}
	}

	@Override
	public String getType(Uri uri) {
		Log.d(LOG_TAG, Helper.getMethodName() + " " + uri);
		try {
			return getProvider(uri).getType(uri);
		} catch (Exception e) {
			Log.d(LOG_TAG, "exception in getType", e);
			return null;
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		Log.d(LOG_TAG, Helper.getMethodName() + " " + uri);
		return getProvider(uri).insert(uri, values);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		Log.d(LOG_TAG, Helper.getMethodName() + " " + uri);
		return getProvider(uri).delete(uri, selection, selectionArgs);
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		Log.d(LOG_TAG, Helper.getMethodName() + " " + uri);
		return getProvider(uri).update(uri, values, selection, selectionArgs);
	}

	public SQLiteDatabase getDb() {
		return mDb;
	}
}
