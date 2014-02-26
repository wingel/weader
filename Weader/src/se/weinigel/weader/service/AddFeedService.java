package se.weinigel.weader.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import se.weinigel.feedparser.Feed;
import se.weinigel.feedparser.FeedParser;
import se.weinigel.feedparser.OpmlHelper;
import se.weinigel.feedparser.UrlHelper;
import se.weinigel.feedparser.XmlHelper;
import se.weinigel.weader.client.ContentHelper;
import se.weinigel.weader.contract.WeadContract;
import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

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
			String string = uri.toString();

			if (mContentHelper.hasFeed(string)) {
				Log.d(LOG_TAG, "feed already exists (1)");
				respond(uri, RESPONSE_EXISTS);
				return;
			}

			Log.d(LOG_TAG, "fetching new feed from " + uri);

			URL url = new URL(uri.toString());

			UrlHelper urlHelper = mContentHelper.createUrlHelper();

			System.out.println(url);

			URLConnection conn = urlHelper.connect(url);

			InputStream in = conn.getInputStream();
			XmlPullParser parser = XmlHelper.createParser(in);

			System.out.println("type " + parser.getName());

			if (mContentHelper.hasFeed(conn.getURL().toExternalForm())) {
				Log.d(LOG_TAG, "feed already exists (3)");
				respond(uri, RESPONSE_EXISTS);
				return;
			}

			String tag = parser.getName();
			if ("opml".equals(tag)) {
				List<Feed> feeds = OpmlHelper.parseOpml(parser);
				in.close();

				for (Feed feed : feeds) {
					System.out.println("Fetching " + feed.url);

					if (mContentHelper.hasFeed(feed.url)) {
						Log.d(LOG_TAG, "feed already exists (4)");
						continue;
					}

					try {
						conn = urlHelper.connect(new URL(feed.url));

						if (mContentHelper.hasFeed(conn.getURL()
								.toExternalForm())) {
							Log.d(LOG_TAG, "feed already exists (5)");
							continue;
						}

						in = conn.getInputStream();
						parser = XmlHelper.createParser(in);
						Feed newFeed = fetch(conn, parser);

						if (feed.title != null)
							newFeed.title = feed.title;

						// TODO maybe tell the user if the feed has changed
						// names

						mContentHelper.insertFeed(newFeed);
					} catch (IOException e) {
						Log.d(LOG_TAG, "failed to fetch feed", e);
					} catch (XmlPullParserException e) {
						Log.d(LOG_TAG, "failed to parse feed", e);
					}
				}

				respond(uri, RESPONSE_ADDED);
				return;
			}

			try {
				Feed feed = fetch(conn, parser);
				mContentHelper.insertFeed(feed);
			} catch (IOException e) {
				Log.d(LOG_TAG, "failed to fetch feed", e);
				respond(uri, RESPONSE_ERROR);
				return;
			} catch (XmlPullParserException e) {
				Log.d(LOG_TAG, "failed to parse feed", e);
				respond(uri, RESPONSE_ERROR);
				return;
			}

			respond(uri, RESPONSE_ADDED);
		} catch (Exception e) {
			Log.d(LOG_TAG, "exception", e);
			respond(uri, RESPONSE_ERROR);
		} finally {
		}
	}

	private Feed fetch(URLConnection conn, XmlPullParser parser)
			throws XmlPullParserException, IOException {
		Feed feed = FeedParser.parseFeed(parser);
		feed.url = conn.getURL().toExternalForm();
		feed.lastModified = conn.getHeaderField("Last-Modified");
		feed.etag = conn.getHeaderField("ETag");
		System.out.println("lastModified: " + feed.lastModified);
		System.out.println("etag: " + feed.etag);

		Log.d(LOG_TAG, "feed title " + feed.title);

		feed.refresh = System.currentTimeMillis();

		return feed;
	}

	private void respond(Uri uri, String response) {
		Intent intent = new Intent();
		intent.setAction(RESPONSE_ACTION);
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		intent.putExtra(WeadContract.Feeds._URL, uri);
		intent.putExtra(RESPONSE, response);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}
}
