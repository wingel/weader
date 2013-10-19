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

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Context;

import com.flurry.android.FlurryAgent;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.mfavez.android.feedgoal.storage.SharedPreferencesHelper;
import com.mobclix.android.sdk.Mobclix;

/**
 * Helper for managing tracking analytics services.
 * @author Mathieu Favez
 * Created 15/08/2010
 */
public final class TrackerAnalyticsHelper {
	
	private static final String LOG_TAG = "TrackerAnalyticsHelper";
	
	private static GoogleAnalyticsTracker tracker;
	
	public static void createTracker(Context ctx) {
		//Log.d(LOG_TAG, "create_tracker");
    	if (SharedPreferencesHelper.trackGoogleAnalytics(ctx)) {
    		tracker = GoogleAnalyticsTracker.getInstance();
    		//Log.d(LOG_TAG, "create_tracker_GAnalytics");
        }
	}
	
    public static void startTracker(Context ctx) {
    	//Log.d(LOG_TAG, "start_tracker");
    	if (SharedPreferencesHelper.trackGoogleAnalytics(ctx)) {
    		if (tracker == null)
    			tracker = GoogleAnalyticsTracker.getInstance();
	        // Start the tracker in manual dispatch mode...
	        tracker.start(SharedPreferencesHelper.getGoogleAnalyticsProfileId(ctx), ctx);
	        //Log.d(LOG_TAG, "start_tracker_GAnalytics");
        }
    	
    	if (SharedPreferencesHelper.trackFlurryAnalytics(ctx)) {
    		FlurryAgent.onStartSession(ctx, SharedPreferencesHelper.getFlurryAnalyticsApiKey(ctx));
    		//Log.d(LOG_TAG, "start_tracker_Flurry");
    	}
    	
    	if (SharedPreferencesHelper.trackMobclixSession(ctx))
    		Mobclix.onCreateWithApplicationId((Activity)ctx, SharedPreferencesHelper.getUniqueId(ctx));
    	
		trackUUID(ctx, SharedPreferencesHelper.getUniqueId(ctx));
    }
    
    public static void stopTracker(Context ctx) {
    	// Manually start a Google Analytics dispatch, not needed if the tracker was started with a dispatch interval.
		// Depending on where dispatch is called in the code, the following exception occurs:
    	// RuntimeException: Handler{} sending message to a Handler on a dead thread
    	dispatchTracker(ctx);
    	//Log.d(LOG_TAG, "stop_tracker");
    	if (SharedPreferencesHelper.trackGoogleAnalytics(ctx)) {
    		tracker.stop();
    		//Log.d(LOG_TAG, "stop_tracker_GAnalytics");
    	}
    	
    	if (SharedPreferencesHelper.trackFlurryAnalytics(ctx)) {
    		FlurryAgent.onEndSession(ctx);
    		//Log.d(LOG_TAG, "stop_tracker_Flurry");
    	}
    	
    	if (SharedPreferencesHelper.trackMobclixSession(ctx))
    		Mobclix.onStop((Activity)ctx);
    }
    
    public static void dispatchTracker(Context ctx) {
    	//Log.d(LOG_TAG, "dispatch_tracker");
    	if (SharedPreferencesHelper.trackGoogleAnalytics(ctx) && SharedPreferencesHelper.isOnline(ctx)) {
    		tracker.dispatch();
    		//Log.d(LOG_TAG, "dispatch_tracker_GAnalytics");
    	}
    }
    
    public static void trackPageView(Context ctx, String page) {
    	//Log.d(LOG_TAG, "track_pageView"+"----"+page);
    	if (SharedPreferencesHelper.trackGoogleAnalytics(ctx)) {
    		tracker.trackPageView(page);
    		//Log.d(LOG_TAG, "track_pageView_Ganalytics"+"----"+page);
    	}
    	
    	if (SharedPreferencesHelper.trackFlurryAnalytics(ctx)) {
    		FlurryAgent.onPageView();
    		//Log.d(LOG_TAG, "track_pageView_Flurry"+"----"+page);
    	}
    }
    
    public static void trackEvent(Context ctx, String category, String action, String label, int value) {
    	//Log.d(LOG_TAG, "track_event");
    	if (SharedPreferencesHelper.trackGoogleAnalytics(ctx)) {
    		tracker.trackEvent(category, action, label, value);
    		//Log.d(LOG_TAG, "track_event_GAnalytics"+"----"+action+"---"+label);
    	}
    	
    	if (SharedPreferencesHelper.trackFlurryAnalytics(ctx)) {
    		Map<String,String> parameters = new HashMap<String,String>();
    		if (label != null && !label.equals(""))
    			parameters.put("Label", shortenParameter(label));
    		FlurryAgent.onEvent(category+"_"+action,parameters);
    		//Log.d(LOG_TAG, "track_event_Flurry"+"----"+action+"---"+label);
    	}
    }
    
    private static String shortenParameter(String parameter) {
		int maxLength = 255;
    	if (parameter.length() > maxLength)
    		parameter = parameter.substring(0, maxLength-1);
    	return parameter;
    }
    
    private static void trackUUID(Context ctx, String UUID) {
    	if (SharedPreferencesHelper.trackGoogleAnalytics(ctx))
    		tracker.setCustomVar(1, "UUID", UUID, 1);
    	
    	if (SharedPreferencesHelper.trackFlurryAnalytics(ctx))
    		FlurryAgent.setUserId(UUID);
    }
    
    public static void trackError(Context ctx, String errorId, String message, String errorClass) {
    	if (SharedPreferencesHelper.trackGoogleAnalytics(ctx))
    		tracker.trackPageView(message);
    	
    	if (SharedPreferencesHelper.trackFlurryAnalytics(ctx))
    		FlurryAgent.onError(errorId, message, errorClass);
    }
}
