package se.weinigel.weader.provider;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

class DbHelper extends SQLiteOpenHelper {
	private final String LOG_TAG = getClass().getSimpleName();

	public static final String DATABASE_NAME = "feeds.db";
	public static final int DATABASE_VERSION = 1;

	// @formatter:off

	public static final Set<String> FEED_COLUMNS = 
		Collections.unmodifiableSet(
		new HashSet<String>(Arrays.asList(new String[] {
			"_id", "url", "refresh", "title", "last_modified", "etag"
		})));

	private static final String CREATE_FEED = 
		"CREATE TABLE feed ("
		+ "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
		+ "url TEXT NOT NULL,"
		+ "refresh INTEGER NOT NULL,"
		+ "title TEXT NOT NULL,"
		+ "last_modified TEXT,"
		+ "etag TEXT);";

	public static final Set<String> ARTICLE_COLUMNS =
		Collections.unmodifiableSet(
		new HashSet<String>(Arrays.asList(new String[] {
			"_id", "feed_id", "title", "published", "updated",
			"read",	"favorite", "guid"
		})));

	private static final String CREATE_ARTICLE = 
		"CREATE TABLE article ("
		+ "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
		+ "feed_id INTEGER NOT NULL," 
		+ "title TEXT NOT NULL,"
		+ "published TEXT," 
		+ "updated TEXT," 
		+ "read INTEGER NOT NULL,"
		+ "favorite INTEGER NOT NULL," 
		+ "guid TEXT NOT NULL,"
		+ "FOREIGN KEY(feed_id) REFERENCES feed(_id) ON DELETE CASCADE);";

	public static final Set<String> ARTICLE_EXTRA_COLUMNS =
		Collections.unmodifiableSet(
		new HashSet<String>(Arrays.asList(new String[] {
			"_id", "content_type", "content"
		})));

	private static final String CREATE_ARTICLE_EXTRA = 
		"CREATE TABLE article_extra ("
		+ "_id INTEGER PRIMARY KEY,"
		+ "content_type TEXT,"
		+ "content BLOB,"
		+ "FOREIGN KEY(_id) REFERENCES article(_id) ON DELETE CASCADE);";

	// @formatter:on

	public DbHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	void execSQL(SQLiteDatabase db, String sql) {
		Log.d(LOG_TAG, sql);
		db.execSQL(sql);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// This is needed to get working ON DELETE CASCADE
		execSQL(db, "PRAGMA foreign_keys = ON;");

		execSQL(db, CREATE_FEED);
		execSQL(db, CREATE_ARTICLE);
		execSQL(db, CREATE_ARTICLE_EXTRA);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}
}
