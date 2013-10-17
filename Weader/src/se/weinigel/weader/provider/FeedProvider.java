package se.weinigel.weader.provider;

import java.util.Arrays;
import java.util.List;

import se.weinigel.weader.contract.WeadContract;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

import com.mfavez.android.feedgoal.storage.DbSchema;

public class FeedProvider extends SimpleProvider {
	private final String LOG_TAG = getClass().getSimpleName();

	public FeedProvider(WeadProvider contentProvider) {
		super(contentProvider);
	}

	@Override
	public String getType(Uri uri) {
		return WeadContract.Feed.CONTENT_TYPE;
	}

	@SuppressWarnings("unused")
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		List<String> columns = Arrays.asList(projection);
		boolean wantsUnread = columns
				.contains(WeadContract.Feed.COLUMN_UNREAD);

		String table = DbSchema.FeedSchema.TABLE_NAME;
		if (wantsUnread)
			table = "feeds_unread";

		if (sortOrder == null)
			sortOrder = WeadContract.Feed.SORT_ORDER_DEFAULT;

		Cursor cursor;
		if (false) {
			cursor = getDb().query(table, projection, selection, selectionArgs,
					null, null, sortOrder);
		} else {
			SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
			qb.setTables(table);
			String sql = qb.buildQuery(projection, selection, selectionArgs,
					null, null, sortOrder, null);
			Log.d(LOG_TAG, sql);
			cursor = getDb().rawQuery(sql, selectionArgs);
		}

		return cursor;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		return getDb().delete(DbSchema.FeedSchema.TABLE_NAME, selection,
				selectionArgs);
	}
}
