package se.weinigel.weader.contract;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

public class WeadContract {
	private static final String PROVIDER_NAME = "se.weinigel.weader.provider";

	public static final String AUTHORITY = PROVIDER_NAME;

	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

	public static final class Feed {
		public static final String BASE_NAME = "feed";

		public static final Uri CONTENT_URI = Uri.withAppendedPath(
				WeadContract.CONTENT_URI, BASE_NAME);

		public static final String CONTENT_SUBTYPE = "/vnd." + PROVIDER_NAME
				+ "." + BASE_NAME;

		public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
				+ CONTENT_SUBTYPE;

		public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
				+ CONTENT_SUBTYPE;

		public static final String COLUMN_ID = BaseColumns._ID;

		public static final String COLUMN_FEED_ID = "feed_id";

		public static final String COLUMN_TITLE = "title";

		public static final String COLUMN_URL = "url";

		public static final String COLUMN_UNREAD = "unread";

		public static final String[] PROJECTION_ALL = { COLUMN_ID,
				COLUMN_FEED_ID, COLUMN_TITLE, COLUMN_UNREAD };

		public static final String SORT_ORDER_DEFAULT = COLUMN_TITLE
				+ " COLLATE LOCALIZED ASC";
	}

	public static final class Article {
		public static final String BASE_NAME = "article";

		public static final Uri CONTENT_URI = Uri.withAppendedPath(
				WeadContract.CONTENT_URI, BASE_NAME);

		public static final String CONTENT_SUBTYPE = "/vnd." + PROVIDER_NAME
				+ "." + BASE_NAME;

		public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
				+ CONTENT_SUBTYPE;

		public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
				+ CONTENT_SUBTYPE;

		public static final String COLUMN_ID = BaseColumns._ID;

		public static final String COLUMN_FEED_ID = "feed_id";

		public static final String COLUMN_LINK = "link";

		public static final String COLUMN_TITLE = "title";

		public static final String COLUMN_CONTENT = "content";

		public static final String COLUMN_PUB_DATE = "pubdate";

		public static final String COLUMN_READ = "read";

		public static final String COLUMN_FAVORITE = "favorite";

		public static final String[] PROJECTION_ALL = { COLUMN_ID,
				COLUMN_TITLE, COLUMN_CONTENT, COLUMN_PUB_DATE, COLUMN_READ,
				COLUMN_FAVORITE };

		public static final String SORT_ORDER_DEFAULT = COLUMN_PUB_DATE
				+ " DESC";
	}
}
