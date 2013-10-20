package se.weinigel.weader;

import java.util.Arrays;
import java.util.Set;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;

class Helper {
	public static long bundleGetStringLong(Bundle bundle, String key) {
		if (bundle == null)
			return -1;
		String s = bundle.getString(key);
		if (s == null)
			return -1;
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return -1;
		}
	}

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

	public static void dumpIntent(Intent intent) {
		final String LOG_TAG = "dumpIntent";

		if (intent != null) {
			Log.d(LOG_TAG, "action " + intent.getAction());
			Log.d(LOG_TAG, "type " + intent.getType());
			Log.d(LOG_TAG, "data " + intent.getData());
			if (intent.getData() != null)
				Log.d(LOG_TAG, "data scheme " + intent.getData().getScheme());
			Log.d(LOG_TAG, "categories " + intent.getCategories());
			Bundle extras = intent.getExtras();
			if (extras != null) {
				Set<String> keys = extras.keySet();
				for (String key : keys)
					Log.d(LOG_TAG, "extra " + key + " " + extras.get(key) + " "
							+ extras.getClass().getSimpleName());
			}
		} else {
			Log.d(LOG_TAG, "null intent");
		}
	}
}
