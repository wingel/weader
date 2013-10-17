package se.weinigel.weader.provider;

import java.util.Arrays;

import android.database.Cursor;
import android.util.Log;

class Helper {
	static public void dumpCursor(Cursor cursor) {
		final String LOG_TAG = "dumpCursor";
		if (cursor != null) {
			Log.d(LOG_TAG, "cursor " + cursor.getCount());
			for (int i = 0; i < cursor.getColumnCount(); i++)
				Log.d(LOG_TAG, "col " + i + " " + cursor.getColumnName(i));

			cursor.moveToFirst();
			String[] a = new String[cursor.getColumnCount()];
			while (!cursor.isAfterLast()) {
				for (int i = 0; i < a.length; i++)
					a[i] = cursor.getString(i);
				Log.d(LOG_TAG, Arrays.toString(a));
				cursor.moveToNext();
			}

			cursor.moveToFirst();
		}
	}

	public static String getMethodName() {
		return Thread.currentThread().getStackTrace()[3].getMethodName();
	}
}
