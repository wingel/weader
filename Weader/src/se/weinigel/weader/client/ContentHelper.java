package se.weinigel.weader.client;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import se.weinigel.weader.contract.WeadContract;
import se.weinigel.weader.provider.DbSchema;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.mfavez.android.feedgoal.common.Feed;
import com.mfavez.android.feedgoal.common.Item;

public class ContentHelper {
	private Context mContext;

	public ContentHelper(Context context) {
		mContext = context;
	}

	/* Generic wrapper stuff */

	private ContentResolver getContentResolver() {
		return mContext.getContentResolver();
	}

	private Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		return getContentResolver().query(uri, projection, selection,
				selectionArgs, sortOrder);
	}

	private Uri insert(Uri uri, ContentValues values) {
		return getContentResolver().insert(uri, values);
	}

	private int update(Uri uri, ContentValues values, String where,
			String[] selectionArgs) {
		return getContentResolver().update(uri, values, where, selectionArgs);
	}

	private int delete(Uri uri, String where, String[] selectionArgs) {
		return getContentResolver().delete(uri, where, selectionArgs);
	}

	/* Articles */

	private void insertArticle(long feedId, Item item) {
		ContentValues values = new ContentValues();
		values.put(DbSchema.ItemSchema.COLUMN_FEED_ID, feedId);
		values.put(DbSchema.ItemSchema.COLUMN_LINK, item.getLink().toString());
		values.put(DbSchema.ItemSchema.COLUMN_GUID, item.getGuid());
		values.put(DbSchema.ItemSchema.COLUMN_TITLE, item.getTitle());
		if (item.getDescription() == null)
			values.putNull(DbSchema.ItemSchema.COLUMN_DESCRIPTION);
		else
			values.put(DbSchema.ItemSchema.COLUMN_DESCRIPTION,
					item.getDescription());
		if (item.getContent() == null)
			values.putNull(DbSchema.ItemSchema.COLUMN_CONTENT);
		else
			values.put(DbSchema.ItemSchema.COLUMN_CONTENT, item.getContent());
		if (item.getImage() == null)
			values.putNull(DbSchema.ItemSchema.COLUMN_IMAGE);
		else
			values.put(DbSchema.ItemSchema.COLUMN_IMAGE, item.getImage()
					.toString());
		values.put(DbSchema.ItemSchema.COLUMN_PUBDATE, item.getPubdate()
				.getTime());
		int state = DbSchema.ON;
		if (!item.isFavorite())
			state = DbSchema.OFF;
		values.put(DbSchema.ItemSchema.COLUMN_FAVORITE, state);
		if (!item.isRead())
			state = DbSchema.OFF;
		else
			state = DbSchema.ON;
		values.put(DbSchema.ItemSchema.COLUMN_READ, state);
		insertArticle(values);
	}

	private void insertArticle(ContentValues values) {
		insert(WeadContract.Article.CONTENT_URI, values);
	}

	public void updateArticleRead(long articleId, boolean b) {
		ContentValues values = new ContentValues();
		values.put(WeadContract.Article.COLUMN_READ, b ? 1 : 0);
		updateArticle(articleId, values);
	}

	public void updateArticleFavorite(long articleId, boolean b) {
		ContentValues values = new ContentValues();
		values.put(WeadContract.Article.COLUMN_FAVORITE, b ? 1 : 0);
		updateArticle(articleId, values);
	}

	private void updateArticle(long articleId, ContentValues values) {
		update(WeadContract.Article.CONTENT_URI, values,
				WeadContract.Feed.COLUMN_ID + "=?",
				new String[] { Long.toString(articleId) });
	}

	public void updateAllArticlesRead(long feedId, boolean read) {
		ContentValues values = new ContentValues();
		values.put(WeadContract.Article.COLUMN_READ, read ? 1 : 0);

		update(WeadContract.Article.CONTENT_URI, values,
				WeadContract.Article.COLUMN_FEED_ID + "=?",
				new String[] { Long.toString(feedId) });
	}

	private Set<String> getArticleGuids(long feedId) {
		HashSet<String> guids = new HashSet<String>();

		Cursor cursor = query(WeadContract.Article.CONTENT_URI,
				new String[] { WeadContract.Article.COLUMN_GUID },
				WeadContract.Article.COLUMN_FEED_ID + "=?",
				new String[] { Long.toString(feedId) }, null);
		try {
			if (cursor.moveToFirst()) {
				int col = cursor
						.getColumnIndex(WeadContract.Article.COLUMN_GUID);
				do {
					guids.add(cursor.getString(col));
				} while (cursor.moveToNext());
			}
			return guids;
		} finally {
			cursor.close();
		}
	}

	/* Feeds */

	public long insertFeed(Feed feed) {
		ContentValues values = new ContentValues();
		values.put(DbSchema.FeedSchema.COLUMN_URL, feed.getURL().toString());
		if (feed.getHomePage() == null)
			values.putNull(DbSchema.FeedSchema.COLUMN_HOMEPAGE);
		else
			values.put(DbSchema.FeedSchema.COLUMN_HOMEPAGE, feed.getHomePage()
					.toString());
		if (feed.getTitle() == null)
			values.putNull(DbSchema.FeedSchema.COLUMN_TITLE);
		else
			values.put(DbSchema.FeedSchema.COLUMN_TITLE, feed.getTitle());
		if (feed.getType() == null)
			values.putNull(DbSchema.FeedSchema.COLUMN_TYPE);
		else
			values.put(DbSchema.FeedSchema.COLUMN_TYPE, feed.getType());
		if (feed.getRefresh() == null)
			values.putNull(DbSchema.FeedSchema.COLUMN_REFRESH);
		else
			values.put(DbSchema.FeedSchema.COLUMN_REFRESH, feed.getRefresh()
					.getTime());
		int state = DbSchema.ON;
		if (!feed.isEnabled())
			state = DbSchema.OFF;
		values.put(DbSchema.FeedSchema.COLUMN_ENABLE, state);

		return insertFeed(values, feed.getItems());
	}

	public long insertFeed(ContentValues values, List<Item> items) {
		Uri uri = insert(WeadContract.Feed.CONTENT_URI, values);
		long feedId = ContentUris.parseId(uri);
		if (feedId != -1 && items != null) {
			Iterator<Item> iterator = items.iterator();
			while (iterator.hasNext()) {
				Item item = iterator.next();
				insertArticle(feedId, item);
			}
		}
		return feedId;
	}

	public boolean updateFeed(long feedId, Feed feed) {
		ContentValues values = new ContentValues();

		if (feed.getRefresh() == null)
			values.putNull(DbSchema.FeedSchema.COLUMN_REFRESH);
		else
			values.put(DbSchema.FeedSchema.COLUMN_REFRESH, feed.getRefresh()
					.getTime());

		boolean feedUpdated = updateFeed(feedId, values) > 0;

		List<Item> items = feed.getItems();
		if (feedUpdated && items != null) {
			Set<String> guids = getArticleGuids(feedId);

			Iterator<Item> iterator = items.listIterator();
			Item item = null;
			while (iterator.hasNext()) {
				item = iterator.next();
				if (!guids.contains(item.getGuid()))
					insertArticle(feedId, item);
			}
		}

		return feedUpdated;
	}

	private int updateFeed(long feedId, ContentValues values) {
		return update(WeadContract.Feed.CONTENT_URI, values,
				WeadContract.Feed.COLUMN_ID + "=?",
				new String[] { Long.toString(feedId) });
	}

	public void deleteFeed(long feedId) {
		delete(WeadContract.Article.CONTENT_URI,
				WeadContract.Article.COLUMN_FEED_ID + "=?",
				new String[] { Long.toString(feedId) });
		delete(WeadContract.Feed.CONTENT_URI, WeadContract.Feed.COLUMN_ID
				+ "=?", new String[] { Long.toString(feedId) });
	}

	public String getFeedUrl(long feedId) {
		Cursor cursor = query(WeadContract.Feed.CONTENT_URI,
				new String[] { WeadContract.Feed.COLUMN_URL },
				WeadContract.Feed.COLUMN_ID + "=?",
				new String[] { Long.toString(feedId) }, null);
		try {
			if (!cursor.moveToFirst())
				return null;

			return cursor.getString(0);
		} finally {
			cursor.close();
		}
	}

	public boolean hasFeed(String url) {
		Cursor cursor = mContext.getContentResolver()
				.query(WeadContract.Feed.CONTENT_URI,
						new String[] { WeadContract.Feed.COLUMN_ID },
						WeadContract.Feed.COLUMN_URL + "=?",
						new String[] { url }, null);
		try {
			return cursor.moveToFirst();
		} finally {
			cursor.close();
		}
	}

	public void gcFeed(long feedId) {
		// TODO perform a garbage collect and remove old items
	}
}
