package se.weinigel.weader;

import java.util.HashSet;

import se.weinigel.weader.R;
import se.weinigel.weader.contract.WeadContract;
import se.weinigel.weader.service.UpdateFeedService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

public class ArticleListActivity extends FragmentActivity {
	private final String LOG_TAG = getClass().getSimpleName();

	private ArticleListAdapter mListAdapter;

	private ContentHelper mArticleHelper;

	private long mFeedId;

	private ArticleSelectedReceiver articleSelectedReceiver;

	private HashSet<String> busy = new HashSet<String>();

	private UpdateFeedServiceReceiver updateServiceReceiver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mArticleHelper = new ContentHelper(this);

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		setContentView(R.layout.activity_article_list);

		ListView listView = (ListView) findViewById(R.id.list);

		mListAdapter = new ArticleListAdapter(this);
		listView.setAdapter(mListAdapter);
		updateList();

		Bundle args = getIntent().getExtras();
		mFeedId = args != null ? args.getLong(WeadContract.Feed.COLUMN_ID) : -1;
		if (mFeedId != -1) {
			AsyncTask<Long, Void, Bundle> task = new InfoTask();
			task.execute(mFeedId);
		}

		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View v,
					int position, long id) {
				Intent intent = new Intent(ArticleListActivity.this,
						ArticlePagerActivity.class);
				intent.putExtra(WeadContract.Article.COLUMN_ID, id);
				startActivity(intent);
			}
		});

		IntentFilter articleSelectedFilter = new IntentFilter(
				ArticlePagerActivity.ARTICLE_SELECTED);
		articleSelectedFilter.addCategory(Intent.CATEGORY_DEFAULT);
		articleSelectedReceiver = new ArticleSelectedReceiver();
		registerLocalReceiver(articleSelectedReceiver, articleSelectedFilter);

		IntentFilter updateServiceFilter = new IntentFilter(
				UpdateFeedService.RESPONSE_ACTION);
		updateServiceFilter.addCategory(Intent.CATEGORY_DEFAULT);
		updateServiceReceiver = new UpdateFeedServiceReceiver();
		registerLocalReceiver(updateServiceReceiver, updateServiceFilter);
	}

	@Override
	protected void onDestroy() {
		unregisterLocalReceiver(articleSelectedReceiver);
		unregisterLocalReceiver(updateServiceReceiver);
		super.onDestroy();
	}

	private void updateList() {
		getSupportLoaderManager().restartLoader(0, getIntent().getExtras(),
				mListAdapter);
	}

	@Override
	protected void onResume() {
		super.onResume();
		updateList();
	}

	private final class InfoTask extends AsyncTask<Long, Void, Bundle> {
		@Override
		protected Bundle doInBackground(Long... params) {
			Long feedId = params[0];

			try {
				Cursor cursor;
				cursor = getContentResolver().query(
						WeadContract.Feed.CONTENT_URI,
						new String[] { WeadContract.Feed.COLUMN_TITLE },
						WeadContract.Feed.COLUMN_ID + "=?",
						new String[] { Long.toString(feedId) }, null);
				cursor.moveToFirst();
				if (cursor.isAfterLast()) {
					Log.d(LOG_TAG, "no feed found for mFeedId " + feedId);
					return null;
				}
				String title = cursor.getString(0);
				cursor.close();

				Bundle result = new Bundle();
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
						R.string.title_activity_item_list_feedname,
						result.getString("title"));
				getWindow().setTitle(s);
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.d(LOG_TAG, Helper.getMethodName());
		getMenuInflater().inflate(R.menu.article_list_option, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		Log.d(LOG_TAG, Helper.getMethodName());

		switch (item.getItemId()) {
		case R.id.action_refresh:
			startRefresh();
			return false;
		case R.id.action_mark_all_read:
			markAllRead(true);
			return false;

		case R.id.action_mark_all_unread:
			markAllRead(false);
			return false;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void markAllRead(final boolean b) {
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				mArticleHelper.setFeedRead(mFeedId, b);
				return null;
			}

			@Override
			protected void onPreExecute() {
				busy.add("markAllRead");
				updateUi();
			}

			@Override
			protected void onPostExecute(Void result) {
				busy.remove("markAllRead");
				updateUi();
				updateList();
				int id;
				if (b)
					id = R.string.toast_marked_all_read;
				else
					id = R.string.toast_marked_all_unread;
				Toast toast = Toast.makeText(ArticleListActivity.this, id,
						Toast.LENGTH_SHORT);
				toast.show();
			}
		}.execute((Void) null);
	}

	public class ArticleSelectedReceiver extends BroadcastReceiver {
		@SuppressWarnings("unused")
		@Override
		public void onReceive(Context context, Intent intent) {
			long articleId = intent.getLongExtra(
					WeadContract.Article.COLUMN_ID, -1);

			Log.d(LOG_TAG, "response received " + articleId);

			for (int i = 0; i < mListAdapter.getCount(); i++) {
				if (articleId == mListAdapter.getItemId(i)) {
					ListView listView = (ListView) findViewById(R.id.list);
					if (false)
						listView.setSelection(i);
					else
						listView.smoothScrollToPosition(i);
					break;
				}
			}
		}
	}

	public class UpdateFeedServiceReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			long feedId = intent.getLongExtra(WeadContract.Feed.COLUMN_ID, -1);
			String response = intent.getStringExtra(UpdateFeedService.RESPONSE);

			Log.d(LOG_TAG, "response received " + feedId + " " + response);

			if (UpdateFeedService.RESPONSE_START.equals(response))
				busy.add("update");
			else {
				busy.remove("update");
				updateList();
			}

			updateUi();
		}
	}

	private void startRefresh() {
		Log.d(LOG_TAG, "starting UpdateFeedService " + mFeedId);
		Intent intent = new Intent(this, UpdateFeedService.class);
		intent.putExtra(WeadContract.Feed.COLUMN_ID, mFeedId);
		startService(intent);
	}

	public void updateUi() {
		setProgressBarIndeterminateVisibility(!busy.isEmpty());
	}

	private void registerLocalReceiver(BroadcastReceiver receiver,
			IntentFilter filter) {
		LocalBroadcastManager.getInstance(this).registerReceiver(receiver,
				filter);
	}

	private void unregisterLocalReceiver(BroadcastReceiver receiver) {
		LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
	}
}
