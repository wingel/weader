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

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;

import com.mfavez.android.feedgoal.storage.DbFeedAdapter;
import com.mfavez.android.feedgoal.storage.SharedPreferencesHelper;

/**
 * Displays the application startup splash screen.
 * @author Mathieu Favez
 * Created 15/04/2010
 */
public class SplashScreenActivity extends Activity {
	
	private static final String LOG_TAG = "SplashScreenActivity";
	private final Handler mHandler = new Handler();
 
    private final Runnable mPendingLauncherRunnable = new Runnable() {
        public void run() {
        	DbFeedAdapter mDbFeedAdapter = new DbFeedAdapter(SplashScreenActivity.this);
            mDbFeedAdapter.open();
            
            Intent intent = new Intent(SplashScreenActivity.this, FeedTabActivity.class);
            startActivity(intent);
            
            mDbFeedAdapter.close();
            finish();
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Track installations of the app (not the device!)
        if (SharedPreferencesHelper.getUniqueId(this) == null)
        	SharedPreferencesHelper.setUniqueId(this);
        
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        SharedPreferencesHelper.setPrefTabFeedId(this, SharedPreferencesHelper.getPrefStartChannel(this));
        
        // Prepare show Startup Dialog on update
        if (SharedPreferencesHelper.showPrefStartupDialogOnUpdate(this) && SharedPreferencesHelper.isNewUpdate(this)) {
        	SharedPreferencesHelper.setPrefStartupDialogOnUpdate(this, true);
        	// Fix bug inherited from version 1.6 (version code 17) and affecting new versions.
        	// Bug: dialog on install was displayed after each refresh.
        	SharedPreferencesHelper.setPrefStartupDialogOnInstall(this, false);
        } else
        	SharedPreferencesHelper.setPrefStartupDialogOnUpdate(this, false);
        
        setContentView(R.layout.splash_screen);
        
        Drawable backgroundDrawable = getResources().getDrawable(R.drawable.splash_background);
        backgroundDrawable.setDither(true);
        findViewById(android.R.id.content).setBackgroundDrawable(backgroundDrawable);
        mHandler.postDelayed(mPendingLauncherRunnable, SharedPreferencesHelper.getSplashDuration(this));
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mPendingLauncherRunnable);
    }
}
