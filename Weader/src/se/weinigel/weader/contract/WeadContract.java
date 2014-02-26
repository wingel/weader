package se.weinigel.weader.contract;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

public class WeadContract {
	private static final String PROVIDER_NAME = "se.weinigel.weader.provider";

	public static final String AUTHORITY = PROVIDER_NAME;

	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

	public static interface Feeds extends BaseColumns {
		public static final String BASE_NAME = "feeds";

		public static final Uri CONTENT_URI = Uri.withAppendedPath(
				WeadContract.CONTENT_URI, BASE_NAME);

		public static final String CONTENT_SUBTYPE = "/vnd." + PROVIDER_NAME
				+ "." + BASE_NAME;

		public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
				+ CONTENT_SUBTYPE;

		public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
				+ CONTENT_SUBTYPE;

		public static final String _TITLE = "title";

		public static final String _URL = "url";

		public static final String _UNREAD = "unread";

		public static final String _LAST_MODIFIED = "last_modified";

		public static final String _ETAG = "etag";

		public static final String _REFRESH = "refresh";

		public static final String[] PROJECTION_ALL = { _ID, _TITLE, _URL,
				_UNREAD, _LAST_MODIFIED, _ETAG, _REFRESH };

		public static final String SORT_ORDER_DEFAULT = _TITLE
				+ " COLLATE LOCALIZED ASC";
	}

	public static interface Articles extends BaseColumns {
		public static final String BASE_NAME = "articles";

		public static final Uri CONTENT_URI = Uri.withAppendedPath(
				WeadContract.CONTENT_URI, BASE_NAME);

		public static final String CONTENT_SUBTYPE = "/vnd." + PROVIDER_NAME
				+ "." + BASE_NAME;

		public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
				+ CONTENT_SUBTYPE;

		public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
				+ CONTENT_SUBTYPE;

		public static final String _FEED_ID = "feed_id";

		public static final String _TITLE = "title";

		public static final String _GUID = "guid";

		public static final String _PUBLISHED = "published";

		public static final String _UPDATED = "updated";

		public static final String _READ = "read";

		public static final String _FAVORITE = "favorite";

		public static final String _CONTENT_TYPE = "content_type";

		public static final String _CONTENT = "content";

		public static final String[] PROJECTION_ALL = { _ID, _FEED_ID, _TITLE,
				_GUID, _PUBLISHED, _UPDATED, _READ, _FAVORITE, _CONTENT_TYPE,
				_CONTENT };

		public static final String SORT_ORDER_DEFAULT = _ID + " DESC";

		public static final String TABLE_NAME = "article";
	}
}
