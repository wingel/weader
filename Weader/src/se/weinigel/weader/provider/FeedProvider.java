package se.weinigel.weader.provider;

import se.weinigel.weader.contract.WeadContract;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.util.Log;

public class FeedProvider extends SimpleProvider {
	private final String LOG_TAG = getClass().getSimpleName();

	public FeedProvider(WeadProvider contentProvider) {
		super(contentProvider);
	}

	@Override
	public String getType(Uri uri) {
		return WeadContract.Feeds.CONTENT_TYPE;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		boolean wantUnread = false;
		String sql = "SELECT ";
		for (int i = 0; i < projection.length; i++) {
			if (i > 0)
				sql += ",";
			String column = projection[i];
			if (WeadContract.Feeds._UNREAD.equals(column)) {
				sql += "COUNT(article._id) AS " + WeadContract.Feeds._UNREAD;
				wantUnread = true;
			} else if (DbHelper.FEED_COLUMNS.contains(column)) {
				sql += "feed." + column + " AS " + column;
			} else
				throw new SQLException("invalid column " + column);
		}

		sql += " FROM feed";

		if (wantUnread) {
			// @formatter:off
			sql += " OUTER LEFT JOIN article" 
			     + " ON feed._id=article.feed_id"
				 + " AND article.read=0 GROUP BY feed._id";
			// @formatter:on
		}

		if (selection != null) {
			// TODO validate selection so that it doesn't do anything strange
			// For now just limit it to the queries Weader uses
			if (!"_id=?".equals(selection) && !"url=?".equals(selection)) {
				Log.d(LOG_TAG, "invalid selection: " + selection);
				throw new SQLException("invalid selection");
			}
			sql += " WHERE " + selection;
		}

		if (sortOrder != null) {
			// TODO validate sort order so that it doesn't do anything strange.
			// For now just limit it to the sort order the Weader GUI uses
			if (!"title COLLATE LOCALIZED ASC".equals(sortOrder))
				throw new SQLException("invalid sort order");
			sql += " ORDER BY " + sortOrder;
		}

		Log.d(LOG_TAG, sql);
		return getDb().rawQuery(sql, selectionArgs);
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		long id = getDb().insert("feed", null, values);
		return Uri.parse(WeadContract.Feeds.CONTENT_URI + "/"
				+ Long.toString(id));
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// TODO validate selection so that it doesn't do anything strange
		// For now just limit it to the queries Weader uses
		if (!"_id=?".equals(selection)) {
			Log.d(LOG_TAG, "invalid selection: " + selection);
			throw new SQLException("invalid selection");
		}
		return getDb().update("feed", values, selection, selectionArgs);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO validate selection so that it doesn't do anything strange
		// For now just limit it to the queries Weader uses
		if (!"_id=?".equals(selection)) {
			Log.d(LOG_TAG, "invalid selection: " + selection);
			throw new SQLException("invalid selection");
		}
		return getDb().delete("feed", selection, selectionArgs);
	}
}
