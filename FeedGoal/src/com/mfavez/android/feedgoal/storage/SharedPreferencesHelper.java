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

package com.mfavez.android.feedgoal.storage;

import java.util.UUID;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

import com.mfavez.android.feedgoal.FeedPrefActivity;

/**
 * Provides helpful getter/setter methods to wrap shared preferences (application prefs + user prefs + manifest properties).
 * @author Mathieu Favez
 * Created 15/04/2010
 */
public final class SharedPreferencesHelper {
	
	private static final String LOG_TAG = "SharedPreferencesHelper";
	private static long errorId = 0;
	
	// Dialogs Id
	public static final int DIALOG_ABOUT = 0;
	public static final int DIALOG_NO_CONNECTION = 1;
	public static final int DIALOG_UPDATE_PROGRESS = 2;
	public static final int DIALOG_REMOVE_CHANNEL = 3;
	public static final int DIALOG_MIN_CHANNEL_REQUIRED = 4;
	public static final int DIALOG_ADD_CHANNEL = 5;
	public static final int DIALOG_ADD_CHANNEL_ERROR_URL = 6;
	public static final int DIALOG_ADD_CHANNEL_ERROR_PARSING = 7;
	public static final int DIALOG_STARTUP = 8;
	
	// Menu Groups Id
	public static final int CHANNEL_SUBMENU_GROUP = 0;
	
	// App Preferences
	protected static final String PREFS_FILE_NAME = "AppPreferences";
	private static final String PREF_UNIQUE_ID = "uuid";
	private static final String PREF_TAB_FEED_KEY = "tabFeed";
	private static final String PREF_STARTUP_DIALOG_ON_INSTALL_KEY = "startupDialogOnInstall";
	private static final String PREF_STARTUP_DIALOG_ON_UPDATE_KEY = "startupDialogOnUpdate";
	private static final String PREF_VERSION_CODE_KEY = "version";
	
	public static final int DEFAULT_SPLASH_SCREEN_DURATION = 2000;
	public static final boolean DEFAULT_DYNAMIC_MODE = false;
	public static final boolean DEFAULT_SHOW_UPDATE_DIALOG = false;
	
	// Min content length to display item view
	public static final int MIN_CONTENT_LENGTH = 80;
	
	// Admob class info
	private static final String ADMOB_CLASS = "com.google.ads.AdActivity";
	
	// Backup Manager Wrapper
	private static BackupManagerCompatWrapper mBckManager;
	private static boolean mBackupManagerClassAvailable;
	
	// Backward Compatibility: establish whether the BackupManager class is available or not
    static {
       try {
    	   BackupManagerCompatWrapper.checkAvailable();
    	   mBackupManagerClassAvailable = true;
       } catch (Throwable t) {
    	   mBackupManagerClassAvailable = false;
       }
    }
	
	public static void backupManagerCompatWrapperDataChanged(Context ctx) {
		if (mBackupManagerClassAvailable) {
			if (mBckManager == null)
				mBckManager = new BackupManagerCompatWrapper(ctx);
			mBckManager.dataChanged();
		}
	}
	
	// Getter/Setter for SharedPreferences
	
