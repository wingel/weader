package se.weinigel.weader.provider;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

public class SimpleProvider {
	private WeadProvider mContentProvider;

	public SimpleProvider(WeadProvider contentProvider) {
		mContentProvider = contentProvider;
	}
	
	protected SQLiteDatabase getDb() {
		return mContentProvider.getDb();
	}

	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		throw new IllegalArgumentException("Unsupported URI for query: " + uri);
	}

	public String getType(Uri uri) {
		throw new IllegalArgumentException("Unsupported URI for getType: " + uri);
	}

	public Uri insert(Uri uri, ContentValues values) {
		throw new IllegalArgumentException("Unsupported URI for insert: " + uri);
	}

	public int delete(Uri uri, String selection, String[] selectionArgs) {
		throw new IllegalArgumentException("Unsupported URI for delete: " + uri);
	}

	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		throw new IllegalArgumentException("Unsupported URI for update: " + uri);
	}
}
