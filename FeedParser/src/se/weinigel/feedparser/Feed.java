package se.weinigel.feedparser;

import java.util.LinkedList;
import java.util.List;

public class Feed {
	public String url;

	public String type;

	public String alternate;

	public String lastModified;

	public String etag;

	public String title;

	public List<String> authors = new LinkedList<String>();

	public LinkedList<Article> articles = new LinkedList<Article>();

	public String generator;

	public long refresh;
}
