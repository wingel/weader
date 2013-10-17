package se.weinigel.weader;

import java.util.HashSet;

import se.weinigel.weader.R;
import se.weinigel.weader.contract.WeadContract;
import se.weinigel.weader.service.AddFeedService;
import se.weinigel.weader.service.UpdateFeedService;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class FeedListActivity extends FragmentActivity {
	private final String LOG_TAG = getClass().getSimpleName();

	private FeedListAdapter mListAdapter;

	private CharSequence mListSelectedText;

	private int mListSelectedPos;

	private ContentHelper mContentHelper;

	public HashSet<Uri> addFeedBusy = new HashSet<Uri>();

	private UpdateFeedServiceReceiver updateServiceReceiver;

	private AddFeedServiceReceiver addServiceReceiver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mContentHelper = new ContentHelper(this);

		Intent intent = getIntent();

		if (intent != null) {
			Log.d(LOG_TAG, "action " + intent.getAction());
			Log.d(LOG_TAG, "type " + intent.getType());
			Log.d(LOG_TAG, "data " + intent.getData());
			if (intent.getData() != null)
				Log.d(LOG_TAG, "data scheme " + intent.getData().getScheme());
			Log.d(LOG_TAG, "categories " + intent.getCategories());
		} else {
			Log.d(LOG_TAG, "null intent");
		}

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		setContentView(R.layout.activity_feed_list);

		ListView listView = (ListView) findViewById(R.id.list);

		mListAdapter = new FeedListAdapter(this);
		listView.setAdapter(mListAdapter);
		getSupportLoaderManager().initLoader(0, getIntent().getExtras(),
				mListAdapter);

		registerForContextMenu(listView);
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View v,
					int position, long id) {
				Intent intent = new Intent(FeedListActivity.this,
						ArticleListActivity.class);
				intent.putExtra(WeadContract.Article.COLUMN_ID, id);
				startActivity(intent);
			}
		});

		if (Intent.ACTION_VIEW.equals(intent.getAction())
				&& intent.getData() != null) {
			Log.d(LOG_TAG, "add request " + intent.getData());
			Intent addIntent = new Intent(FeedListActivity.this,
					AddFeedService.class);
			addIntent.setData(intent.getData());
			startService(addIntent);
		}

		IntentFilter updateServiceFilter = new IntentFilter(
				UpdateFeedService.RESPONSE_ACTION);
		updateServiceFilter.addCategory(Intent.CATEGORY_DEFAULT);
		updateServiceReceiver = new UpdateFeedServiceReceiver();
		registerReceiver(updateServiceReceiver, updateServiceFilter);

		IntentFilter addServiceFilter = new IntentFilter(
				AddFeedService.RESPONSE_ACTION);
		addServiceFilter.addCategory(Intent.CATEGORY_DEFAULT);
		addServiceReceiver = new AddFeedServiceReceiver();
		registerReceiver(addServiceReceiver, addServiceFilter);
	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(addServiceReceiver);
		unregisterReceiver(updateServiceReceiver);
		super.onDestroy();
	}

	protected void updateUi() {
		Log.d(LOG_TAG, "updateUi feedsBusy " + mListAdapter.isBusy() + " add "
				+ addFeedBusy.isEmpty());

		setProgressBarIndeterminateVisibility(mListAdapter.isBusy()
				|| !addFeedBusy.isEmpty());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.d(LOG_TAG, Helper.getMethodName());
		getMenuInflater().inflate(R.menu.feed_list, menu);

		MenuItem aboutItem = menu.findItem(R.id.action_about);
		Intent aboutIntent = new Intent(this, AboutActivity.class);
		aboutItem.setIntent(aboutIntent);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.d(LOG_TAG, Helper.getMethodName());

		// Handle item selection
		switch (item.getItemId()) {
		case R.id.action_refresh:
			startRefreshAll();
			return false;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view,
			ContextMenuInfo menuInfo) {
		Log.d(LOG_TAG, Helper.getMethodName());

		if (view.getId() == R.id.list) {
			ListView listView = (ListView) view;
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
			// menu.setHeaderIcon(R.drawable.icon);
			TextView titleView = (TextView) info.targetView
					.findViewById(R.id.title);
			mListSelectedText = titleView.getText();
			menu.setHeaderTitle(mListSelectedText);
			mListSelectedPos = listView.getPositionForView(info.targetView);
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.feed_list_context, menu);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		Log.d(LOG_TAG, Helper.getMethodName());

		switch (item.getItemId()) {
		case R.id.action_delete: {
			Log.d(LOG_TAG, "Delete " + mListSelectedText);
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(mListSelectedText);
			builder.setTitle(R.string.feed_delete_title);
			builder.setPositiveButton(R.string.yes, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					deleteFeed(mListAdapter.getItemId(mListSelectedPos));
				}
			});
			builder.setNegativeButton(R.string.no, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
				}
			});
			AlertDialog dialog = builder.create();
			dialog.show();
			return false;
		}
		case R.id.action_refresh: {
			Log.d(LOG_TAG, "Refresh " + mListSelectedText);
			startRefresh(mListAdapter.getItemId(mListSelectedPos));
			return false;
		}
		default:
			return super.onContextItemSelected(item);
		}
	}

	protected void deleteFeed(final long feedId) {
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				Log.d(LOG_TAG, "Deleting feed " + feedId);
				mContentHelper.deleteFeed(feedId);
				Log.d(LOG_TAG, "Delete Done");
				return null;
			}

			protected void onPreExecute() {
				updateUi();
			}

			protected void onPostExecute(Void result) {
				CharSequence text = "Feed deleted";
				Toast toast = Toast.makeText(FeedListActivity.this, text,
						Toast.LENGTH_SHORT);
				toast.show();
				updateList();
				updateUi();
			}
		}.execute((Void) null);
	}

	private void startRefresh(long feedId) {
		Log.d(LOG_TAG, "starting UpdateFeedService " + feedId);
		Intent intent = new Intent(this, UpdateFeedService.class);
		intent.putExtra(WeadContract.Feed.COLUMN_ID, feedId);
		startService(intent);
	}

	private void startRefreshAll() {
		for (int i = 0; i < mListAdapter.getCount(); i++)
			startRefresh(mListAdapter.getItemId(i));
	}

	void updateList() {
		getSupportLoaderManager().restartLoader(0, getIntent().getExtras(),
				mListAdapter);
	}

	@Override
	protected void onResume() {
		super.onResume();
		getSupportLoaderManager().restartLoader(0, null, mListAdapter);
	}

	public class UpdateFeedServiceReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			long feedId = intent.getLongExtra(WeadContract.Feed.COLUMN_ID, -1);
			String response = intent.getStringExtra(UpdateFeedService.RESPONSE);

			Log.d(LOG_TAG, "response received " + feedId + " " + response);

			if (UpdateFeedService.RESPONSE_START.equals(response))
				mListAdapter.setFeedBusy(feedId, true);
			else {
				mListAdapter.setFeedBusy(feedId, false);
				updateList();
			}

			updateUi();
		}
	}

	public class AddFeedServiceReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String response = intent.getStringExtra(AddFeedService.RESPONSE);

			Uri uri = intent.getData();
			Log.d(LOG_TAG, "response received " + uri + " " + response);

			if (AddFeedService.RESPONSE_START.equals(response)) {
				addFeedBusy.add(uri);
				updateUi();
			} else {
				addFeedBusy.remove(uri);

				String text = "Unknown response when adding feed";
				if (AddFeedService.RESPONSE_EXISTS.equals(response))
					text = "Feed already exists";
				else if (AddFeedService.RESPONSE_ERROR.equals(response))
					text = "Error while fetching feed";
				else if (AddFeedService.RESPONSE_ADDED.equals(response))
					text = "Feed added";
				Toast toast = Toast.makeText(FeedListActivity.this, text,
						Toast.LENGTH_SHORT);
				toast.show();

				updateUi();
				updateList();
			}
		}
	}
}
