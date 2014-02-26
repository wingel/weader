package se.weinigel.feedparser;

import java.io.IOException;
import java.io.StringWriter;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class AtomFeedParser extends XmlHelper {
	protected static class TextConstruct {
		String type;
		String text;
	}

	protected static class Link {
		public String type;
		public String rel;
		public String href;
	}

	public static Feed parseFeed(XmlPullParser parser)
			throws XmlPullParserException, IOException {
		Feed feed = new Feed();
		feed.type = "atom";

		parser.require(XmlPullParser.START_TAG, NS, "feed");

		while (parser.nextTag() != XmlPullParser.END_TAG) {
			String name = parser.getName();

			if ("entry".equals(name)) {
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

				feed.articles.add(article);
			} else if ("title".equals(name)) {
				feed.title = parseTextNormalized(parser);
				System.out.println("feed title: " + feed.title);
			} else if ("author".equals(name)) {
				String s = parsePersonConstruct(parser, "author");
				if (s != null) {
					feed.authors.add(s);
					System.out.println("feed author: " + s);
				}
			} else if ("generator".equals(name)) {
				feed.generator = parseTextNormalized(parser);
				System.out.println("feed generator: " + feed.generator);
			} else if ("link".equals(name)) {
				Link link = parseLink(parser, "link");
				if ("alternate".equals(link.rel)) {
					feed.alternate = link.href;
					System.out.println("feed alternate: " + feed.alternate);
				}
			} else {
				skip(parser);
			}
		}

		parser.require(XmlPullParser.END_TAG, NS, "feed");

		return feed;
	}

	public static Article parseEntry(XmlPullParser parser)
			throws XmlPullParserException, IOException {
		Article article = new Article();
		article.type = Article.ATOM;

		TextConstruct summary = null;
		TextConstruct content = null;

		parser.require(XmlPullParser.START_TAG, NS, "entry");

		while (parser.nextTag() != XmlPullParser.END_TAG) {
			String name = parser.getName();

			if ("title".equals(name)) {
				article.title = parseTextNormalized(parser);
				System.out.println("entry title: " + article.title);
			} else if ("id".equals(name)) {
				article.guid = parseTextNormalized(parser);
				System.out.println("entry guid: " + article.guid);
			} else if ("link".equals(name)) {
				Link link = parseLink(parser, "link");
				if ("alternate".equals(link.rel)) {
					article.alternate = link.href;
					System.out.println("entry alternate: " + article.alternate);
				}
			} else if ("category".equals(name)) {
				String s = parseCategory(parser, "category");
				if (s != null) {
					article.categories.add(s);
					System.out.println("entry category: " + s);
				}
			} else if ("author".equals(name)) {
				String s = parsePersonConstruct(parser, "author");
				if (s != null) {
					article.authors.add(s);
					System.out.println("entry author: " + s);
				}
			} else if ("published".equals(name)) {
				article.published = parseTextNormalized(parser);
				System.out.println("entry published: " + article.published);
			} else if ("updated".equals(name)) {
				article.updated = parseTextNormalized(parser);
				System.out.println("entry updated: " + article.updated);
			} else if ("summary".equals(name)) {
				summary = parseTextConstruct(parser);
				System.out.println("entry summary: " + summary.type + " "
						+ summary.text.length() + " bytes");
			} else if ("content".equals(name)) {
				content = parseTextConstruct(parser);
				System.out.println("entry content: " + content.type + " "
						+ content.text.length() + " bytes");
				// System.out.println(content.text);
			} else {
				skip(parser);
			}
		}

		if (content == null)
			content = summary;

		if (content != null) {
			article.contentType = content.type;
			article.content = content.text.trim();
		}

		// TODO parse dates

		parser.require(XmlPullParser.END_TAG, NS, "entry");

		return article;
	}

	private static TextConstruct parseTextConstruct(XmlPullParser parser)
			throws XmlPullParserException, IOException {
		TextConstruct tc = new TextConstruct();

		String type = parser.getAttributeValue(NS, "type");
		if ("xhtml".equals(type) || "application/xhtml+xml".equals(type))
			tc.type = "xhtml";
		else if ("html".equals(type) || "text/html".equals(type))
			tc.type = "html";
		else if (type == null || "text".equals(type)
				|| "text/plain".equals(type))
			tc.type = "text";
		else
			tc.type = type;

		String tag = parser.getName();

		System.out.println("parsing: " + parser.getEventType() + " "
				+ parser.getName());

		// This is ugly, we should be able to trust the type field and use
		// parseText if it is text or HTML and process it as XML if it is XHTML.
		// But some pages lie and insert unquoted XHTML while saying it is HTML,
		// so try to autodetect the format instead.
		StringWriter writer = new StringWriter();

		XmlSerializer serializer = createSerializer(writer);

		int event;
		while ((event = parser.next()) != XmlPullParser.END_TAG) {
			if (event == XmlPullParser.IGNORABLE_WHITESPACE) {
				String s = parser.getText().trim();
				writer.write(s);
				System.out.println("whitespace" + s.length());
			} else if (event == XmlPullParser.TEXT) {
				String s = parser.getText().trim();
				writer.write(s);
				System.out.println("text " + s.length());
			} else if (event == XmlPullParser.START_TAG) {
				System.out.println("tag: " + parser.getName());
				CopyingXmlPullParserWrapper copyingParser = new CopyingXmlPullParserWrapper(
						parser, serializer);
				skip(copyingParser);
				parser.next();
				serializer.flush();
			} else
				System.out.println("unknown: " + parser.getEventType() + " "
						+ parser.getName());
		}

		System.out.println("parsing done: " + parser.getName());

		parser.require(XmlPullParser.END_TAG, NS, tag);

		tc.text = writer.toString();

		return tc;
	}

	private static String parseCategory(XmlPullParser parser, String tag)
			throws XmlPullParserException, IOException {
		parser.require(XmlPullParser.START_TAG, NS, tag);
		String category = parser.getAttributeValue(NS, "term");
		parser.next();
		parser.require(XmlPullParser.END_TAG, NS, tag);
		return category;
	}

	private static Link parseLink(XmlPullParser parser, String tag)
			throws XmlPullParserException, IOException {
		Link link = new Link();
		parser.require(XmlPullParser.START_TAG, NS, tag);
		link.type = parser.getAttributeValue(NS, "type");
		link.rel = parser.getAttributeValue(NS, "rel");
		link.href = parser.getAttributeValue(NS, "href");
		parser.next();
		parser.require(XmlPullParser.END_TAG, NS, tag);
		return link;
	}

	private static String parsePersonConstruct(XmlPullParser parser, String tag)
			throws XmlPullParserException, IOException {
		parser.require(XmlPullParser.START_TAG, NS, tag);

		String s = null;

		while (parser.nextTag() != XmlPullParser.END_TAG) {
			String name = parser.getName();

			if (name.equals("name"))
				s = parseTextNormalized(parser);
			else
				skip(parser);
		}
		parser.require(XmlPullParser.END_TAG, NS, tag);
		return s;
	}
}
