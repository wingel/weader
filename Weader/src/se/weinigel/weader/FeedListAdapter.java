package se.weinigel.weader;

import java.util.HashSet;

import se.weinigel.weader.R;
import se.weinigel.weader.contract.WeadContract;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

final class FeedListAdapter extends SimpleCursorAdapter implements
		LoaderCallbacks<Cursor> {
	private final String LOG_TAG = getClass().getSimpleName();

	protected static final String[] PROJECTION = new String[] {
			WeadContract.Feed.COLUMN_ID, WeadContract.Feed.COLUMN_TITLE,
			WeadContract.Feed.COLUMN_UNREAD, WeadContract.Feed.COLUMN_REFRESH };

	private int mItemNotselectedText;
	private int mItemSelectedText;

	private HashSet<Long> feedsBusy = new HashSet<Long>();

	public interface FeedListAdapterListener {
		public void onLoadFinished();
	}

	private FeedListAdapterListener listener;

	public void setListener(FeedListAdapterListener listener) {
		this.listener = listener;
	}

	FeedListAdapter(Context context) {
		super(context, R.layout.list_item_feed, null, new String[] {},
				new int[] {}, 0);

		mContext = context;

		final Resources res = context.getResources();
		mItemSelectedText = res.getColor(R.color.item_selected_text);
		mItemNotselectedText = res.getColor(R.color.item_notselected_text);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		long id = cursor.getInt(0);
		String title = cursor.getString(1);
		int unread = cursor.getInt(2);

		TextView titleView = (TextView) view.findViewById(R.id.title);
		if (titleView != null) {
			titleView.setText(title);
			if (unread != 0) {
				titleView.setTextColor(mItemNotselectedText);
				titleView.setTypeface(null, Typeface.BOLD);
			} else {
				titleView.setTextColor(mItemSelectedText);
				titleView.setTypeface(null, Typeface.NORMAL);
			}
		}

		TextView unreadView = (TextView) view.findViewById(R.id.unread);
		if (unreadView != null) {
			if (unread != 0) {
				unreadView.setText(Integer.toString(unread));
				unreadView.setTextColor(mItemNotselectedText);
			} else {
				unreadView.setText("");
			}
		}

		ProgressBar busyView = (ProgressBar) view.findViewById(R.id.busy);
		if (busyView != null) {
			if (feedsBusy.contains(id))
				busyView.setVisibility(View.VISIBLE);
			else
				busyView.setVisibility(View.GONE);
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Log.d(LOG_TAG, Helper.getMethodName());

		return new ErrorCheckingCursorLoader(mContext,
				WeadContract.Feed.CONTENT_URI, PROJECTION, null, null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		Log.d(LOG_TAG, Helper.getMethodName());
		swapCursor(data);
		if (listener != null)
			listener.onLoadFinished();
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		Log.d(LOG_TAG, Helper.getMethodName());
		swapCursor(null);
	}

	public void setFeedBusy(long feedId, boolean b) {
		if (b)
			feedsBusy.add(feedId);
		else
			feedsBusy.remove(feedId);
		notifyDataSetChanged();
	}

	public boolean isBusy() {
		return !feedsBusy.isEmpty();
	}
}