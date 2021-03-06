package se.weinigel.weader;

import java.io.IOException;
import java.io.StringReader;
import java.lang.ref.WeakReference;

import org.xmlpull.v1.XmlPullParser;

import se.weinigel.feedparser.Article;
import se.weinigel.feedparser.AtomFeedParser;
import se.weinigel.feedparser.RSSParser;
import se.weinigel.feedparser.XmlHelper;
import se.weinigel.weader.client.ContentHelper;
import se.weinigel.weader.contract.WeadContract;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.CacheManager;
import android.webkit.CacheManager.CacheResult;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;

public class ArticlePageFragment extends Fragment implements
		LoaderCallbacks<Cursor> {
	private final String LOG_TAG = getClass().getSimpleName();

	private View mView;

	private long mArticleId;

	private boolean mRead;

	private boolean mFavorite;

	private boolean mFinished;

	private ContentHelper mContentHelper;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Log.d(LOG_TAG, Helper.getMethodName());
		Log.d(LOG_TAG, "arguments " + getArguments());

		mContentHelper = new ContentHelper(getActivity());

		mArticleId = Helper.bundleGetStringLong(getArguments(),
				WeadContract.Articles._ID);

		Log.d(LOG_TAG,
				"onCreateView " + System.identityHashCode(savedInstanceState)
						+ " o=" + getResources().getConfiguration().orientation
						+ " guid=" + mArticleId);

		mView = inflater.inflate(R.layout.fragment_article_page, container,
				false);

		Bundle bundle = new Bundle();
		bundle.putLong(WeadContract.Articles._ID, mArticleId);
		getLoaderManager().initLoader(0, bundle, this);

		if (mArticleId != -1) {
			AsyncTask<Long, Void, Bundle> task = new InfoTask();
			task.execute(mArticleId);
		}

		return mView;
	}

	@Override
	public void onAttach(Activity activity) {
		Log.d(LOG_TAG, Helper.getMethodName());
		super.onAttach(activity);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(LOG_TAG, Helper.getMethodName());
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		Log.d(LOG_TAG, Helper.getMethodName());
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onStart() {
		Log.d(LOG_TAG, Helper.getMethodName());
		super.onStart();
	}

	@Override
	public void onResume() {
		Log.d(LOG_TAG, Helper.getMethodName());
		super.onResume();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		Log.d(LOG_TAG, Helper.getMethodName());
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		Log.d(LOG_TAG, Helper.getMethodName());
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void onPause() {
		Log.d(LOG_TAG, Helper.getMethodName());
		super.onPause();
	}

	@Override
	public void onStop() {
		Log.d(LOG_TAG, Helper.getMethodName());
		super.onStop();
	}

	@Override
	public void onDestroyView() {
		Log.d(LOG_TAG, Helper.getMethodName());
		super.onDestroyView();
	}

	@Override
	public void onDestroy() {
		Log.d(LOG_TAG, Helper.getMethodName());
		super.onDestroy();
		// TODO This seems to be necessary to avoid StaleDataException on the
		// cursor. It does feel kind of stupid though since one of the points of
		// loaderManager is to be able to keep data over a configuration change
		getLoaderManager().destroyLoader(0);
	}

	@Override
	public void onDetach() {
		Log.d(LOG_TAG, Helper.getMethodName());
		super.onDetach();
	}

	@Override
	public void setArguments(Bundle args) {
		Log.d(LOG_TAG, Helper.getMethodName());
		super.setArguments(args);
	}

	protected static final String[] PROJECTION = new String[] {
			WeadContract.Articles._TITLE,
			WeadContract.Articles._PUBLISHED,
			WeadContract.Articles._FAVORITE,
			WeadContract.Articles._CONTENT_TYPE,
			WeadContract.Articles._CONTENT };

	private WeakReference<ArticlePageListener> mListener = new WeakReference<ArticlePageListener>(
			null);

	private String mLink;

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		long articleId = args.getLong(WeadContract.Articles._ID);

		Log.d(LOG_TAG, Helper.getMethodName() + " guid=" + articleId);
		String selection = WeadContract.Articles._ID + "=?";
		String[] selectionArgs = new String[] { Long.toString(articleId) };

		return new ErrorCheckingCursorLoader(getActivity(),
				WeadContract.Articles.CONTENT_URI, PROJECTION, selection,
				selectionArgs, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		Log.d(LOG_TAG, Helper.getMethodName() + " id=" + mArticleId);

		if (cursor == null)
			return;

		String title;
		String date;
		mRead = true;
		mFavorite = false;
		String contentType;
		byte[] content;

		try {
			cursor.moveToFirst();
			if (cursor.isAfterLast())
				return;

			title = cursor.getString(0);
			date = cursor.getString(1);
			mFavorite = "1".equals(cursor.getString(2));
			contentType = cursor.getString(3);
			content = cursor.getBlob(4);
		} catch (Exception e) {
			Log.d(LOG_TAG, "cursor exception", e);
			return;
		} finally {
			cursor.close();
		}

		TextView titleView = (TextView) mView.findViewById(R.id.title);
		if (titleView != null && title != null)
			titleView.setText(title);
		if (titleView != null) {
			titleView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (mLink == null)
						return;

					Uri uri = Uri.parse(mLink);
					Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri);
					MainActivity.startBrowserActivity(getActivity(),
							browserIntent);
				}
			});
		}

		TextView dateView = (TextView) mView.findViewById(R.id.pub_date);
		if (dateView != null)
			dateView.setText(date);

		ImageView favView = (ImageView) mView.findViewById(R.id.fav);
		if (favView != null) {
			favView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					setFavorite(!isFavorite());
				}
			});
		}

		updateFavorite();

		WebView webView = (WebView) mView.findViewById(R.id.web);
		if (webView != null) {
			webView.getSettings().setBuiltInZoomControls(false);
			webView.setWebViewClient(new ItemWebViewClient());

			// This tries to stop the WebView from asking for focus and causing
			// scrolling on its own
			webView.setFocusableInTouchMode(false);
			webView.setFocusable(false);

			Article article = null;
			try {
				String s = new String(content, "UTF-8");

				Log.d(LOG_TAG,
						"webView.loadData "
								+ contentType
								+ " "
								+ s.substring(0,
										s.length() >= 100 ? 100 : s.length()));

				XmlPullParser parser = XmlHelper.createParser(new StringReader(
						s));

				if (Article.ATOM.equals(contentType))
					article = AtomFeedParser.parseEntry(parser);
				else if (Article.RSS.equals(contentType))
					article = RSSParser.parseEntry(parser);
				else {
					throw new IOException("unknown value type " + contentType);
				}

				String text = article.content;
				mLink = article.alternate;

				if (content != null) {
					Log.d(LOG_TAG,
							"webView.loadData "
									+ mLink
									+ " "
									+ text.substring(
											0,
											text.length() >= 40 ? 40 : text
													.length()));

					// TODO try to identify plain text

					// This is a hack which tries to make images smaller
					// than the screen width
					text = "<style type='text/css'>img {max-width: 95%;height:initial;} div,p,span,a {max-width: 100%;}</style>"
							+ text;
					webView.loadDataWithBaseURL(mLink, text, "text/html",
							"utf-8", null);
				}

			} catch (Exception e) {
				Log.e(LOG_TAG, "failed to parse content", e);
				return;
			}
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		Log.d(LOG_TAG, Helper.getMethodName() + " guid=" + mArticleId);
	}

	public boolean isRead() {
		return mRead;
	}

	public boolean isFavorite() {
		return mFavorite;
	}

	public void setRead(boolean b) {
		mRead = b;

		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				mContentHelper.updateArticleRead(mArticleId, mRead);
				return null;
			}
		}.execute((Void) null);
	}

	public void setFavorite(boolean b) {
		mFavorite = b;
		updateFavorite();

		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				mContentHelper.updateArticleFavorite(mArticleId, mFavorite);
				return null;
			}
		}.execute((Void) null);
	}

	private void updateFavorite() {
		ImageView favView = (ImageView) mView.findViewById(R.id.fav);
		if (favView != null) {
			if (mFavorite)
				favView.setImageResource(R.drawable.btn_star_big_on);
			else
				favView.setImageResource(R.drawable.btn_star_big_off);
			favView.setVisibility(View.VISIBLE);
		}
	}

	public void refresh() {
		Bundle bundle = new Bundle();
		bundle.putLong(WeadContract.Articles._ID, mArticleId);
		getLoaderManager().restartLoader(0, bundle, this);
	}

	public class ItemWebViewClient extends WebViewClient {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			if (url == null)
				return false;

			Log.d(LOG_TAG, "follow " + url);
			Intent intent;
			// If we can find the target in the cache and it is an image, use
			// our internal ImageViewActivity to show the image instead of
			// asking the system to view the image
			CacheResult res = CacheManager.getCacheFile(url, null);
			if (res != null && res.getMimeType().startsWith("image/")) {
				intent = new Intent(getActivity(), ImageViewActivity.class);
				Log.d(LOG_TAG, "cached " + url);
				intent.putExtra("url", url);
			} else if (!url.contains("flickr.com/")
					&& (url.endsWith(".png") || url.endsWith(".jpg")
							|| url.endsWith(".jpeg") || url.endsWith(".gif"))) {
				Log.d(LOG_TAG, "internal " + url);
				intent = new Intent(getActivity(), ImageViewActivity.class);
				intent.putExtra("url", url);
			} else {
				Log.d(LOG_TAG, "default " + url);
				intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse(url));
			}

			MainActivity.startBrowserActivity(getActivity(), intent);
			return true;
		}

		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			setFinished(false);
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			setFinished(true);
		}
	}

	public interface ArticlePageListener {
		public void onArticlePageLoaded(long id, boolean mFinished);
	}

	public void setArticlePageListener(ArticlePageListener listener) {
		if (listener != null)
			mListener = new WeakReference<ArticlePageListener>(listener);
		else
			mListener.clear();
	}

	public boolean isFinished() {
		return mFinished;
	}

	private void setFinished(boolean mFinished) {
		this.mFinished = mFinished;
		ArticlePageListener listener = mListener.get();
		if (listener != null)
			listener.onArticlePageLoaded(mArticleId, mFinished);
	}

	private final class InfoTask extends AsyncTask<Long, Void, Bundle> {
		@Override
		protected Bundle doInBackground(Long... params) {
			Long articleId = params[0];

			try {
				Cursor cursor;
				cursor = getActivity().getContentResolver().query(
						WeadContract.Articles.CONTENT_URI,
						new String[] { WeadContract.Articles._FEED_ID, },
						WeadContract.Articles._ID + "=?",
						new String[] { articleId.toString() }, null);
				cursor.moveToFirst();
				if (cursor.isAfterLast()) {
					Log.d(LOG_TAG, "no feed found for article " + articleId);
					return null;
				}
				long feedId = cursor.getLong(0);
				cursor.close();

				cursor = getActivity().getContentResolver().query(
						WeadContract.Feeds.CONTENT_URI,
						new String[] { WeadContract.Feeds._TITLE },
						WeadContract.Feeds._ID + "=?",
						new String[] { Long.toString(feedId) }, null);
				cursor.moveToFirst();
				if (cursor.isAfterLast()) {
					Log.d(LOG_TAG, "no feed found for article " + articleId);
					return null;
				}
				String title = cursor.getString(0);
				cursor.close();

				Bundle result = new Bundle();
				result.putLong("feedId", feedId);
				result.putLong("articleId", articleId);
				result.putString("title", title);
				return result;
			} catch (Exception e) {
				Log.d(LOG_TAG, "failed to retrieve feed", e);
				return null;
			}
		}

		@Override
		protected void onPostExecute(Bundle result) {
			if (result != null) {
				String s = result.getString("title");
				TextView feedView = (TextView) mView.findViewById(R.id.feed);
				if (feedView != null && s != null)
					feedView.setText(s);
			}
		}
	}
}
