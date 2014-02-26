package se.weinigel.feedparser.examples;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import se.weinigel.feedparser.Feed;
import se.weinigel.feedparser.FeedParser;
import se.weinigel.feedparser.UrlHelper;
import se.weinigel.feedparser.FeedHttpException;
import se.weinigel.feedparser.OpmlHelper;
import se.weinigel.feedparser.XmlHelper;

public class FeedParserTest {
	public static void main(String[] args) throws IOException,
			XmlPullParserException {
		List<Feed> feeds;
		feeds = OpmlHelper.load(new FileReader("testdata/simple.opml"));

		List<Feed> newFeeds = new ArrayList<Feed>();

		for (Feed feed : feeds) {
			try {
				System.out.println("Fetching " + feed.url);
				System.out.flush();

				URL url;
				try {
					url = new URL(feed.url);
				} catch (MalformedURLException e) {
					File file = new File(feed.url);
					if (!file.exists())
						throw e;
					url = file.toURI().toURL();
				}

				UrlHelper urlHelper = new UrlHelper();
				urlHelper.lastModified = feed.lastModified;
				urlHelper.etag = feed.etag;

				URLConnection conn = urlHelper.connect(url);
				String lastModified = conn.getHeaderField("Last-Modified");
				String etag = conn.getHeaderField("ETag");

				System.out.println("lastModified: " + lastModified);
				System.out.println("etag: " + etag);

				InputStream in = conn.getInputStream();
				XmlPullParser parser = XmlHelper.createParser(in);
				Feed newFeed = FeedParser.parseFeed(parser);

				newFeed.url = conn.getURL().toExternalForm();
				newFeed.lastModified = lastModified;
				newFeed.etag = etag;

				if (!newFeed.url.equals(feed.url))
					System.out.println("feed moved " + feed.url + " -> "
							+ newFeed.url);

				// Save memory by forgetting the articles
				newFeed.articles.clear();
				newFeeds.add(newFeed);
				urlHelper.lastModified = newFeed.lastModified;
				urlHelper.etag = newFeed.etag;

				// Try to connect again to see if we get a "Not Modified" back
				urlHelper.connect(new URL(newFeed.url));
			} catch (FeedHttpException e) {
				System.out.println(e);
				break;
			} catch (Exception e) {
				e.printStackTrace();
				break;
			}

			System.out.println();
			System.out
					.println("================================================================");
			System.out.println();
		}

		OpmlHelper.save(new FileWriter("test.opml"), newFeeds);
	}
}
