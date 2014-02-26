package se.weinigel.feedparser;

import java.io.IOException;
import java.io.StringWriter;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class RSS2FeedParser extends RSSParser {
	public static Feed parseFeed(XmlPullParser parser)
			throws XmlPullParserException, IOException {
		Feed feed = new Feed();
		feed.type = "rss";

		parser.require(XmlPullParser.START_TAG, NS, "rss");
		parser.nextTag();
		parser.require(XmlPullParser.START_TAG, NS, "channel");

		while (parser.nextTag() != XmlPullParser.END_TAG) {
			String name = parser.getName();

			if ("item".equals(name)) {
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
			} else if ("title".equals(name)) {
				feed.title = parseTextNormalized(parser);
				System.out.println("feed title: " + feed.title);
			} else if ("managingEditor".equals(name)) {
				String s = parseTextNormalized(parser);
				feed.authors.add(s);
				System.out.println("feed author: " + s);
			} else if ("generator".equals(name)) {
				feed.generator = parseTextNormalized(parser);
				System.out.println("feed generator: " + feed.generator);
			} else if ("generatorAgent".equals(name)) {
				// More extensions
				String res = parser.getAttributeValue(NS, "resource");
				if (res != null) {
					feed.generator = res;
					System.out
							.println("feed generatorAgent: " + feed.generator);
				}
				parser.nextTag();
			} else if ("link".equals(name)) {
				// TODO rss feeds can contain <atom:link> tags
				// (xmlns:atom="http://www.w3.org/2005/Atom")
				String link = parseTextNormalized(parser);
				if (!link.isEmpty()) {
					feed.alternate = link;
					System.out.println("feed link: " + feed.alternate);
				}
			} else {
				skip(parser);
			}
		}

		parser.require(XmlPullParser.END_TAG, NS, "channel");
		parser.nextTag();
		parser.require(XmlPullParser.END_TAG, NS, "rss");

		return feed;
	}
}
