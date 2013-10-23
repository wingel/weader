package se.weinigel.weader;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class AddFeedActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = new Intent(getIntent());
		intent.setClass(this, MainActivity.class);
		startActivity(intent);
		finish();
	}
}
