package se.weinigel.weader.service;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;

import se.weinigel.weader.client.ContentHelper;
import se.weinigel.weader.contract.WeadContract;
import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.mfavez.android.feedgoal.FeedHandler;
import com.mfavez.android.feedgoal.common.Feed;

public class AddFeedService extends IntentService {
	private static final String LOG_TAG = AddFeedService.class.getSimpleName();

	public static final String EXTRA_FUZZY = "fuzzy";

	public static final String RESPONSE_ACTION = AddFeedService.class.getName()
			+ ".RESPONSE_ACTION";

	public static final String RESPONSE = "state";

	public static final String RESPONSE_START = "start";
	public static final String RESPONSE_ERROR = "error";
	public static final String RESPONSE_EXISTS = "exists";
	public static final String RESPONSE_ADDED = "added";

	private ContentHelper mContentHelper;

	public AddFeedService() {
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
		Uri uri = intent.getData();
		Log.d(LOG_TAG, Helper.getMethodName() + " " + uri);
		if (uri == null) {
			Log.w(LOG_TAG, "handleIntent without an URI");
			return;
		}

		boolean fuzzy = intent.getBooleanExtra(EXTRA_FUZZY, false);

		addFeed(uri, fuzzy);
	}

	private void addFeed(Uri uri, boolean fuzzy) {
		respond(uri, RESPONSE_START);

		try {
			HashSet<String> existing = new HashSet<String>();

			String url = uri.toString();

			if (mContentHelper.hasFeed(url)) {
				respond(uri, RESPONSE_EXISTS);
				return;
			}

			FeedHandler feedHandler = new FeedHandler(this);

			Log.d(LOG_TAG, "fetching new feed from " + url);
			URL url2;
			try {
				url2 = new URL(url);
			} catch (MalformedURLException e) {
				if (!fuzzy)
					throw e;
				url = "http://" + url;
				url2 = new URL(url);
			}

			Feed feed;
			try {
				feed = feedHandler.handleFeed(url2);
			} catch (Exception e) {
				if (!fuzzy)
					throw e;

				url2 = new URL(url + "/feeds/posts/default");
				try {
					feed = feedHandler.handleFeed(url2);
				} catch (Exception e2) {
					if (!fuzzy)
						throw e2;

					url2 = new URL(url + "/feed/");
					feed = feedHandler.handleFeed(url2);
				}
			}

			Log.d(LOG_TAG, "feed title " + feed.getTitle());

			if (existing.contains(feed.getURL().toString())) {
				respond(uri, RESPONSE_EXISTS);
				return;
			}

			mContentHelper.insertFeed(feed);
			respond(uri, RESPONSE_ADDED);
		} catch (Exception e) {
			Log.d(LOG_TAG, "exception", e);
			respond(uri, RESPONSE_ERROR);
		} finally {
		}
	}

	private void respond(Uri uri, String response) {
		Intent intent = new Intent();
		intent.setAction(RESPONSE_ACTION);
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		intent.putExtra(WeadContract.Feed.COLUMN_URL, uri);
		intent.putExtra(RESPONSE, response);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}
}
