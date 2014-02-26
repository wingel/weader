package se.weinigel.feedparser;

import java.io.IOException;

public class FeedHttpException extends IOException {
	private static final long serialVersionUID = 5443673964887802582L;

	public final int code;

	public FeedHttpException(int code, String message) {
		super(message + " (" + code + ")");
		this.code = code;
	}
}