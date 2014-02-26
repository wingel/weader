package se.weinigel.weader;

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
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

final class ArticleListAdapter extends SimpleCursorAdapter implements
		LoaderCallbacks<Cursor> {
	private final String LOG_TAG = getClass().getSimpleName();

	protected static final String[] PROJECTION = new String[] {
			WeadContract.Articles._ID, WeadContract.Articles._TITLE,
			WeadContract.Articles._PUBLISHED,
			WeadContract.Articles._READ,
			WeadContract.Articles._FAVORITE };

	private int mItemNotselectedText;
	private int mItemSelectedText;

	public interface ArticleListListener {
		public void onClickFavorite(long l);
	};

	private ArticleListListener listener;

	public void setListener(ArticleListListener listener) {
		this.listener = listener;
	}

	private OnClickListener favClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			listener.onClickFavorite((Long) v.getTag());
		}
	};

	ArticleListAdapter(Context context) {
		super(context, R.layout.list_item_article, null, new String[] {},
				new int[] {}, 0);

		mContext = context;

		final Resources res = context.getResources();
		mItemSelectedText = res.getColor(R.color.item_selected_text);
		mItemNotselectedText = res.getColor(R.color.item_notselected_text);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		long id = cursor.getLong(0);
		String title = cursor.getString(1);
		String date = cursor.getString(2);
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
			dateView.setText(date);
			dateView.setTextColor(mItemNotselectedText);
			dateView.setTextColor(mItemSelectedText);
		}

		ImageView favView = (ImageView) view.findViewById(R.id.fav);
		if (favView != null) {
			if (fav != 0)
				favView.setImageResource(R.drawable.btn_star_big_on);
			else
				favView.setImageResource(R.drawable.btn_star_big_off);

			favView.setOnClickListener(favClickListener);
			favView.setTag(id);
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Log.d(LOG_TAG, Helper.getMethodName());

		long feedId = args != null ? args.getLong(WeadContract.Articles._ID)
				: -1;

		String selection = null;
		String selectionArgs[] = null;
		if (feedId != -1) {
			selection = "feed_id=?";
			selectionArgs = new String[] { Long.toString(feedId) };
		}

		return new ErrorCheckingCursorLoader(mContext,
				WeadContract.Articles.CONTENT_URI, PROJECTION, selection,
				selectionArgs, "_id DESC");
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