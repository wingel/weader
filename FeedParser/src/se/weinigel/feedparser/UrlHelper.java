package se.weinigel.feedparser;

import java.io.EOFException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class UrlHelper {
	public String userAgent = "FeedParser";

	public String lastModified = null;
	public String etag = null;

	public URLConnection connect(URL url) throws IOException {
		URLConnection conn;
		int redirects = 0;

		while (true) {
			conn = url.openConnection();

			if (conn instanceof HttpURLConnection) {
				HttpURLConnection httpConn = (HttpURLConnection) conn;

				// Cloudflare rejects any User-Agent starting with "Java", so
				// use something else to get around this
				httpConn.setRequestProperty("User-Agent", userAgent);

				if (lastModified != null)
					httpConn.setRequestProperty("If-Modified-Since",
							lastModified);
				if (etag != null)
					httpConn.setRequestProperty("If-None-Match", etag);

				httpConn.connect();

				int responseCode;
				try {
					responseCode = httpConn.getResponseCode();
				} catch (EOFException e) {
					// It seems that WordPress Mobile Pack (fredrik.cafe.se)
					// will provide gzipped data which ends abruptly when it
					// actually is a 304 Not Modified. So assume that
					// EOFException means 304
					System.out
							.println("EOFException, faking a 304 Not Modified "
									+ e);
					responseCode = 304;
				}
				if (responseCode == 200) {
					break;
				} else if (responseCode == 304) {
					System.out.println("Not modified");
					httpConn.disconnect();
					return null;
				} else if (responseCode == 301 || responseCode == 302) {
					String location = httpConn.getHeaderField("Location");
					httpConn.disconnect();

					URL newUrl = new URL(location);
					System.out.println("Redirect to: " + location);
					if (newUrl.sameFile(url)) {
						throw new FeedHttpException(responseCode,
								"Redirect to the same location");
					} else if (++redirects >= 5) {
						throw new FeedHttpException(responseCode,
								"Too many redirects");
					}

					url = newUrl;
					continue;
				} else {
					String message = httpConn.getResponseMessage();
					httpConn.disconnect();
					throw new FeedHttpException(responseCode, message);
				}
			} else
				break;
		} /* while */

		if (true) {
			System.out.println();
			for (int i = 0; conn.getHeaderField(i) != null; i++) {
				System.out.println(conn.getHeaderFieldKey(i) + ": "
						+ conn.getHeaderField(i));
			}
			System.out.println();
		}

		return conn;
	}
}
