package se.weinigel.weader.service;

import java.io.StringWriter;
import java.util.Date;

import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import se.weinigel.feedparser.Article;
import se.weinigel.feedparser.Feed;
import se.weinigel.weader.client.ContentHelper;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.DateFormat;
import android.util.Log;

public class LegacyUpdateService extends IntentService {
	private static class LegacyDbHelper extends SQLiteOpenHelper {
		public static final String DATABASE_NAME = "dbfeed";
		public static final int DATABASE_VERSION = 3;

		public LegacyDbHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		}
	}

	private static final String LOG_TAG = LegacyUpdateService.class
			.getSimpleName();

	public static final String RESPONSE_ACTION = LegacyUpdateService.class
			.getName() + ".RESPONSE_ACTION";

	public static final String RESPONSE = "response";

	public static final String RESPONSE_PROGRESS = "progress";
	public static final String RESPONSE_SUCCESS = "success";
	public static final String RESPONSE_FAILURE = "failure";

	private ContentHelper contentHelper;

	private void respond(String response, String text) {
		Intent intent = new Intent();
		intent.setAction(RESPONSE_ACTION);
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		intent.putExtra(RESPONSE, response);
		if (text != null)
			intent.putExtra("text", text);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}

	public LegacyUpdateService() {
		super(LOG_TAG);
		Log.d(LOG_TAG, "starting");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		try {
			Log.d(LOG_TAG, "Starting database upgrade");

			LegacyDbHelper dbHelper = new LegacyDbHelper(this);
			SQLiteDatabase db = dbHelper.getWritableDatabase();

			this.contentHelper = new ContentHelper(this);

			convert34(db);

			db.close();
			dbHelper.close();

			respond(RESPONSE_SUCCESS, null);
		} catch (Exception e) {
			respond(RESPONSE_FAILURE, null);
			throw new RuntimeException(e);
		}
	}

	/** Convert version 3 to version 4 database */
	private void convert34(final SQLiteDatabase db) {
		System.out.println("Upgrading from version 3 to version 4");
		convert34Feeds(db);
	}

	private void convert34Feeds(SQLiteDatabase db) {
		Cursor cursor = db.query("feeds",
				new String[] { "_id", "title", "url" }, null, null, null, null,
				"title COLLATE LOCALIZED ASC");

		if (cursor.moveToFirst()) {
			int idCol = cursor.getColumnIndex("_id");
			int titleCol = cursor.getColumnIndex("title");
			int urlCol = cursor.getColumnIndex("url");
			do {
				long oldId = cursor.getLong(idCol);

				String title = cursor.getString(titleCol);
				System.out.println("Converting " + title);

				respond(RESPONSE_PROGRESS, title);

				Feed feed = new Feed();
				feed.url = cursor.getString(urlCol);
				feed.refresh = 0;
				feed.title = title;
				feed.lastModified = null;
				feed.etag = null;

				convert34Articles(db, oldId, feed);

				long feedId = contentHelper.getFeedId(feed.url);
				if (feedId != -1)
					contentHelper.updateFeed(feedId, feed);
				else
					contentHelper.insertFeed(feed);
			} while (cursor.moveToNext());
		}
	}

	private void convert34Articles(SQLiteDatabase db, long feedId, Feed feed) {
		Cursor cursor = db.query("items", new String[] { "link", "guid",
				"title", "description", "content", "pubdate", "favorite",
				"read" }, "feed_id=?", new String[] { Long.toString(feedId) },
				null, null, "_id ASC");

		if (cursor.moveToFirst()) {
			XmlSerializer serializer;
			try {
				XmlPullParserFactory factory = XmlPullParserFactory
						.newInstance();
				serializer = factory.newSerializer();
			} catch (XmlPullParserException e) {
				throw new RuntimeException(e);
			}

			int linkCol = cursor.getColumnIndex("link");
			int guidCol = cursor.getColumnIndex("guid");
			int titleCol = cursor.getColumnIndex("title");
			int decriptionCol = cursor.getColumnIndex("description");
			int contentCol = cursor.getColumnIndex("content");
			int pubdateCol = cursor.getColumnIndex("pubdate");
			int favoriteCol = cursor.getColumnIndex("favorite");
			int readCol = cursor.getColumnIndex("read");
			do {
				Article article = new Article();

				article.title = cursor.getString(titleCol);
				long pubDate = cursor.getLong(pubdateCol);
				article.published = DateFormat.format(
						"E, dd MMMM yyyy h:MM:ss z", new Date(pubDate))
						.toString();
				article.updated = null;
				article.guid = cursor.getString(guidCol);
				article.read = cursor.getInt(readCol) == 1;
				article.favorite = cursor.getInt(favoriteCol) == 1;

				String description = cursor.getString(decriptionCol);
				String content = cursor.getString(contentCol);

				article.type = "atom";

				try {
					StringWriter writer = new StringWriter();
					serializer.setOutput(writer);

					serializer
							.processingInstruction("xml version=\"1.0\" encoding=\"utf-8\"");
					serializer.startTag(null, "entry");

					serializer.startTag(null, "title");
					serializer.text(cursor.getString(titleCol));
					serializer.endTag(null, "title");

					serializer.startTag(null, "link");
					serializer.attribute(null, "rel", "alternate");
					serializer.attribute(null, "type", "html");
					serializer.attribute(null, "href",
							cursor.getString(linkCol));
					serializer.endTag(null, "link");

					serializer.startTag(null, "published");
					serializer.text(article.published);
					serializer.endTag(null, "published");

					if (description != null) {
						serializer.startTag(null, "summary");
						serializer.text(description);
						serializer.endTag(null, "summary");

					}
					if (content != null) {
						serializer.startTag(null, "content");
						serializer.text(content);
						serializer.endTag(null, "content");
					}

					serializer.endTag(null, "entry");

					serializer.flush();
					article.raw = writer.toString();
				} catch (Exception e) {
					Log.e(LOG_TAG, "", e);
					continue;
				}

				feed.articles.add(article);
			} while (cursor.moveToNext());
		}
	}
}
