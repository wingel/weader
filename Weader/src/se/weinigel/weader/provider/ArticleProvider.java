package se.weinigel.weader.provider;

import se.weinigel.weader.contract.WeadContract;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

public class ArticleProvider extends SimpleProvider {
	private final String LOG_TAG = getClass().getSimpleName();

	public ArticleProvider(WeadProvider contentProvider) {
		super(contentProvider);
	}

	@Override
	public String getType(Uri uri) {
		return WeadContract.Article.CONTENT_TYPE;
	}

	@SuppressWarnings("unused")
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		if (sortOrder == null)
			sortOrder = WeadContract.Article.SORT_ORDER_DEFAULT;

		Cursor cursor;
		if (false) {
			cursor = getDb().query(DbSchema.ItemSchema.TABLE_NAME, projection,
					selection, selectionArgs, null, null, sortOrder);
		} else {
			SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
			qb.setTables(DbSchema.ItemSchema.TABLE_NAME);
			String sql = qb.buildQuery(projection, selection, selectionArgs,
					null, null, sortOrder, null);
			Log.d(LOG_TAG, sql);
			cursor = getDb().rawQuery(sql, selectionArgs);
		}

		return cursor;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		long id = getDb().insert(DbSchema.ItemSchema.TABLE_NAME, null, values);
		return Uri.parse(WeadContract.Article.CONTENT_URI + "/"
				+ Long.toString(id));
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		return getDb().update(DbSchema.ItemSchema.TABLE_NAME, values,
				selection, selectionArgs);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		return getDb().delete(DbSchema.ItemSchema.TABLE_NAME, selection,
				selectionArgs);
	}
}
