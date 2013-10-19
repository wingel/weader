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

package com.mfavez.android.feedgoal.storage;

import android.app.backup.BackupAgentHelper;
import android.app.backup.SharedPreferencesBackupHelper;

/**
 * Helper class to backup user preferences:
 * - SharedPreferences saved in PREFS_FILE_NAME
 * - preferences.xml used in FeedPrefActivity (Setting Activity)
 */
public class PrefsBackupAgentHelper extends BackupAgentHelper {
	private static final String LOG_TAG = "PrefsBackupAgentHelper";
	
	// The name of the file corresponding to FeedPrefActivity
    static final String PREFS_ACTIVITY_ENDING_COMMON_FILE_NAME = "_" + "preferences";

    // A key to uniquely identify the set of backup data
    static final String PREFS_BACKUP_KEY = "prefs_backup_key";

    // Allocate a helper and add it to the backup agent
    @Override
    public void onCreate() {
    	String prefsActivityFullFileName = this.getPackageName() + PREFS_ACTIVITY_ENDING_COMMON_FILE_NAME;
        SharedPreferencesBackupHelper helper = new SharedPreferencesBackupHelper(this, SharedPreferencesHelper.PREFS_FILE_NAME, prefsActivityFullFileName);
        addHelper(PREFS_BACKUP_KEY, helper);
    }
}
