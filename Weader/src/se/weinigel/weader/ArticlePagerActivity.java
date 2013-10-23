package se.weinigel.weader;

import se.weinigel.weader.R;
import se.weinigel.weader.ArticlePageFragment.ArticlePageListener;
import se.weinigel.weader.contract.WeadContract;
import se.weinigel.weader.service.AddFeedService;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

@SuppressWarnings("unused")
public class ArticlePagerActivity extends FragmentActivity implements
		ArticlePageListener {
	private final String LOG_TAG = getClass().getSimpleName();

	public static final String ARTICLE_SELECTED = ArticlePagerActivity.class
			.getName() + ".ARTICLE_SELECTED";

	private ArticlePagerAdapter mPagerAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		setContentView(R.layout.activity_article_pager);

		ViewPager pagerView = (ViewPager) findViewById(R.id.pager);

		mPagerAdapter = new ArticlePagerAdapter(this);

		pagerView.setAdapter(mPagerAdapter);

		Bundle extras = getIntent().getExtras();
		long articleId = extras != null ? extras
				.getLong(WeadContract.Article.COLUMN_ID) : -1;

		if (articleId != -1) {
			AsyncTask<Long, Void, Bundle> task = new InfoTask();
			task.execute(articleId);
		} else
			getSupportLoaderManager().initLoader(0, null, mPagerAdapter);
	}

	private long getCurrentArticleId() {
		ViewPager pagerView = (ViewPager) findViewById(R.id.pager);
		int position = pagerView.getCurrentItem();
		return mPagerAdapter.getItemId(position);
	}

	private ArticlePageFragment getCurrentFragment() {
		ViewPager pagerView = (ViewPager) findViewById(R.id.pager);
		int position = pagerView.getCurrentItem();
		return mPagerAdapter.getItemAt(position);
	}

	private final class InfoTask extends AsyncTask<Long, Void, Bundle> {
		@Override
		protected Bundle doInBackground(Long... params) {
			Long articleId = params[0];

			try {
				Cursor cursor;
				cursor = getContentResolver().query(
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

				cursor = getContentResolver().query(
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
				String s = getResources().getString(
						R.string.title_activity_pager_feedname,
						result.getString("title"));
				getWindow().setTitle(s);
			}
			getSupportLoaderManager().initLoader(0, result, mPagerAdapter);
		}
	}

	public void updatePage(int idx) {
		ViewPager pagerView = (ViewPager) findViewById(R.id.pager);
		pagerView.setCurrentItem(idx);
		PageChangeListener listener = new PageChangeListener();
		pagerView.setOnPageChangeListener(listener);
		listener.onPageSelected(idx);
	}

	private final class PageChangeListener implements OnPageChangeListener {
		@Override
		public void onPageSelected(int position) {
			ArticlePageFragment frag = mPagerAdapter.getItemAt(position);
			Log.d(LOG_TAG, "pageSelected " + position + " " + frag);
			if (frag != null) {
				frag.setRead(true);
				setProgressBarIndeterminateVisibility(!frag.isFinished());
				frag.setArticlePageListener(ArticlePagerActivity.this);
			}
		}

		@Override
		public void onPageScrolled(int position, float positionOffset,
				int positionOffsetPixels) {
		}

		@Override
		public void onPageScrollStateChanged(int state) {
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.d(LOG_TAG, Helper.getMethodName());
		getMenuInflater().inflate(R.menu.article_pager, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		ArticlePageFragment frag = getCurrentFragment();

		if (frag != null) {
			MenuItem readMenuItem = menu.findItem(R.id.action_mark_read);
			if (frag.isRead()) {
				readMenuItem.setTitle(R.string.mark_unread);
				readMenuItem.setIcon(R.drawable.ic_menu_mark_clear);
			} else {
				readMenuItem.setTitle(R.string.mark_read);
				readMenuItem.setIcon(R.drawable.ic_menu_mark_set);
			}

			MenuItem favMenuItem = menu.findItem(R.id.action_mark_fav);
			if (frag.isFavorite())
				favMenuItem.setTitle(R.string.mark_unfav);
			else
				favMenuItem.setTitle(R.string.mark_fav);
		} else
			Log.w(LOG_TAG, "onPrepareOptionsmenu, frag is null");

		return super.onPrepareOptionsMenu(menu);
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		Log.d(LOG_TAG, Helper.getMethodName());

		ArticlePageFragment frag = getCurrentFragment();

		// Handle item selection
		switch (item.getItemId()) {
		case R.id.action_mark_read:
			frag.setRead(!frag.isRead());
			return false;
		case R.id.action_mark_fav:
			frag.setFavorite(!frag.isFavorite());
			return false;
		case R.id.action_refresh:
			frag.refresh();
			return false;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onArticlePageLoaded(long id, boolean mFinished) {
		long articleId = getCurrentArticleId();
		if (articleId == id)
			setProgressBarIndeterminateVisibility(!mFinished);
	}

	@Override
	protected void onPause() {
		Log.d(LOG_TAG, Helper.getMethodName());

		Intent intent = new Intent();
		intent.setAction(ARTICLE_SELECTED);
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		intent.putExtra(WeadContract.Article.COLUMN_ID, getCurrentArticleId());
		sendBroadcast(intent);

		super.onPause();
	}
}
