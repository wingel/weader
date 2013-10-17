package se.weinigel.weader;

import java.util.Arrays;

import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Log;
import android.util.SparseArray;
import android.view.ViewGroup;

public class CursorPagerAdapter<F extends Fragment> extends
		FragmentStatePagerAdapter {
	private final String LOG_TAG = getClass().getSimpleName();

	private final Class<F> fragmentClass;
	private final String[] projection;
	private Cursor cursor;
	private int mIdColumn;

	private SparseArray<F> mPageMap = new SparseArray<F>();

	public CursorPagerAdapter(FragmentManager fm, Class<F> fragmentClass,
			String[] projection, Cursor cursor) {
		super(fm);
		this.fragmentClass = fragmentClass;
		this.projection = projection;
		this.cursor = cursor;

		updateIdColumn();
	}

	@Override
	public F getItem(int position) {
		Log.d(LOG_TAG, Helper.getMethodName() + " pos=" + position);
		Bundle args = getItemArgs(position);
		if (args == null)
			return null;

		F frag;
		try {
			frag = fragmentClass.newInstance();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		frag.setArguments(args);
		mPageMap.put(position, frag);
		return frag;
	}

	public F getItemAt(int position) {
		return mPageMap.get(position);
	}

	@Override
	public void destroyItem(ViewGroup container, int position, Object object) {
		mPageMap.remove(Integer.valueOf(position));
		super.destroyItem(container, position, object);
	}

	public Bundle getItemArgs(int position) {
		if (cursor == null) // shouldn't happen
			return null;

		cursor.moveToPosition(position);
		Bundle args = new Bundle();
		for (int i = 0; i < projection.length; ++i) {
			args.putString(projection[i], cursor.getString(i));
		}
		return args;
	}

	public long getItemId(int position) {
		if (cursor == null) // shouldn't happen
			return -1;

		if (mIdColumn == -1)
			return -1;

		cursor.moveToPosition(position);
		return cursor.getLong(mIdColumn);
	}

	@Override
	public int getCount() {
		if (cursor == null)
			return 0;
		else
			return cursor.getCount();
	}

	public void swapCursor(Cursor c) {
		if (cursor == c)
			return;

		this.cursor = c;
		notifyDataSetChanged();

		updateIdColumn();
	}

	private void updateIdColumn() {
		if (cursor == null) {
			mIdColumn = -1;
			return;
		}
		String[] cols = cursor.getColumnNames();
		mIdColumn = Arrays.asList(cols).indexOf(BaseColumns._ID);
	}

	public Cursor getCursor() {
		return cursor;
	}
}
