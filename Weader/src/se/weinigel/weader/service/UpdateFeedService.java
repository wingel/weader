package se.weinigel.weader.service;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import se.weinigel.weader.contract.WeadContract;
import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.mfavez.android.feedgoal.FeedHandler;
import com.mfavez.android.feedgoal.common.Feed;
import com.mfavez.android.feedgoal.storage.DbFeedAdapter;

public class UpdateFeedService extends IntentService {
	private static final String LOG_TAG = UpdateFeedService.class
			.getSimpleName();

	public static final String RESPONSE_ACTION = UpdateFeedService.class
			.getName() + ".RESPONSE_ACTION";

	public static final String RESPONSE = "response";

	public static final String RESPONSE_START = "start";
	public static final String RESPONSE_STOP = "stop";

	private DbFeedAdapter mDbFeedAdapter;

	public UpdateFeedService() {
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
		long feedId = intent.getLongExtra(WeadContract.Feed.COLUMN_ID, -1);
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
		mDbFeedAdapter.open();
		Feed feed = mDbFeedAdapter.getFeed(feedId);
		if (feed != null)
			updateFeed(feed);
		mDbFeedAdapter.close();
	}

	private void updateFeed(Feed feed) {
		Log.d(LOG_TAG, "loading " + feed.getTitle());
		long feedId = feed.getId();

		respond(feedId, RESPONSE_START);
		try {
			FeedHandler feedHandler = new FeedHandler(this);
			Feed handledFeed = feedHandler.handleFeed(feed.getURL());
			handledFeed.setId(feedId);
			mDbFeedAdapter.updateFeed(handledFeed);
			mDbFeedAdapter.cleanDbItems(feedId);
		} catch (IOException ioe) {
			Log.e(LOG_TAG, "", ioe);
		} catch (SAXException se) {
			Log.e(LOG_TAG, "", se);
		} catch (ParserConfigurationException pce) {
			Log.e(LOG_TAG, "", pce);
		} finally {
			respond(feedId, RESPONSE_STOP);
		}
	}

	private void respond(long feedId, String response) {
		Intent intent = new Intent();
		intent.setAction(RESPONSE_ACTION);
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		intent.putExtra(WeadContract.Feed.COLUMN_ID, feedId);
		intent.putExtra(RESPONSE, response);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}
}
