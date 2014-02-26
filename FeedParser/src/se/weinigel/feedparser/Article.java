package se.weinigel.feedparser;

import java.util.LinkedList;
import java.util.List;

public class Article {
	public static final String ATOM = "atom";

	public static final String RSS = "rss";

	public String title;

	public String published;

	public String updated;

	public boolean favorite;

	public boolean read;

	public String guid;

	public String alternate;

	public List<String> authors = new LinkedList<String>();

	public List<String> categories = new LinkedList<String>();

	public String contentType;
	public String content;

	public String type;

	public String raw;
}
