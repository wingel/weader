package se.weinigel.weader;

import java.util.Date;

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
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

final class ArticleListAdapter extends SimpleCursorAdapter implements
		LoaderCallbacks<Cursor> {
	private final String LOG_TAG = getClass().getSimpleName();

	protected static final String[] PROJECTION = new String[] {
			WeadContract.Article.COLUMN_ID, WeadContract.Article.COLUMN_TITLE,
			WeadContract.Article.COLUMN_PUB_DATE,
			WeadContract.Article.COLUMN_READ,
			WeadContract.Article.COLUMN_FAVORITE };

	private int mItemNotselectedText;
	private int mItemSelectedText;
	private CharSequence mDatePattern;

	ArticleListAdapter(Context context) {
		super(context, R.layout.list_item_article, null, new String[] {},
				new int[] {}, 0);

		mContext = context;

		final Resources res = context.getResources();
		mItemSelectedText = res.getColor(R.color.item_selected_text);
		mItemNotselectedText = res.getColor(R.color.item_notselected_text);
		mDatePattern = res.getText(R.string.pubdate_format_pattern);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		String title = cursor.getString(1);
		Date date = new Date(cursor.getLong(2));
		int read = cursor.getInt(3);
		int fav = cursor.getInt(4);

		TextView titleView = (TextView) view.findViewById(R.id.title);
		TextView dateView = (TextView) view.findViewById(R.id.pub_date);

		if (titleView != null) {
			titleView.setText(title);
			if (read != 0) {
				titleView.setTextColor(mItemSelectedText);
				titleView.setTypeface(null, Typeface.NORMAL);
			} else {
				titleView.setTextColor(mItemNotselectedText);
				titleView.setTypeface(null, Typeface.BOLD);
			}
		}

		if (dateView != null) {
			String formattedDate = DateFormat.format(mDatePattern, date)
					.toString();
			dateView.setText(formattedDate);
			dateView.setTextColor(mItemNotselectedText);
			dateView.setTextColor(mItemSelectedText);
		}

		ImageView favView = (ImageView) view.findViewById(R.id.fav);
		if (favView != null) {
			if (fav != 0)
				favView.setImageResource(R.drawable.fav);
			else
				favView.setImageResource(R.drawable.no_fav);
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Log.d(LOG_TAG, Helper.getMethodName());

		long feedId = args != null ? args
				.getLong(WeadContract.Article.COLUMN_ID) : -1;

		String selection = null;
		String selectionArgs[] = null;
		if (feedId != -1) {
			selection = "feed_id=?";
			selectionArgs = new String[] { Long.toString(feedId) };
		}

		return new ErrorCheckingCursorLoader(mContext,
				WeadContract.Article.CONTENT_URI, PROJECTION, selection,
				selectionArgs, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		Log.d(LOG_TAG, Helper.getMethodName());
		swapCursor(data);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		Log.d(LOG_TAG, Helper.getMethodName());
		swapCursor(null);
	}
}