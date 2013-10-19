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
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.mfavez.android.feedgoal.storage.DbSchema;

/**
 * A class for creating and managing instances of feeds.
 * @author Mathieu Favez
 * Created 15/04/2010
 */
public class Feed {
	private static final String LOG_TAG = "Feed";
	
	public static final String TYPE_RDF = "rdf";
	public static final String TYPE_RSS = "rss";
	public static final String TYPE_ATOM = "atom";
	
	private long mId = -1;
	private URL mURL;
	private URL mHomePage;
	private String mTitle;
	private String mType;
	private Date mRefresh = null;
	private boolean mEnabled = true;
	private List<Item> mItems;
	
	public Feed() {
		mItems = new ArrayList<Item>();
	}
	
	public Feed(long id, URL url, URL homePage, String title, String type, Date refresh, boolean enabled, List<Item> items) {
		super();
		this.mId = id;
		this.mURL = url;
		this.mHomePage = homePage;
		this.mTitle = title;
		this.mType = type;
		this.mRefresh = refresh;
		this.mEnabled = enabled;
		this.mItems = items;
	}
	
	public void setId(long id) {
		this.mId = id;
	}
	
	public long getId() {
		return mId;
	}
	
	public void setURL(URL url) {
		this.mURL = url;
	}

	public URL getURL() {
		return this.mURL;
	}
	
	public void setHomePage(URL homepage) {
		this.mHomePage = homepage;
	}

	public URL getHomePage() {
		return this.mHomePage;
	}
	
	public void setTitle(String title) {
		this.mTitle = title;
	}
	
	public String getTitle() {
		return this.mTitle;
	}
	
	public void setType(String type) {
		this.mType = type;
	}

	public String getType() {
		return mType;
	}
	
	public void setRefresh(Date refresh) {
		mRefresh = refresh;
	}
	
	public Date getRefresh() {
		return mRefresh;
	}

	public void enable() {
		this.mEnabled = true;
	}
	
	public void disable() {
		this.mEnabled = false;
	}
	
	public void setEnabled(int state) {
		if (state == DbSchema.OFF)
			this.mEnabled = false;
		else
			this.mEnabled = true;
	}
	
	public boolean isEnabled() {
		return this.mEnabled;
	}
	
	public void addItem(Item item) {
		this.mItems.add(item);
	}
	
	public void setItems(List<Item> items){
		this.mItems = items;
	}
	
	public List<Item> getItems() {
		return this.mItems;
	}
	
	public String toString() {
		String s = "{ID=" + this.mId + " URL=" + this.mURL.toString() + " homepage=" + this.mHomePage.toString() + " title=" + this.mTitle + " type=" + this.mType + " update=" + this.mRefresh.toString() + " enabled=" + this.mEnabled;
		s = s + " items={";
		Iterator<Item> iterator = this.mItems.iterator();
		while (iterator.hasNext()) {
			s = s + iterator.next().toString();
		}
		s = s + "}}";
		return s;
	}
}
