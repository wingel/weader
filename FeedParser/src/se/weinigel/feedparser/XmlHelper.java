package se.weinigel.feedparser;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

public class XmlHelper {
	protected final static String NS = null;

	protected static String parseTextNormalized(XmlPullParser parser)
			throws XmlPullParserException, IOException {
		return parseText(parser).trim().replaceAll("\\s+", " ");
	}

	protected static String parseText(XmlPullParser parser)
			throws XmlPullParserException, IOException {
		String tag = parser.getName();
		String result = "";
		if (parser.next() == XmlPullParser.TEXT) {
			result = parser.getText();
			parser.nextTag();
		}
		parser.require(XmlPullParser.END_TAG, NS, tag);
		return result;
	}

	/**
	 * Skips tags the parser isn't interested in. Uses depth to handle nested
	 * tags. i.e., if the next tag after a START_TAG isn't a matching END_TAG,
	 * it keeps going until it finds the matching END_TAG (as indicated by the
	 * value of "depth" being 0).
	 */
	protected static void skip(XmlPullParser parser)
			throws XmlPullParserException, IOException {
		if (parser.getEventType() != XmlPullParser.START_TAG)
			throw new IllegalStateException();

		int depth = 1;
		while (depth != 0) {
			switch (parser.next()) {
			case XmlPullParser.END_TAG:
				depth--;
				break;
			case XmlPullParser.START_TAG:
				depth++;
				break;
			}
		}
	}

	public static XmlPullParser createParser(InputStream in)
			throws XmlPullParserException, IOException {
		XmlPullParser parser = createParser();
		parser.setInput(in, "UTF-8");
		skipProcessingInstructions(parser);
		return parser;
	}

	public static XmlPullParser createParser(Reader reader)
			throws XmlPullParserException, IOException {
		XmlPullParser parser = createParser();
		parser.setInput(reader);
		skipProcessingInstructions(parser);
		return parser;
	}

	private static XmlPullParser createParser() throws XmlPullParserException {
		XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
		XmlPullParser parser = factory.newPullParser();
		parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
		return parser;
	}

	private static void skipProcessingInstructions(XmlPullParser parser)
			throws XmlPullParserException, IOException {
		while (parser.next() == XmlPullParser.PROCESSING_INSTRUCTION)
			;
	}

	public static XmlSerializer createSerializer(Writer writer)
			throws XmlPullParserException, IOException {
		XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
		XmlSerializer serializer = factory.newSerializer();
		serializer.setOutput(writer);
		return serializer;
	}
}