	public static String getUniqueId(Context ctx) {
		return ctx.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE).getString(PREF_UNIQUE_ID,null);
	}
	
	// Set installations unique identifier
	public static void setUniqueId(Context ctx) {
		String uniqueID = UUID.randomUUID().toString();
		SharedPreferences prefs = ctx.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE);
    	Editor editor = prefs.edit();
    	editor.putString(PREF_UNIQUE_ID, uniqueID);
    	//editor.commit();
    	SharedPreferencesCompat.apply(editor); //Strict Mode Performance since 2.3 API 9
    	
    	backupManagerCompatWrapperDataChanged(ctx); //Backup Manager available since 2.2 API 8
	}
	
    public static long getPrefTabFeedId(Context ctx, long defaultTabFeedId) {
    	return ctx.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE).getLong(PREF_TAB_FEED_KEY, defaultTabFeedId);
    }
    
    public static void setPrefTabFeedId(Context ctx, long feedId) {
    	SharedPreferences prefs = ctx.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE);
    	Editor editor = prefs.edit();
    	editor.putLong(PREF_TAB_FEED_KEY, feedId);
    	//editor.commit();
    	SharedPreferencesCompat.apply(editor); //Strict Mode Performance since 2.3 API 9
    	
    	backupManagerCompatWrapperDataChanged(ctx); //Backup Manager available since 2.2 API 8
    }
    
    public static boolean getPrefStartupDialogOnInstall(Context ctx, boolean defaultShowDialog) {
    	return ctx.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE).getBoolean(PREF_STARTUP_DIALOG_ON_INSTALL_KEY, defaultShowDialog);
    }
    
    public static void setPrefStartupDialogOnInstall(Context ctx, boolean showDialog) {
    	SharedPreferences prefs = ctx.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE);
    	Editor editor = prefs.edit();
    	editor.putBoolean(PREF_STARTUP_DIALOG_ON_INSTALL_KEY, showDialog);
    	//editor.commit();
    	SharedPreferencesCompat.apply(editor); //Strict Mode Performance since 2.3 API 9
    	
    	backupManagerCompatWrapperDataChanged(ctx); //Backup Manager available since 2.2 API 8
    }
    
    public static boolean getPrefStartupDialogOnUpdate(Context ctx, boolean defaultShowDialog) {
    	return ctx.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE).getBoolean(PREF_STARTUP_DIALOG_ON_UPDATE_KEY, defaultShowDialog);
    }
    
    public static void setPrefStartupDialogOnUpdate(Context ctx, boolean showDialog) {
    	SharedPreferences prefs = ctx.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE);
    	Editor editor = prefs.edit();
    	editor.putBoolean(PREF_STARTUP_DIALOG_ON_UPDATE_KEY, showDialog);
    	//editor.commit();
    	SharedPreferencesCompat.apply(editor); //Strict Mode Performance since 2.3 API 9
    	
    	backupManagerCompatWrapperDataChanged(ctx); //Backup Manager available since 2.2 API 8
    }
    
    public static int getPrefVersionCode(Context ctx, int defaultVersionCode) {
    	return ctx.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE).getInt(PREF_VERSION_CODE_KEY, defaultVersionCode);
    }
    
    public static void setPrefVersionCode(Context ctx, int versionCode) {
    	SharedPreferences prefs = ctx.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE);
    	Editor editor = prefs.edit();
    	editor.putInt(PREF_VERSION_CODE_KEY, versionCode);
    	//editor.commit();
    	SharedPreferencesCompat.apply(editor); //Strict Mode Performance since 2.3 API 9
    	
    	backupManagerCompatWrapperDataChanged(ctx); //Backup Manager available since 2.2 API 8
    }
    
    // Getters for Application attributes and preferences configured in Android Manifest
    
    public static int getSplashDuration(Context ctx) {
    	int splashScreenDuration = DEFAULT_SPLASH_SCREEN_DURATION;
    	try {
	    	ApplicationInfo ai = ctx.getPackageManager().getApplicationInfo(ctx.getPackageName(), PackageManager.GET_META_DATA);
	    	splashScreenDuration = ai.metaData.getInt("splash_screen_duration");
	    } catch (NameNotFoundException nnfe) {
			Log.e(LOG_TAG,"",nnfe);
			errorId = errorId + 1;
		}
    	return splashScreenDuration;
    }
    
    public static boolean isDynamicMode(Context ctx) {
    	boolean dynamicMode = DEFAULT_DYNAMIC_MODE;
    	try {
	    	ApplicationInfo ai = ctx.getPackageManager().getApplicationInfo(ctx.getPackageName(), PackageManager.GET_META_DATA);
	    	dynamicMode = ai.metaData.getBoolean("dynamic_mode");
	    } catch (NameNotFoundException nnfe) {
			Log.e(LOG_TAG,"",nnfe);
			errorId = errorId + 1;
		}
    	return dynamicMode;
    }
    
    public static boolean showPrefStartupDialogOnUpdate(Context ctx) {
    	boolean showUpdateDialog = DEFAULT_SHOW_UPDATE_DIALOG;
    	try {
	    	ApplicationInfo ai = ctx.getPackageManager().getApplicationInfo(ctx.getPackageName(), PackageManager.GET_META_DATA);
	    	showUpdateDialog = ai.metaData.getBoolean("show_update_startup_dialog");
	    } catch (NameNotFoundException nnfe) {
			Log.e(LOG_TAG,"",nnfe);
			errorId = errorId + 1;
		}
    	return showUpdateDialog;
    }
    
    public static boolean useAdmob(Context ctx) {
    	boolean useAdmob = false;
    	PackageManager pm = ctx.getPackageManager();
    	ComponentName cn = new ComponentName(ctx.getPackageName(),ADMOB_CLASS);
    	try {
    		pm.getActivityInfo(cn, PackageManager.GET_ACTIVITIES);
    		useAdmob = true;
    	} catch (NameNotFoundException nnfe) {
    		useAdmob = false;
    	}
    	return useAdmob;
    }
    
    public static String getFlurryAnalyticsApiKey(Context ctx) {
    	String flurryAnalyticsApiKey = null;
    	try {
	    	ApplicationInfo ai = ctx.getPackageManager().getApplicationInfo(ctx.getPackageName(), PackageManager.GET_META_DATA);
	    	flurryAnalyticsApiKey = ai.metaData.getString("FLURRY_API_KEY");
	    } catch (NameNotFoundException nnfe) {
			Log.e(LOG_TAG,"",nnfe);
		}
    	return flurryAnalyticsApiKey;
    }
    
    public static String getGoogleAnalyticsProfileId(Context ctx) {
    	String googleAnalyticsProfileId = null;
    	try {
	    	ApplicationInfo ai = ctx.getPackageManager().getApplicationInfo(ctx.getPackageName(), PackageManager.GET_META_DATA);
	    	googleAnalyticsProfileId = ai.metaData.getString("GOOGLE_ANALYTICS_PROFILE_ID");
	    } catch (NameNotFoundException nnfe) {
			Log.e(LOG_TAG,"",nnfe);
		}
    	return googleAnalyticsProfileId;
    }
    
    public static String getMobclixApplicationId(Context ctx) {
    	String mobclixApplicationId = null;
    	try {
	    	ApplicationInfo ai = ctx.getPackageManager().getApplicationInfo(ctx.getPackageName(), PackageManager.GET_META_DATA);
	    	mobclixApplicationId = ai.metaData.getString("com.mobclix.APPLICATION_ID");
	    } catch (NameNotFoundException nnfe) {
			Log.e(LOG_TAG,"",nnfe);
		}
    	return mobclixApplicationId;
    }
    
    public static boolean areAnalyticsEnabled(Context ctx) {
    	String flurryAnalyticsApiKey = getFlurryAnalyticsApiKey(ctx);
    	String googleAnalyticsProfileId = getGoogleAnalyticsProfileId(ctx);
    	String mobclixApplicationId = getMobclixApplicationId(ctx);
    	
    	if ((flurryAnalyticsApiKey == null || flurryAnalyticsApiKey.equals("") || flurryAnalyticsApiKey.equalsIgnoreCase("xxxxxxxxxxxxxxxxxxxx")) && (googleAnalyticsProfileId == null || googleAnalyticsProfileId.equals("") || googleAnalyticsProfileId.equalsIgnoreCase("UA-xxxxx-xx")) && (mobclixApplicationId == null || mobclixApplicationId.equals("") || mobclixApplicationId.equalsIgnoreCase("xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx")))
    		return false;
    	else
    		return true;
    }
    
    public static boolean trackFlurryAnalytics(Context ctx) {
    	boolean track = false;
    	String flurryAnalyticsApiKey = getFlurryAnalyticsApiKey(ctx);
    	
	    if (flurryAnalyticsApiKey == null || flurryAnalyticsApiKey.equals("") || flurryAnalyticsApiKey.equalsIgnoreCase("xxxxxxxxxxxxxxxxxxxx"))
	    	track = false;
    	else if (SharedPreferencesHelper.getPrefUsageData(ctx))
    		track = true;
	    
    	return track;
    }
    
    public static boolean trackGoogleAnalytics(Context ctx) {
    	boolean track = false;
    	String googleAnalyticsProfileId = getGoogleAnalyticsProfileId(ctx);
    	
	    if (googleAnalyticsProfileId == null || googleAnalyticsProfileId.equals("") || googleAnalyticsProfileId.equalsIgnoreCase("UA-xxxxx-xx"))
	    	track = false;
	    else if (SharedPreferencesHelper.getPrefUsageData(ctx))
    		track = true;
	    
    	return track;
    }
    
    public static boolean trackMobclixSession(Context ctx) {
    	boolean track = false;
    	String mobclixApplicationId = getMobclixApplicationId(ctx);
    	
	    if (mobclixApplicationId == null || mobclixApplicationId.equals("") || mobclixApplicationId.equalsIgnoreCase("xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"))
	    	track = false;
	    else if (SharedPreferencesHelper.getPrefUsageData(ctx))
    		track = true;
	    
    	return track;
    }
    
    // Getters/Setters for User Preferences shared in file res/xml/preferences.xml and accessed via 'Settings' option menu
    
    public static long getPrefStartChannel(Context ctx) {
    	return Long.parseLong(PreferenceManager.getDefaultSharedPreferences(ctx).getString(FeedPrefActivity.PREF_START_CHANNEL_KEY,FeedPrefActivity.DEFAULT_START_CHANNEL));
    }
    
    public static void setPrefStartChannel(Context ctx, long feedId) {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
    	Editor editor = prefs.edit();
    	editor.putString(FeedPrefActivity.PREF_START_CHANNEL_KEY, Long.toString(feedId));
    	SharedPreferencesCompat.apply(editor); //Strict Mode Performance since 2.3 API 9
    }
    
    // Get the item view type (offline or online)
    public static int getItemView(Context ctx) {
    	return Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(ctx).getString(FeedPrefActivity.PREF_ITEM_VIEW_KEY,FeedPrefActivity.DEFAULT_ITEM_VIEW));
    }
    
    public static int getPrefMaxItems(Context ctx) {
    	return Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(ctx).getString(FeedPrefActivity.PREF_MAX_ITEMS_KEY,FeedPrefActivity.DEFAULT_MAX_ITEMS_PER_FEED));
    }
    
    public static long getPrefMaxHours(Context ctx) {
    	return Long.parseLong(PreferenceManager.getDefaultSharedPreferences(ctx).getString(FeedPrefActivity.PREF_MAX_HOURS_KEY,FeedPrefActivity.DEFAULT_MAX_HOURS_PER_FEED));
    }
    
    // Get result in minutes
    public static long getPrefUpdatePeriod(Context ctx) {
    	return Long.parseLong(PreferenceManager.getDefaultSharedPreferences(ctx).getString(FeedPrefActivity.PREF_UPDATE_PERIOD_KEY,FeedPrefActivity.DEFAULT_UPDATE_PERIOD));
    }
    
    public static boolean getPrefUsageData(Context ctx) {
    	return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(FeedPrefActivity.PREF_USAGE_DATA_KEY,FeedPrefActivity.DEFAULT_USAGE_DATA);
    }
    
    public static void setPrefUsageData(Context ctx, boolean sendUsageData) {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
    	Editor editor = prefs.edit();
    	editor.putBoolean(FeedPrefActivity.PREF_USAGE_DATA_KEY, sendUsageData);
    	SharedPreferencesCompat.apply(editor); //Strict Mode Performance since 2.3 API 9
    }
    
    // Shared getter util methods
    
    public static CharSequence getVersionName(Context ctx) {
		CharSequence version_name = "";
		try {
			PackageInfo packageInfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
			version_name = packageInfo.versionName;
		} catch (NameNotFoundException nnfe) {
			Log.e(LOG_TAG,"",nnfe);
			errorId = errorId + 1;
		}
		return version_name;
    }
    
    public static int getVersionCode(Context ctx) {
		int version_code = 0;
		try {
			PackageInfo packageInfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
			version_code = packageInfo.versionCode;
		} catch (NameNotFoundException nnfe) {
			Log.e(LOG_TAG,"",nnfe);
			errorId = errorId + 1;
		}
		return version_code;
    }
    
    public static boolean isOnline(Context ctx) {
    	ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
    	NetworkInfo ni = cm.getActiveNetworkInfo();
    	if (ni != null)
    		return ni.isConnectedOrConnecting();
    	else return false;
    }
    
    public static boolean isNewUpdate(Context ctx) {
    	boolean isNewUpdate = false;
    	int prefVersionCode = SharedPreferencesHelper.getPrefVersionCode(ctx, 0);
    	int updateVersionCode = SharedPreferencesHelper.getVersionCode(ctx);
    	
    	if (updateVersionCode > prefVersionCode)
    		isNewUpdate = true;
    		
    	SharedPreferencesHelper.setPrefVersionCode(ctx, updateVersionCode);
    	
    	return isNewUpdate;
    }
}
