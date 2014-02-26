package se.weinigel.weader.service;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.xmlpull.v1.XmlPullParser;

import se.weinigel.feedparser.Feed;
import se.weinigel.feedparser.FeedParser;
import se.weinigel.feedparser.UrlHelper;
import se.weinigel.feedparser.XmlHelper;
import se.weinigel.weader.client.ContentHelper;
import se.weinigel.weader.contract.WeadContract;
import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class UpdateFeedService extends IntentService {
	private static final String LOG_TAG = UpdateFeedService.class
			.getSimpleName();

	public static final String RESPONSE_ACTION = UpdateFeedService.class
			.getName() + ".RESPONSE_ACTION";

	public static final String RESPONSE = "response";

	public static final String RESPONSE_START = "start";
	public static final String RESPONSE_STOP = "stop";

	private ContentHelper mContentHelper;

	public UpdateFeedService() {
		super(LOG_TAG);
		mContentHelper = new ContentHelper(this);
	}

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		long feedId = intent.getLongExtra(WeadContract.Feeds._ID, -1);
		Log.d(LOG_TAG, Helper.getMethodName() + " " + feedId);
		if (feedId == -1)
			return;

		try {
			updateFeed(feedId);
		} catch (Exception e) {
			Log.d(LOG_TAG, "exception", e);
		}
	}

	private void updateFeed(long feedId) {
		String url = mContentHelper.getFeedUrl(feedId);

		Log.d(LOG_TAG, "loading feed " + url);

		respond(feedId, RESPONSE_START);
		try {
			UrlHelper urlHelper = mContentHelper.createUrlHelper();

			Cursor cursor;
			cursor = getContentResolver()
					.query(WeadContract.Feeds.CONTENT_URI,
							new String[] { WeadContract.Feeds._URL,
									WeadContract.Feeds._LAST_MODIFIED,
									WeadContract.Feeds._ETAG,
									WeadContract.Feeds._TITLE },
							WeadContract.Feeds._ID + "=?",
							new String[] { Long.toString(feedId) }, null);
			cursor.moveToFirst();
			if (cursor.isAfterLast()) {
				Log.e(LOG_TAG, "no feed found for id " + feedId);
				return;
			}
			String path = cursor.getString(0);
			urlHelper.lastModified = cursor.getString(1);
			urlHelper.etag = cursor.getString(2);
			String title = cursor.getString(3);
			cursor.close();

			Log.d(LOG_TAG, "Fetching feed " + path + " " + title);

			URLConnection conn = urlHelper.connect(new URL(path));
			if (conn == null) {
				Log.d(LOG_TAG, "not modified");
				mContentHelper.updateFeedRefresh(feedId,
						System.currentTimeMillis());
				return;
			}

			InputStream in = conn.getInputStream();
			XmlPullParser parser = XmlHelper.createParser(in);

			Feed feed = FeedParser.parseFeed(parser);
			feed.url = conn.getURL().toExternalForm();
			feed.lastModified = conn.getHeaderField("Last-Modified");
			feed.etag = conn.getHeaderField("ETag");

			if (!path.equals(feed.url))
				Log.d(LOG_TAG, "feed has moved " + path + " -> " + feed.url);
			System.out.println("lastModified: " + feed.lastModified);
			System.out.println("etag: " + feed.etag);

			feed.refresh = System.currentTimeMillis();

			Log.e(LOG_TAG, "updateFeed");
			mContentHelper.updateFeed(feedId, feed);

		} catch (Exception e) {
			Log.e(LOG_TAG, "updateFeed failed", e);
			mContentHelper
					.updateFeedRefresh(feedId, System.currentTimeMillis());
		} finally {
			Log.e(LOG_TAG, "gcFeed");
			mContentHelper.gcFeed(feedId);

			respond(feedId, RESPONSE_STOP);
		}
	}

	private void respond(long feedId, String response) {
		Intent intent = new Intent();
		intent.setAction(RESPONSE_ACTION);
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		intent.putExtra(WeadContract.Feeds._ID, feedId);
		intent.putExtra(RESPONSE, response);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}
}
