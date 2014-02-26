package se.weinigel.feedparser;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class RSSParser extends XmlHelper {
	public static Article parseEntry(XmlPullParser parser)
			throws XmlPullParserException, IOException {
		Article article = new Article();
		article.type = Article.RSS;

		parser.require(XmlPullParser.START_TAG, NS, "item");

		String description = null;
		String encoded = null;

		while (parser.nextTag() != XmlPullParser.END_TAG) {
			String name = parser.getName();

			if ("title".equals(name)) {
				article.title = parseTextNormalized(parser);
				System.out.println("entry title: " + article.title);
			} else if ("guid".equals(name)) {
				article.guid = parseTextNormalized(parser);
				System.out.println("entry guid: " + article.guid);
			} else if ("description".equals(name)) {
				description = parseTextNormalized(parser);
				System.out.println("entry description: " + description.length()
						+ " bytes");
			} else if ("encoded".equals(name)) {
				// This is a common extension, so use it if it is present
				encoded = parseTextNormalized(parser);
				System.out.println("entry encoded : " + encoded.length()
						+ " bytes");
			} else if ("link".equals(name)) {
				article.alternate = parseTextNormalized(parser);
				System.out.println("entry link: " + article.alternate);
			} else if ("category".equals(name)) {
				String s = parseTextNormalized(parser);
				article.categories.add(s);
				System.out.println("entry category: " + s);
			} else if ("author".equals(name)) {
				String s = parseTextNormalized(parser);
				article.authors.add(s);
				System.out.println("entry author: " + s);
			} else if ("creator".equals(name)) {
				// Comon extension
				String s = parseTextNormalized(parser);
				article.authors.add(s);
				System.out.println("entry creator: " + s);
			} else if ("pubDate".equals(name)) {
				article.published = parseTextNormalized(parser);
				System.out.println("entry pubDate: " + article.published);
			} else if ("date".equals(name)) {
				// Date is a common extension, so use it if it is present
				article.published = parseTextNormalized(parser);
				System.out.println("entry date: " + article.published);
			} else {
				skip(parser);
			}
		}

		if (encoded != null)
			description = encoded;

		if (description != null) {
			// TODO detect if content is html or plain text
			article.contentType = "html";
			article.content = description.trim();
		}

		if (article.guid == null)
			article.guid = article.alternate;

		if (article.alternate == null && article.guid != null) {
			try {
				article.alternate = new URL(article.guid).toExternalForm();
			} catch (MalformedURLException e) {
			}
		}

		// TODO parse dates

		parser.require(XmlPullParser.END_TAG, NS, "item");

		return article;
	}
}
