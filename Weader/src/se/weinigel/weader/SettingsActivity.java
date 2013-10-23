package se.weinigel.weader;

import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

public class SettingsActivity extends PreferenceActivity {
	private final String TAG = getClass().getSimpleName();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);
	}

	@Override
	public void onBackPressed() {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);

		boolean pathItemFilter = prefs.getBoolean("prefPathItemFilter", true);
		PackageManager pm = getPackageManager();
		int state = pathItemFilter ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
				: PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
		ComponentName componentName = new ComponentName(this,
				AddFeedActivity.class);
		Log.d(TAG, "component " + componentName + " " + pathItemFilter);
		pm.setComponentEnabledSetting(componentName, state, 0);

		super.onBackPressed();
	}
}