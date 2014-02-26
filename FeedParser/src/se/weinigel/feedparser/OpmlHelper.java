package se.weinigel.feedparser;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class OpmlHelper extends XmlHelper {
	public static void save(Writer writer, List<Feed> feeds)
			throws XmlPullParserException, IOException {
		XmlSerializer serializer = createSerializer(writer);

		serializer.setFeature(
				"http://xmlpull.org/v1/doc/features.html#indent-output", true);

		serializer
				.processingInstruction("xml version=\"1.0\" encoding=\"utf-8\"");

		String ns = null;
		serializer.startTag(ns, "opml");
		serializer.attribute(ns, "version", "1.0");

		serializer.startTag(ns, "head");

		serializer.startTag(ns, "title");
		serializer.text("Subscriptions");
		serializer.endTag(ns, "title");

		serializer.endTag(ns, "head");

		serializer.startTag(ns, "body");

		for (Feed feed : feeds) {
			serializer.startTag(ns, "outline");
			serializer.attribute(ns, "xmlUrl", feed.url);
			serializer.attribute(ns, "type", feed.type);
			if (feed.title == null)
				feed.title = feed.url;
			serializer.attribute(ns, "text", feed.title);
			serializer.attribute(ns, "title", feed.title);
			if (feed.alternate != null)
				serializer.attribute(ns, "htmlUrl", feed.alternate);
			serializer.endTag(ns, "outline");
		}

		serializer.endTag(ns, "body");
		serializer.endTag(ns, "opml");
		serializer.flush();
	}

	public static List<Feed> load(Reader reader) throws XmlPullParserException,
			IOException {
		XmlPullParser parser = createParser(reader);
		ArrayList<Feed> feeds = parseOpml(parser);
		return feeds;
	}

	public static ArrayList<Feed> parseOpml(XmlPullParser parser)
			throws XmlPullParserException, IOException {
		ArrayList<Feed> feeds = new ArrayList<Feed>();

		parser.require(XmlPullParser.START_TAG, NS, "opml");
		while (parser.nextTag() != XmlPullParser.END_TAG) {
			String tag = parser.getName();

			if ("body".equals(tag)) {
				parseOutlines(parser, feeds);
			} else {
				skip(parser);
			}
		}
		parser.require(XmlPullParser.END_TAG, NS, "opml");
		return feeds;
	}

	private static void parseOutlines(XmlPullParser parser,
			ArrayList<Feed> feeds) throws XmlPullParserException, IOException {
		while (parser.nextTag() != XmlPullParser.END_TAG) {
			String tag = parser.getName();

			if ("outline".equals(tag)) {
				parseOutline(parser, feeds);
			} else {
				skip(parser);
			}
		}
	}

	private static void parseOutline(XmlPullParser parser, ArrayList<Feed> feeds)
			throws XmlPullParserException, IOException {
		parser.require(XmlPullParser.START_TAG, NS, "outline");
		Feed feed = new Feed();
		feed.type = parser.getAttributeValue(NS, "type");
		feed.url = parser.getAttributeValue(NS, "xmlUrl");
		feed.alternate = parser.getAttributeValue(NS, "htmlUrl");
		feed.title = parser.getAttributeValue(NS, "title");
		if (feed.title == null)
			feed.title = parser.getAttributeValue(NS, "text");
		feeds.add(feed);

		while (parser.nextTag() != XmlPullParser.END_TAG) {
			String tag = parser.getName();

			if ("outline".equals(tag)) {
				parseOutline(parser, feeds);
			} else {
				skip(parser);
			}
		}

		parser.require(XmlPullParser.END_TAG, NS, "outline");
	}
}