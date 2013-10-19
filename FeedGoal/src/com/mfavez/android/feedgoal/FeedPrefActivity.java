/*
 * Copyright (C) 2010-2011 Mathieu Favez - http://mfavez.com
 *
 *
 * This file is part of FeedGoal - http://feedgoal.org
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

package com.mfavez.android.feedgoal;

import java.util.Iterator;
import java.util.List;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;

import com.mfavez.android.feedgoal.common.Feed;
import com.mfavez.android.feedgoal.common.TrackerAnalyticsHelper;
import com.mfavez.android.feedgoal.storage.DbFeedAdapter;
import com.mfavez.android.feedgoal.storage.SharedPreferencesHelper;

/**
 * Manages and displays user preferences.
 * @author Mathieu Favez
 * Created 15/04/2010
 */
public class FeedPrefActivity extends PreferenceActivity {
	private static final String LOG_TAG = "FeedPrefActivity";
	
	public static final String PREF_START_CHANNEL_KEY = "startChannel";
	public static final String PREF_ITEM_VIEW_KEY = "itemView";
	public static final String PREF_MAX_ITEMS_KEY = "maxItems";
	public static final String PREF_MAX_HOURS_KEY = "maxHours";
	public static final String PREF_UPDATE_PERIOD_KEY = "updatePeriod";
	public static final String PREF_USAGE_DATA_KEY = "usageData";
	
	public static final String DEFAULT_START_CHANNEL = "1";
	public static final String DEFAULT_ITEM_VIEW = "0"; // 0 => Offline, 1 => Online
	public static final String DEFAULT_MAX_ITEMS_PER_FEED = "20";
	public static final String DEFAULT_MAX_HOURS_PER_FEED = "-1"; // Never
	public static final String DEFAULT_UPDATE_PERIOD = "60"; // 60 minutes = 1 hour
	public static final boolean DEFAULT_USAGE_DATA = false;
	
	public static final String[] PREF_KEYS = {PREF_START_CHANNEL_KEY,PREF_ITEM_VIEW_KEY,PREF_MAX_ITEMS_KEY,PREF_MAX_HOURS_KEY,PREF_UPDATE_PERIOD_KEY, PREF_USAGE_DATA_KEY};
	
	private DbFeedAdapter mDbFeedAdapter;
	
	private class FeedPreferenceChangeListener implements Preference.OnPreferenceChangeListener {
		@Override
	    public boolean onPreferenceChange(Preference preference, Object newValue) {
			String label = "";
			if (newValue instanceof String) {
				label = (String) newValue;
				label = label.replace(" ", "_");
			} else if (newValue instanceof Boolean)
				label = newValue.toString();
			
			TrackerAnalyticsHelper.trackEvent(FeedPrefActivity.this, LOG_TAG, preference.getKey(), label, 1);
	    	
	    	if (preference.isEnabled() && preference.getKey().equals(FeedPrefActivity.PREF_USAGE_DATA_KEY)) {
	    		boolean sendUsageData = ((Boolean) newValue).booleanValue();
	    		if (sendUsageData) {
	    			TrackerAnalyticsHelper.startTracker(FeedPrefActivity.this);
	    		} else
	    			TrackerAnalyticsHelper.stopTracker(FeedPrefActivity.this);
	    	}
	    	
	    	SharedPreferencesHelper.backupManagerCompatWrapperDataChanged(FeedPrefActivity.this);
	    	
	        return true;
	    }
	}
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mDbFeedAdapter = new DbFeedAdapter(this);
        mDbFeedAdapter.open();
        
        TrackerAnalyticsHelper.createTracker(this);
        
        CharSequence title =  getString(R.string.app_name) + " - " + getString(R.string.pref_name);
        setTitle(title);
        
        addPreferencesFromResource(R.xml.preferences);
        
        Preference preference = null;
        for (int i = 0; i < PREF_KEYS.length; i++) {
        	preference = findPreference(PREF_KEYS[i]);
        	if (preference != null)
        		preference.setOnPreferenceChangeListener(new FeedPreferenceChangeListener());
        }
        
        Preference dataUsagePreference = findPreference(PREF_USAGE_DATA_KEY);
        if (!SharedPreferencesHelper.areAnalyticsEnabled(this))
        	dataUsagePreference.setEnabled(false);
        
        ListPreference listPref = (ListPreference) findPreference(PREF_START_CHANNEL_KEY);
        
        if (listPref != null) {
	        List<Feed> feeds = mDbFeedAdapter.getFeeds();
	        Iterator<Feed> feedIterator = feeds.iterator();
	        Feed feed = null;
	        CharSequence[] entries = new CharSequence[feeds.size()];
	        CharSequence[] entryValues = new CharSequence[feeds.size()];
	        int index = 0;
	        while (feedIterator.hasNext()) {
				feed = feedIterator.next();
				entries[index] = feed.getTitle();
				entryValues[index] = Long.toString(feed.getId());
				index++;
			}
        
	        listPref.setEntries(entries);
	        listPref.setEntryValues(entryValues);
        }
    }
	
	@Override
    protected void onStart() {
    	super.onStart();
    	TrackerAnalyticsHelper.startTracker(this);
    }
	
	@Override
    protected void onResume() {
    	super.onResume();
    	TrackerAnalyticsHelper.trackPageView(this,"/preferenceView");
    }
	
	@Override
    protected void onStop() {
    	super.onStop();
    	TrackerAnalyticsHelper.stopTracker(this);
    }
	
	@Override
    protected void onDestroy() {
		super.onDestroy();
    	mDbFeedAdapter.close();
    }
}
