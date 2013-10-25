package se.weinigel.weader;

import java.lang.ref.WeakReference;

import se.weinigel.weader.R;
import se.weinigel.weader.contract.WeadContract;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.text.format.DateFormat;
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

	private ContentHelper mArticleHelper;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Log.d(LOG_TAG, Helper.getMethodName());
		Log.d(LOG_TAG, "arguments " + getArguments());

		mArticleHelper = new ContentHelper(getActivity());

		mArticleId = Helper.bundleGetStringLong(getArguments(),
				WeadContract.Article.COLUMN_ID);

		Log.d(LOG_TAG,
				"onCreateView " + System.identityHashCode(savedInstanceState)
						+ " o=" + getResources().getConfiguration().orientation
						+ " id=" + mArticleId);

		mView = inflater.inflate(R.layout.fragment_article_page, container,
				false);

		Bundle bundle = new Bundle();
		bundle.putLong(WeadContract.Article.COLUMN_ID, mArticleId);
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
			WeadContract.Article.COLUMN_TITLE,
			WeadContract.Article.COLUMN_PUB_DATE,
			WeadContract.Article.COLUMN_READ,
			WeadContract.Article.COLUMN_FAVORITE,
			WeadContract.Article.COLUMN_LINK,
			WeadContract.Article.COLUMN_CONTENT };

	private WeakReference<ArticlePageListener> mListener = new WeakReference<ArticlePageListener>(
			null);

	private String mLink;

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		long articleId = args.getLong(WeadContract.Article.COLUMN_ID);

		Log.d(LOG_TAG, Helper.getMethodName() + " id=" + articleId);
		String selection = WeadContract.Article.COLUMN_ID + "=?";
		String[] selectionArgs = new String[] { Long.toString(articleId) };

		return new ErrorCheckingCursorLoader(getActivity(),
				WeadContract.Article.CONTENT_URI, PROJECTION, selection,
				selectionArgs, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		Log.d(LOG_TAG, Helper.getMethodName() + " id=" + mArticleId);

		if (cursor == null)
			return;

		String title;
		long date;
		mRead = true;
		mFavorite = false;

		String content;
		try {
			cursor.moveToFirst();
			if (cursor.isAfterLast())
				return;

			title = cursor.getString(0);
			date = cursor.getLong(1);
			mFavorite = "1".equals(cursor.getString(3));
			mLink = cursor.getString(4);
			content = cursor.getString(5);
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
					MainActivity.startBrowserActivity(getActivity(), browserIntent);
				}
			});
		}

		TextView dateView = (TextView) mView.findViewById(R.id.pub_date);
		if (dateView != null) {
			Resources res = getActivity().getResources();
			CharSequence datePattern = res
					.getText(R.string.pubdate_format_pattern);

			try {
				String formattedDate = DateFormat.format(datePattern, date)
						.toString();
				dateView.setText(formattedDate);
			} catch (NumberFormatException e) {
				Log.d(LOG_TAG, "Invalid date for article " + title, e);
			}
		}

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

			if (content != null) {
				if (content != null) {
					Log.d(LOG_TAG,
							"webView.loadData " + mLink + " "
									+ content.substring(0, 40));

					// This is a hack which tries to make images smaller than
					// the screen width
					content = "<style type='text/css'>img {max-width: 95%;height:initial;} div,p,span,a {max-width: 100%;}</style>"
							+ content;
					webView.loadDataWithBaseURL(mLink, content, "text/html",
							"utf-8", null);
				}
			}
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		Log.d(LOG_TAG, Helper.getMethodName() + " id=" + mArticleId);
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
				mArticleHelper.setArticleRead(mArticleId, mRead);
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
				mArticleHelper.setArticleFavorite(mArticleId, mFavorite);
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
		bundle.putLong(WeadContract.Article.COLUMN_ID, mArticleId);
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
						WeadContract.Article.CONTENT_URI,
						new String[] { WeadContract.Article.COLUMN_FEED_ID, },
						WeadContract.Article.COLUMN_ID + "=?",
						new String[] { articleId.toString() }, null);
				cursor.moveToFirst();
				if (cursor.isAfterLast()) {
					Log.d(LOG_TAG, "no feed found for article " + articleId);
					return null;
				}
				long feedId = cursor.getLong(0);
				cursor.close();

				cursor = getActivity().getContentResolver().query(
						WeadContract.Feed.CONTENT_URI,
						new String[] { WeadContract.Feed.COLUMN_TITLE },
						WeadContract.Feed.COLUMN_ID + "=?",
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
