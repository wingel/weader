package se.weinigel.weader.service;

import java.net.URL;
import java.util.HashSet;
import java.util.List;

import se.weinigel.weader.contract.WeadContract;
import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.mfavez.android.feedgoal.FeedHandler;
import com.mfavez.android.feedgoal.common.Feed;
import com.mfavez.android.feedgoal.storage.DbFeedAdapter;

public class AddFeedService extends IntentService {
	private static final String LOG_TAG = AddFeedService.class.getSimpleName();

	public static final String RESPONSE_ACTION = AddFeedService.class.getName()
			+ ".RESPONSE_ACTION";

	public static final String RESPONSE = "state";

	public static final String RESPONSE_START = "start";
	public static final String RESPONSE_ERROR = "error";
	public static final String RESPONSE_EXISTS = "exists";
	public static final String RESPONSE_ADDED = "added";

	private DbFeedAdapter mDbFeedAdapter;

	public AddFeedService() {
		super(LOG_TAG);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mDbFeedAdapter = new DbFeedAdapter(this);
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
		addFeed(uri);
	}

	private void addFeed(Uri uri) {
		mDbFeedAdapter.open();

		respond(uri, RESPONSE_START);

		try {
			HashSet<String> existing = new HashSet<String>();

			List<Feed> feeds = mDbFeedAdapter.getFeeds();
			for (Feed feed : feeds)
				existing.add(feed.getURL().toString());

			String url = uri.toString();
			if (existing.contains(url)) {
				respond(uri, RESPONSE_EXISTS);
				return;
			}

			FeedHandler feedHandler = new FeedHandler(this);

			Log.d(LOG_TAG, "fetching new feed from " + url);
			Feed feed = feedHandler.handleFeed(new URL(url));

			Log.d(LOG_TAG, "feed title " + feed.getTitle());

			if (existing.contains(feed.getURL().toString())) {
				respond(uri, RESPONSE_EXISTS);
				return;
			}

			mDbFeedAdapter.addFeed(feed);
			respond(uri, RESPONSE_ADDED);
		} catch (Exception e) {
			Log.d(LOG_TAG, "exception", e);
			respond(uri, RESPONSE_ERROR);
		} finally {
			mDbFeedAdapter.close();
		}
	}

	private void respond(Uri uri, String response) {
		Intent intent = new Intent();
		intent.setAction(RESPONSE_ACTION);
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		intent.putExtra(WeadContract.Feed.COLUMN_URL, uri);
		intent.putExtra(RESPONSE, response);
		sendBroadcast(intent);
	}
}
