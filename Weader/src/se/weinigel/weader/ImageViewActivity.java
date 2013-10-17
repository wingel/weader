package se.weinigel.weader;

import se.weinigel.weader.R;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;

public class ImageViewActivity extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.image_view);

		Bundle extras = getIntent().getExtras();
		String url = extras != null ? extras.getString("url") : null;
		Log.d("ImageViewActivity", "url=" + url);
		if (url != null) {
			WebView webView = (WebView) findViewById(R.id.imageView);
			if (webView != null) {
				webView.getSettings().setBuiltInZoomControls(true);
				webView.getSettings().setLoadWithOverviewMode(true);
				webView.getSettings().setUseWideViewPort(true);
				webView.loadUrl(url);
			}
		}
	}
}