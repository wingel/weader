package se.weinigel.feedparser;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class FeedParser {
	public static Feed parseFeed(XmlPullParser parser)
			throws XmlPullParserException, IOException {
		assert parser.getEventType() == XmlPullParser.START_TAG;

		String tag = parser.getName();

		Feed feed;

		if ("feed".equals(tag)) {
			feed = AtomFeedParser.parseFeed(parser);
		} else if ("rss".equals(tag)) {
			feed = RSS2FeedParser.parseFeed(parser);
		} else if ("RDF".equals(tag) || "rdf".equals(tag)) {
			feed = RSS1FeedParser.parseFeed(parser);
		} else {
			throw new XmlPullParserException("Unknown root tag \"" + tag
					+ "\"");
		}

		assert parser.getEventType() == XmlPullParser.END_TAG
				&& tag.equals(parser.getName());

		return feed;
	}
}
