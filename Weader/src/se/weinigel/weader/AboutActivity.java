package se.weinigel.weader;

import se.weinigel.weader.R;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class AboutActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);

		final TextView webSiteView = (TextView) findViewById(R.id.website);
		webSiteView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse(webSiteView.getText().toString()));
				startActivity(intent);
			}
		});
	}
}
