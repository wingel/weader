package se.weinigel.weader.provider;

import se.weinigel.weader.contract.WeadContract;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.util.Log;

public class ArticleProvider extends SimpleProvider {
	private final String LOG_TAG = getClass().getSimpleName();

	public ArticleProvider(WeadProvider contentProvider) {
		super(contentProvider);
	}

	@Override
	public String getType(Uri uri) {
		return WeadContract.Articles.CONTENT_TYPE;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		boolean wantExtra = false;

		String sql = "SELECT ";
		for (int i = 0; i < projection.length; i++) {
			if (i > 0)
				sql += ",";
			String column = projection[i];
			if (DbHelper.ARTICLE_COLUMNS.contains(column)) {
				sql += "article." + column + " AS " + column;
			} else if (DbHelper.ARTICLE_EXTRA_COLUMNS.contains(column)) {
				sql += "article_extra." + column + " AS " + column;
				wantExtra = true;
			} else
				throw new SQLException("invalid column " + column);
		}

		sql += " FROM article";

		if (wantExtra) {
			// @formatter:off
			sql += " OUTER LEFT JOIN article_extra"
			     + " ON article_extra._id=article._id";
			// @formatter:on
		}

		if (selection != null) {
			// TODO validate selection so that it doesn't do anything strange
			// For now just limit it to the queries Weader uses
			if ("_id=?".equals(selection)) {
				selection = "article._id=?";
			} else if (!"feed_id=?".equals(selection)
					&& !"guid=?".equals(selection)) {
				Log.d(LOG_TAG, "invalid selection: " + selection);
				throw new SQLException("invalid selection");
			}
			sql += " WHERE " + selection;
		}

		if (sortOrder != null) {
			// TODO validate sort order so that it doesn't do anything strange.
			// For now just limit it to the sort order the Weader GUI uses
			if (!"_id DESC".equals(sortOrder))
				throw new SQLException("invalid sort order");
			sql += " ORDER BY " + sortOrder;
		}

		Log.d(LOG_TAG, sql);
		return getDb().rawQuery(sql, selectionArgs);
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		ContentValues extraValues = new ContentValues();

		if (values.containsKey(WeadContract.Articles._CONTENT_TYPE)) {
			extraValues.put(WeadContract.Articles._CONTENT_TYPE,
					values.getAsString(WeadContract.Articles._CONTENT_TYPE));
			values.remove(WeadContract.Articles._CONTENT_TYPE);
		}
		if (values.containsKey(WeadContract.Articles._CONTENT)) {
			extraValues.put(WeadContract.Articles._CONTENT,
					values.getAsByteArray(WeadContract.Articles._CONTENT));
			values.remove(WeadContract.Articles._CONTENT);
		}

		long id = getDb().insert("article", null, values);

		extraValues.put("_id", id);
		getDb().insert("article_extra", null, extraValues);

		return Uri.parse(WeadContract.Articles.CONTENT_URI + "/"
				+ Long.toString(id));
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		ContentValues extraValues = new ContentValues();

		if (values.containsKey(WeadContract.Articles._CONTENT_TYPE)) {
			extraValues.put(WeadContract.Articles._CONTENT_TYPE,
					values.getAsString(WeadContract.Articles._CONTENT_TYPE));
			values.remove(WeadContract.Articles._CONTENT_TYPE);
		}
		if (values.containsKey(WeadContract.Articles._CONTENT)) {
			extraValues.put(WeadContract.Articles._CONTENT,
					values.getAsByteArray(WeadContract.Articles._CONTENT));
			values.remove(WeadContract.Articles._CONTENT);
		}

		if (extraValues.size() > 0) {
			getDb().update("article_extra", extraValues, selection,
					selectionArgs);
		}

		if (!"_id=?".equals(selection) && !"feed_id=?".equals(selection)) {
			Log.d(LOG_TAG, "invalid selection: " + selection);
			throw new SQLException("invalid selection");
		}

		return getDb().update("article", values, selection, selectionArgs);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		if ("feed_id=? AND OLDER THAN ?".equals(selection)) {
			// @formatter:off
			selection = "_id IN (SELECT _id FROM article"
					+ " WHERE feed_id=? AND read=1 AND favorite=0"
					+ " ORDER BY _id DESC LIMIT ?,-1)";
			// @formatter:on
		} else if (!"_id=?".equals(selection)) {
			Log.d(LOG_TAG, "invalid selection: " + selection);
			throw new SQLException("invalid selection");
		}

		Log.d(LOG_TAG, "delete " + selection);

		return getDb().delete("article", selection, selectionArgs);
	}
}
