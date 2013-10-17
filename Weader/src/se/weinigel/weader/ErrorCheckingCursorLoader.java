package se.weinigel.weader;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.CursorLoader;
import android.util.Log;

final class ErrorCheckingCursorLoader extends CursorLoader {
	private final String LOG_TAG = getClass().getSimpleName();

	ErrorCheckingCursorLoader(Context context, Uri uri, String[] projection,
			String selection, String[] selectionArgs, String sortOrder) {
		super(context, uri, projection, selection, selectionArgs, sortOrder);
	}

	@Override
	public void deliverResult(Cursor cursor) {
		Log.d(LOG_TAG, Helper.getMethodName());
		try {
			super.deliverResult(cursor);
		} catch (Exception e) {
			Log.d(LOG_TAG,
					Helper.getMethodName() + " exception " + e.toString());
			e.printStackTrace();
		}
	}
}