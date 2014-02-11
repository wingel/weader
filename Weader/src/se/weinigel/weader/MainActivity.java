package se.weinigel.weader;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import se.weinigel.weader.FeedListAdapter.FeedListAdapterListener;
import se.weinigel.weader.client.ContentHelper;
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
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends FragmentActivity implements
		FeedListAdapterListener {
	private final String LOG_TAG = getClass().getSimpleName();

	private FeedListAdapter mListAdapter;

	private CharSequence mListSelectedText;

	private int mListSelectedPos;

	private ContentHelper mContentHelper;

	public HashSet<Uri> addFeedBusy = new HashSet<Uri>();

	private UpdateFeedServiceReceiver updateServiceReceiver;

	private AddFeedServiceReceiver addServiceReceiver;

	private HashSet<Long> refreshing = new HashSet<Long>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mContentHelper = new ContentHelper(this);

		Intent intent = getIntent();

		Helper.dumpIntent(intent);

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		setContentView(R.layout.activity_feed_list);

		ListView listView = (ListView) findViewById(R.id.list);

		mListAdapter = new FeedListAdapter(this);
		listView.setAdapter(mListAdapter);
		getSupportLoaderManager().initLoader(0, getIntent().getExtras(),
				mListAdapter);
		mListAdapter.setListener(this);

		registerForContextMenu(listView);
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View v,
					int position, long id) {
				Intent intent = new Intent(MainActivity.this,
						ArticleListActivity.class);
				intent.putExtra(WeadContract.Article.COLUMN_ID, id);
				startActivity(intent);
			}
		});

		Uri uri = intent.getData();
		if (Intent.ACTION_VIEW.equals(intent.getAction()) && uri != null) {
			Log.d(LOG_TAG, "add request " + uri);
			addFeed(uri, false);
		}

		IntentFilter updateServiceFilter = new IntentFilter(
				UpdateFeedService.RESPONSE_ACTION);
		updateServiceFilter.addCategory(Intent.CATEGORY_DEFAULT);
		updateServiceReceiver = new UpdateFeedServiceReceiver();
		registerLocalReceiver(updateServiceReceiver, updateServiceFilter);

		IntentFilter addServiceFilter = new IntentFilter(
				AddFeedService.RESPONSE_ACTION);
		addServiceFilter.addCategory(Intent.CATEGORY_DEFAULT);
		addServiceReceiver = new AddFeedServiceReceiver();
		registerLocalReceiver(addServiceReceiver, addServiceFilter);
	}

	private void addFeed(Uri uri, boolean fuzzy) {
		Intent addIntent = new Intent(MainActivity.this, AddFeedService.class);
		addIntent.setData(uri);
		if (fuzzy)
			addIntent.putExtra(AddFeedService.EXTRA_FUZZY, fuzzy);
		startService(addIntent);
	}

	@Override
	protected void onDestroy() {
		unregisterLocalReceiver(addServiceReceiver);
		unregisterLocalReceiver(updateServiceReceiver);
		super.onDestroy();
	}

	@Override
	protected void onStart() {
		super.onStart();
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
		case R.id.action_add_feed:
			onAddFeed();
			return false;
		case R.id.action_settings:
			onSettings();
			return false;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void onSettings() {
		Intent intent = new Intent(this, SettingsActivity.class);
		startActivity(intent);
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

	private void onAddFeed() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.add_feed_title);
		builder.setMessage(R.string.add_feed_message);

		// Use an EditText view to get user input.
		final EditText input = new EditText(this);
		input.setId(View.NO_ID);
		input.setHint(R.string.add_feed_uri);
		input.setInputType(EditorInfo.TYPE_TEXT_VARIATION_URI);
		builder.setView(input);

		builder.setPositiveButton(android.R.string.ok,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {
						String value = input.getText().toString();
						Log.d(LOG_TAG, "Add feed: " + value);
						addFeed(Uri.parse(value), true);
						return;
					}
				});

		builder.setNegativeButton(android.R.string.cancel,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						return;
					}
				});

		AlertDialog dialog = builder.create();
		dialog.show();
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
				Toast toast = Toast.makeText(MainActivity.this, text,
						Toast.LENGTH_SHORT);
				toast.show();
				updateList();
				updateUi();
			}
		}.execute((Void) null);
	}

	private void startRefresh(long feedId) {
		if (refreshing.contains(feedId))
			return;
		refreshing.add(feedId);
		Log.d(LOG_TAG, "starting UpdateFeedService " + feedId);
		Intent intent = new Intent(this, UpdateFeedService.class);
		intent.putExtra(WeadContract.Feed.COLUMN_ID, feedId);
		startService(intent);
	}

	private void startRefreshAll() {
		Cursor cursor = mListAdapter.getCursor();
		cursor.moveToFirst();
		int idCol = cursor.getColumnIndex(WeadContract.Feed.COLUMN_ID);
		while (!cursor.isAfterLast()) {
			long id = cursor.getLong(idCol);
			startRefresh(id);
			cursor.moveToNext();
		}
	}

	private void startAutoRefresh() {
		Cursor cursor = mListAdapter.getCursor();
		cursor.moveToFirst();
		int idCol = cursor.getColumnIndex(WeadContract.Feed.COLUMN_ID);
		int titleCol = cursor.getColumnIndex(WeadContract.Feed.COLUMN_TITLE);
		int refreshCol = cursor
				.getColumnIndex(WeadContract.Feed.COLUMN_REFRESH);
		long now = new Date().getTime();
		while (!cursor.isAfterLast()) {
			long refresh = cursor.getLong(refreshCol);
			Log.d(LOG_TAG, "refresh " + cursor.getString(titleCol) + " "
					+ refresh + " " + (now - refresh));
			// TODO should have a preference for refresh time
			if (now - refresh > 60 * 60 * 1000) {
				long id = cursor.getLong(idCol);
				startRefresh(id);
			}
			cursor.moveToNext();
		}
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
				refreshing.remove(feedId);
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

				int text = R.string.add_feed_unknown;
				if (AddFeedService.RESPONSE_EXISTS.equals(response))
					text = R.string.add_feed_exists;
				else if (AddFeedService.RESPONSE_ERROR.equals(response))
					text = R.string.add_feed_failed;
				else if (AddFeedService.RESPONSE_ADDED.equals(response))
					text = R.string.add_feed_ok;
				Toast toast = Toast.makeText(MainActivity.this, text,
						Toast.LENGTH_SHORT);
				toast.show();

				updateUi();
				updateList();
			}
		}
	}

	private void registerLocalReceiver(BroadcastReceiver receiver,
			IntentFilter filter) {
		LocalBroadcastManager.getInstance(this).registerReceiver(receiver,
				filter);
	}

	private void unregisterLocalReceiver(BroadcastReceiver receiver) {
		LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
	}

	/**
	 * Start another activity to handle web pages.
	 * 
	 * When a user clicks on a link in an article, the intent for the article
	 * might match Weader's own intent filters so that Weader would be asked to
	 * add the link target as a feed. That is probably not what we want, so try
	 * to filter the list of intents and make sure that we start a real browser
	 * instead. This is ugly and fragile but it seems to work decently.
	 */
	public static void startBrowserActivity(Context context, Intent intent) {
		final String LOG_TAG = "startBrowserActivity";

		Helper.dumpIntent(intent);

		// Start by finding an Activity that will handle normal web pages.
		Intent genericBrowserIntent = new Intent(Intent.ACTION_VIEW,
				Uri.parse("http://example.com/"));
		ResolveInfo browserInfo = context.getPackageManager().resolveActivity(
				genericBrowserIntent, PackageManager.MATCH_DEFAULT_ONLY);
		Log.d(LOG_TAG, "browser  " + browserInfo.activityInfo.packageName);

		// Exclude our the package for Weader
		final String exclude = MainActivity.class.getPackage().getName();

		Intent newIntent = null;
		List<Intent> possibleIntents = new ArrayList<Intent>();

		/*
		 * Find all possible activities that can handle the actual intent.
		 * 
		 * If we find an activity matching the activity for normal web pages,
		 * that should be the default browser, so create a new intent using that
		 * activity and break out of the loop.
		 * 
		 * Otherwise, fill in possibleIntents with an intent for each activity,
		 * excluding ourselves.
		 */
		List<ResolveInfo> possibleInfos = context.getPackageManager()
				.queryIntentActivities(intent,
						PackageManager.MATCH_DEFAULT_ONLY);
		for (ResolveInfo resolveInfo : possibleInfos) {
			String packageName = resolveInfo.activityInfo.packageName;

			if (packageName.equals(exclude)) {
				Log.d(LOG_TAG, "ignoring " + packageName);
				continue;
			}

			if (browserInfo.activityInfo.packageName.equals(packageName)) {
				Log.d(LOG_TAG, "using default " + packageName);

				newIntent = new Intent(intent);
				newIntent.setPackage(packageName);
				break;
			}

			Log.d(LOG_TAG, "including " + packageName);

			Intent targetedIntent = new Intent(intent);
			targetedIntent.setPackage(packageName);

			possibleIntents.add(targetedIntent);
		}

		// No default activity found, start a chooser with all possible intents.
		if (newIntent == null) {
			if (possibleIntents.isEmpty()) {
				Log.d(LOG_TAG, "no other activities to choose from");
				return;
			}

			if (possibleIntents.size() == 1) {
				Log.d(LOG_TAG, "only one activity to choose from");
				context.startActivity(possibleIntents.get(0));
				return;
			}

			newIntent = Intent.createChooser(
					possibleIntents.remove(possibleIntents.size() - 1), null);
			newIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS,
					possibleIntents.toArray(new Parcelable[] {}));
		}

		context.startActivity(newIntent);
	}

	@Override
	public void onLoadFinished() {
		startAutoRefresh();
	}
}
