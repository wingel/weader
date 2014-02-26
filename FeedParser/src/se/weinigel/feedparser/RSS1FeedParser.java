package se.weinigel.feedparser;

import java.io.IOException;
import java.io.StringWriter;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class RSS1FeedParser extends RSSParser {
	public static Feed parseFeed(XmlPullParser parser)
			throws XmlPullParserException, IOException {
		Feed feed = null;

		parser.require(XmlPullParser.START_TAG, NS, "RDF");

		while (parser.nextTag() != XmlPullParser.END_TAG) {
			String name = parser.getName();

			if ("channel".equals(name)) {
				if (feed != null) {
					throw new XmlPullParserException(
							"multiple channel tags in rdf feed");
				}
				feed = parseChannel(parser);
			} else if ("item".equals(name)) {
				StringWriter writer = new StringWriter();
				XmlSerializer serializer = createSerializer(writer);
				CopyingXmlPullParserWrapper copyingParser = new CopyingXmlPullParserWrapper(
						parser, serializer);
				Article article = parseEntry(copyingParser);
				serializer.flush();

				article.raw = writer.toString();

				System.out.println("entry raw: " + article.raw.length()
						+ " bytes");
				// System.out.println(entry.raw);
				System.out.println();

				feed.articles.push(article);
			} else {
				skip(parser);
			}
		}

		parser.require(XmlPullParser.END_TAG, NS, "RDF");

		return feed;
	}

	public static Feed parseChannel(XmlPullParser parser)
			throws XmlPullParserException, IOException {
		Feed feed = new Feed();
		feed.type = "rss";

		parser.require(XmlPullParser.START_TAG, NS, "channel");

		while (parser.nextTag() != XmlPullParser.END_TAG) {
			String name = parser.getName();

			if ("title".equals(name)) {
				feed.title = parseTextNormalized(parser);
				System.out.println("feed title: " + feed.title);
			} else if ("managingEditor".equals(name)) {
				String s = parseTextNormalized(parser);
				feed.authors.add(s);
				System.out.println("feed author: " + s);
			} else if ("generator".equals(name)) {
				feed.generator = parseTextNormalized(parser);
				System.out.println("feed generator: " + feed.generator);
			} else {
				skip(parser);
			}
		}

		parser.require(XmlPullParser.END_TAG, NS, "channel");

		return feed;
	}
}
