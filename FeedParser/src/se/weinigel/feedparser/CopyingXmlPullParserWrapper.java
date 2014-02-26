package se.weinigel.feedparser;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class CopyingXmlPullParserWrapper implements XmlPullParser {
	private XmlPullParser parser;
	private XmlSerializer serializer;

	public boolean compact = false;

	public CopyingXmlPullParserWrapper(XmlPullParser parser,
			XmlSerializer serializer) throws XmlPullParserException,
			IllegalArgumentException, IllegalStateException, IOException {
		this.parser = parser;
		this.serializer = serializer;
		copy();
	}

	/* Override next and nextTag so that they copy data to the serializer. */

	public int next() throws XmlPullParserException, IOException {
		int event = parser.next();
		copy();
		return event;
	}

	public int nextTag() throws XmlPullParserException, IOException {
		int event = next();
		if (event == XmlPullParser.TEXT && parser.isWhitespace())
			event = next();
		if (event != XmlPullParser.START_TAG && event != XmlPullParser.END_TAG) {
			throw new XmlPullParserException("unexpected type", parser, null);
		}
		return event;
	}

	/**
	 * Copy data from parser to serializer
	 * 
	 * @throws XmlPullParserException
	 * @throws IllegalArgumentException
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	private void copy() throws XmlPullParserException,
			IllegalArgumentException, IllegalStateException, IOException {
		int eventType = parser.getEventType();
		switch (eventType) {
		case XmlPullParser.START_TAG:
			serializer.startTag(parser.getNamespace(), parser.getName());

			for (int i = 0; i < parser.getAttributeCount(); i++) {
				serializer
						.attribute(parser.getAttributeNamespace(i),
								parser.getAttributeName(i),
								parser.getAttributeValue(i));
			}
			break;

		case XmlPullParser.END_TAG:
			serializer.endTag(parser.getNamespace(), parser.getName());
			break;

		case XmlPullParser.IGNORABLE_WHITESPACE:
			if (!compact)
				serializer.ignorableWhitespace(parser.getText());
			break;

		case XmlPullParser.TEXT:
			if (!compact || !parser.isWhitespace())
				serializer.text(parser.getText());
			break;

		case XmlPullParser.ENTITY_REF:
			serializer.entityRef(parser.getName());
			break;

		case XmlPullParser.CDSECT:
			serializer.cdsect(parser.getText());
		}
	}

	/* I ought to implement these but I'm too lazy */

	public String nextText() throws XmlPullParserException, IOException {
		throw new XmlPullParserException("nextText not implemented");
	}

	public int nextToken() throws XmlPullParserException, IOException {
		throw new XmlPullParserException("nextText not implemented");
	}

	/* Pure delegate functions */

	public boolean getFeature(String name) {
		return parser.getFeature(name);
	}

	public void defineEntityReplacementText(String entityName,
			String replacementText) throws XmlPullParserException {
		parser.defineEntityReplacementText(entityName, replacementText);
	}

	public int getNamespaceCount(int depth) throws XmlPullParserException {
		return parser.getNamespaceCount(depth);
	}

	public String getNamespacePrefix(int pos) throws XmlPullParserException {
		return parser.getNamespacePrefix(pos);
	}

	public String getNamespace(String prefix) {
		return parser.getNamespace(prefix);
	}

	public int getDepth() {
		return parser.getDepth();
	}

	public int getLineNumber() {
		return parser.getLineNumber();
	}

	public int getColumnNumber() {
		return parser.getColumnNumber();
	}

	public String getNamespace() {
		return parser.getNamespace();
	}

	public String getName() {
		return parser.getName();
	}

	public int getAttributeCount() {
		return parser.getAttributeCount();
	}

	public String getAttributeNamespace(int index) {
		return parser.getAttributeNamespace(index);
	}

	public String getAttributeName(int index) {
		return parser.getAttributeName(index);
	}

	public String getAttributePrefix(int index) {
		return parser.getAttributePrefix(index);
	}

	public String getAttributeType(int arg0) {
		return parser.getAttributeType(arg0);
	}

	public String getAttributeValue(int index) {
		return parser.getAttributeValue(index);
	}

	public String getAttributeValue(String namespace, String name) {
		return parser.getAttributeValue(namespace, name);
	}

	public int getEventType() throws XmlPullParserException {
		return parser.getEventType();
	}

	public String getInputEncoding() {
		return parser.getInputEncoding();
	}

	public Object getProperty(String name) {
		return parser.getProperty(name);
	}

	public void setInput(Reader in) throws XmlPullParserException {
		parser.setInput(in);
	}

	public String getNamespaceUri(int pos) throws XmlPullParserException {
		return parser.getNamespaceUri(pos);
	}

	public String getPositionDescription() {
		return parser.getPositionDescription();
	}

	public boolean isWhitespace() throws XmlPullParserException {
		return parser.isWhitespace();
	}

	public String getText() {
		return parser.getText();
	}

	public char[] getTextCharacters(int[] holderForStartAndLength) {
		return parser.getTextCharacters(holderForStartAndLength);
	}

	public String getPrefix() {
		return parser.getPrefix();
	}

	public boolean isAttributeDefault(int arg0) {
		return parser.isAttributeDefault(arg0);
	}

	public boolean isEmptyElementTag() throws XmlPullParserException {
		return parser.isEmptyElementTag();
	}

	public void setFeature(String name, boolean state)
			throws XmlPullParserException {
		parser.setFeature(name, state);
	}

	public void setProperty(String name, Object value)
			throws XmlPullParserException {
		parser.setProperty(name, value);
	}

	public void require(int type, String namespace, String name)
			throws XmlPullParserException, IOException {
		parser.require(type, namespace, name);
	}

	public void setInput(InputStream arg0, String arg1)
			throws XmlPullParserException {
		parser.setInput(arg0, arg1);
	}
}
