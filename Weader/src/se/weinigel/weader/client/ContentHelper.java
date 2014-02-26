package se.weinigel.weader.client;

import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import se.weinigel.feedparser.Article;
import se.weinigel.feedparser.Feed;
import se.weinigel.feedparser.UrlHelper;
import se.weinigel.weader.R;
import se.weinigel.weader.contract.WeadContract;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;

public class ContentHelper {
	public static final int MAX_ARTICLES = 100;
	private static final boolean LIMIT_ARTICLES = false;

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

	private void insertArticle(long feedId, Article article) {
		byte[] data;
		try {
			data = article.raw.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}

		ContentValues values = new ContentValues();
		values.put(WeadContract.Articles._FEED_ID, feedId);
		values.put(WeadContract.Articles._TITLE, article.title);
		values.put(WeadContract.Articles._PUBLISHED, article.published);
		values.put(WeadContract.Articles._UPDATED, article.updated);
		values.put(WeadContract.Articles._READ, article.read);
		values.put(WeadContract.Articles._FAVORITE, article.favorite);
		values.put(WeadContract.Articles._GUID, article.guid);
		values.put(WeadContract.Articles._CONTENT_TYPE, article.type);
		values.put(WeadContract.Articles._CONTENT, data);
		insertArticle(values);
	}

	private long insertArticle(ContentValues values) {
		Uri uri = insert(WeadContract.Articles.CONTENT_URI, values);
		return ContentUris.parseId(uri);
	}

	public void updateArticleRead(long articleId, boolean b) {
		ContentValues values = new ContentValues();
		values.put(WeadContract.Articles._READ, b ? 1 : 0);
		updateArticle(articleId, values);
	}

	public void updateArticleFavorite(long articleId, boolean b) {
		ContentValues values = new ContentValues();
		values.put(WeadContract.Articles._FAVORITE, b ? 1 : 0);
		updateArticle(articleId, values);
	}

	private void updateArticle(long articleId, ContentValues values) {
		update(WeadContract.Articles.CONTENT_URI, values,
				WeadContract.Feeds._ID + "=?",
				new String[] { Long.toString(articleId) });
	}

	public void updateAllArticlesRead(long feedId, boolean read) {
		ContentValues values = new ContentValues();
		values.put(WeadContract.Articles._READ, read ? 1 : 0);

		update(WeadContract.Articles.CONTENT_URI, values,
				WeadContract.Articles._FEED_ID + "=?",
				new String[] { Long.toString(feedId) });
	}

	private Set<String> getArticleGuids(long feedId) {
		HashSet<String> guids = new HashSet<String>();

		Cursor cursor = query(WeadContract.Articles.CONTENT_URI,
				new String[] { WeadContract.Articles._GUID },
				WeadContract.Articles._FEED_ID + "=?",
				new String[] { Long.toString(feedId) }, null);
		try {
			if (cursor.moveToFirst()) {
				int col = cursor.getColumnIndex(WeadContract.Articles._GUID);
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
		values.put(WeadContract.Feeds._URL, feed.url);
		values.put(WeadContract.Feeds._TITLE, feed.title);
		values.put(WeadContract.Feeds._LAST_MODIFIED, feed.lastModified);
		values.put(WeadContract.Feeds._ETAG, feed.etag);
		values.put(WeadContract.Feeds._REFRESH, feed.refresh);

		Uri uri = insert(WeadContract.Feeds.CONTENT_URI, values);

		long feedId = ContentUris.parseId(uri);

		List<Article> articles = feed.articles;
		if (feedId != -1 && feed.articles != null) {
			int i = 0;
			if (LIMIT_ARTICLES) {
				if (articles.size() > MAX_ARTICLES)
					i = articles.size() - MAX_ARTICLES;
			}
			while (i < articles.size()) {
				Article article = articles.get(i++);
				insertArticle(feedId, article);
			}
		}
		return feedId;
	}

	public void updateFeed(long feedId, Feed feed) {
		ContentValues values = new ContentValues();

		values.put(WeadContract.Feeds._URL, feed.url);
		values.put(WeadContract.Feeds._REFRESH, feed.refresh);
		values.put(WeadContract.Feeds._LAST_MODIFIED, feed.lastModified);
		values.put(WeadContract.Feeds._ETAG, feed.etag);

		updateFeed(feedId, values);

		List<Article> articles = feed.articles;
		if (articles != null) {
			Set<String> guids = getArticleGuids(feedId);

			int i = 0;
			if (LIMIT_ARTICLES) {
				if (articles.size() > MAX_ARTICLES)
					i = articles.size() - MAX_ARTICLES;
			}
			while (i < articles.size()) {
				Article article = articles.get(i++);
				if (!guids.contains(article.guid))
					insertArticle(feedId, article);
			}
		}
	}

	public void updateFeedRefresh(long feedId, long refresh) {
		ContentValues values = new ContentValues();
		values.put(WeadContract.Feeds._REFRESH, refresh);
		updateFeed(feedId, values);
	}

	private int updateFeed(long feedId, ContentValues values) {
		return update(WeadContract.Feeds.CONTENT_URI, values,
				WeadContract.Feeds._ID + "=?",
				new String[] { Long.toString(feedId) });
	}

	public void deleteFeed(long feedId) {
		delete(WeadContract.Feeds.CONTENT_URI, WeadContract.Feeds._ID + "=?",
				new String[] { Long.toString(feedId) });
	}

	public String getFeedUrl(long feedId) {
		Cursor cursor = query(WeadContract.Feeds.CONTENT_URI,
				new String[] { WeadContract.Feeds._URL },
				WeadContract.Feeds._ID + "=?",
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
		Cursor cursor = mContext.getContentResolver().query(
				WeadContract.Feeds.CONTENT_URI,
				new String[] { WeadContract.Feeds._ID },
				WeadContract.Feeds._URL + "=?", new String[] { url }, null);
		try {
			return cursor.moveToFirst();
		} finally {
			cursor.close();
		}
	}

	public long getFeedId(String url) {
		Cursor cursor = mContext.getContentResolver().query(
				WeadContract.Feeds.CONTENT_URI,
				new String[] { WeadContract.Feeds._ID },
				WeadContract.Feeds._URL + "=?", new String[] { url }, null);
		try {
			if (cursor.moveToFirst())
				return cursor.getLong(0);
			else
				return -1;
		} finally {
			cursor.close();
		}
	}

	public void gcFeed(long feedId) {
		delete(WeadContract.Articles.CONTENT_URI,
				"feed_id=? AND OLDER THAN ?",
				new String[] { Long.toString(feedId),
						Integer.toString(MAX_ARTICLES) });
	}

	public UrlHelper createUrlHelper() {
		String versionName;
		try {
			versionName = mContext.getPackageManager().getPackageInfo(
					mContext.getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			versionName = "unknown";
		}
		UrlHelper urlHelper = new UrlHelper();
		urlHelper.userAgent = mContext.getResources().getString(
				R.string.user_agent, versionName);
		return urlHelper;
	}
}
