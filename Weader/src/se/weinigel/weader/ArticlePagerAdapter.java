package se.weinigel.weader;

import se.weinigel.weader.contract.WeadContract;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.util.Log;

final class ArticlePagerAdapter extends CursorPagerAdapter<ArticlePageFragment>
		implements LoaderCallbacks<Cursor> {
	private final String LOG_TAG = getClass().getSimpleName();

	private long mArticleId;

	private ArticlePagerActivity mContext;

	protected static final String[] PROJECTION = new String[] { WeadContract.Article.COLUMN_ID };

	ArticlePagerAdapter(ArticlePagerActivity context) {
		super(context.getSupportFragmentManager(), ArticlePageFragment.class,
				PROJECTION, null);
		mContext = context;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Log.d(LOG_TAG, Helper.getMethodName());

		long feedId = args != null ? args.getLong("feedId") : -1;
		mArticleId = args != null ? args.getLong("articleId") : -1;

		String selection = null;
		String selectionArgs[] = null;
		if (feedId != -1) {
			selection = "feed_id=?";
			selectionArgs = new String[] { Long.toString(feedId) };
		}

		return new ErrorCheckingCursorLoader(mContext,
				WeadContract.Article.CONTENT_URI, PROJECTION,
				selection, selectionArgs, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		Log.d(LOG_TAG, Helper.getMethodName());

		/*
		 * TODO is there any better way of finding the index for a certain
		 * articleId? this feels really ugly and inefficient
		 */

		int idx = 0;
		if (mArticleId != -1 && data != null) {
			data.moveToFirst();
			while (!data.isAfterLast()) {
				if (data.getLong(0) == mArticleId)
					break;
				idx++;
				data.moveToNext();
			}
			data.moveToFirst();
			if (idx >= data.getCount())
				idx = 0;
		}

		/*
		 * TODO this ain't pretty. swapCursor will make the pager adapter go to
		 * page 0 and then there's a visible page flip when we call updatePage.
		 * There must be a better way of doing this.
		 */
		swapCursor(data);
		mContext.updatePage(idx);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		Log.d(LOG_TAG, Helper.getMethodName());
		swapCursor(null);
	}
}