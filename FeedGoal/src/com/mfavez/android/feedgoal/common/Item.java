/*
 * Copyright (C) 2010-2011 Mathieu Favez - http://mfavez.com
 *
 *
 * This file is part of FeedGoal.
 * 
 * FeedGoal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FeedGoal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FeedGoal.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mfavez.android.feedgoal.common;

import java.net.URL;
import java.util.Date;

/**
 * A class for creating and managing instances of items.
 * @author Mathieu Favez
 * Created 15/04/2010
 */
public class Item {
	// private static final String LOG_TAG = "Item";
	
	private long mId = -1;
	private URL mLink;
	private String mGuid;
	private String mTitle;
	private String mDescription;
	private String mContent;
	private URL mImage = null;
	private Date mPubdate;
	private boolean mFavorite = false;
	private boolean mRead = false;
	
	public Item() {
		mPubdate = new Date();
	}
	
	public Item(long id, URL link, String guid, String title, String description, String content, URL image, Date pubdate, boolean favorite, boolean read) {
		super();
		this.mId = id;
		this.mLink = link;
		this.mGuid = guid;
		this.mTitle = title;
		this.mDescription = description;
		this.mContent = content;
		this.mImage = image;
		this.mPubdate = pubdate;
		this.mFavorite = favorite;
		this.mRead = read;
	}
	
	public void setId(long id) {
		this.mId = id;
	}
	
	public long getId() {
		return mId;
	}
	
	public void setLink(URL link) {
		this.mLink = link;
	}

	public URL getLink() {
		return this.mLink;
	}
	
	public void setGuid(String guid) {
		this.mGuid = guid;
	}
	
	public String getGuid() {
		return mGuid;
	}
	
	public void setTitle(String title) {
		this.mTitle = title;
	}
	
	public String getTitle() {
		return this.mTitle;
	}
	
	public void setDescription(String description) {
		this.mDescription = description;
	}

	public String getDescription() {
		return mDescription;
	}
	
	public void setContent(String content) {
		this.mContent = content;
	}

	public String getContent() {
		return mContent;
	}
	
	public void setImage(URL image) {
		this.mImage = image;
	}

	public URL getImage() {
		return this.mImage;
	}
	
	public void setPubdate(Date pubdate) {
		this.mPubdate = pubdate;
	}

	public Date getPubdate() {
		return this.mPubdate;
	}
	
	public void favorite() {
		this.mFavorite = true;
	}
	
	public void unfavorite() {
		this.mFavorite = false;
	}

	/*
	public void setFavorites(boolean favorite) {
		if (!favorite)
			this.mFavorite = false;
		else
			this.mFavorite = true;
	}
	*/
	
	public void setFavorite(int state) {
		if (state == 0)
			this.mFavorite = false;
		else
			this.mFavorite = true;
	}
	
	public boolean isFavorite() {
		return this.mFavorite;
	}
	
	public void read() {
		this.mRead = true;
	}
	
	public void unread() {
		this.mRead = false;
	}
	
	/*
	public void setRead(boolean read) {
		if (!read)
			this.mRead = false;
		else
			this.mRead = true;
	}
	*/
	
	public void setRead(int state) {
		if (state == 0)
			this.mRead = false;
		else
			this.mRead = true;
	}
	
	public boolean isRead() {
		return this.mRead;
	}
	
	public String toString() {
		String s =  "{ID=" + this.mId + " link=" + this.mLink.toString() + " GUID=" + this.mGuid + " title=" + this.mTitle + " description=" + this.mDescription + " content=" + this.mContent + " image=" + this.mImage.toString() + " pubdate=" + this.mPubdate.toString() + " favorite=" + this.mFavorite + " read=" + this.mRead + "}";
		return s;
	}
}
