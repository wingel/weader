package se.weinigel.weader;

import se.weinigel.weader.contract.WeadContract;
import android.content.ContentValues;
import android.content.Context;

public class ContentHelper {
	private Context mContext;

	public ContentHelper(Context context) {
		mContext = context;
	}

	public void setArticleRead(long articleId, boolean b) {
		ContentValues values = new ContentValues();
		values.put(WeadContract.Article.COLUMN_READ, b ? 1 : 0);
		updateArticle(articleId, values);
	}

	public void setArticleFavorite(long articleId, boolean b) {
		ContentValues values = new ContentValues();
		values.put(WeadContract.Article.COLUMN_FAVORITE, b ? 1 : 0);
		updateArticle(articleId, values);
	}

	private void updateArticle(long articleId, ContentValues values) {
		mContext.getContentResolver().update(WeadContract.Article.CONTENT_URI,
				values, WeadContract.Feed.COLUMN_ID + "=?",
				new String[] { Long.toString(articleId) });
	}

	public void setFeedRead(long feedId, boolean read) {
		ContentValues values = new ContentValues();
		values.put(WeadContract.Article.COLUMN_READ, read ? 1 : 0);

		mContext.getContentResolver().update(WeadContract.Article.CONTENT_URI,
				values, WeadContract.Feed.COLUMN_FEED_ID + "=?",
				new String[] { Long.toString(feedId) });
	}

	public void deleteFeed(long feedId) {
		mContext.getContentResolver().delete(WeadContract.Article.CONTENT_URI,
				WeadContract.Feed.COLUMN_FEED_ID + "=?",
				new String[] { Long.toString(feedId) });
		mContext.getContentResolver().delete(WeadContract.Feed.CONTENT_URI,
				WeadContract.Feed.COLUMN_ID + "=?",
				new String[] { Long.toString(feedId) });
	}
}
