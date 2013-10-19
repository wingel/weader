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

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TabHost.OnTabChangeListener;

import com.google.ads.AdRequest;
import com.google.ads.AdView;
import com.mfavez.android.feedgoal.common.Feed;
import com.mfavez.android.feedgoal.common.Item;
import com.mfavez.android.feedgoal.common.TrackerAnalyticsHelper;
import com.mfavez.android.feedgoal.storage.DbFeedAdapter;
import com.mfavez.android.feedgoal.storage.DbSchema;
import com.mfavez.android.feedgoal.storage.SharedPreferencesHelper;

/**
 * Displays a list of feed items in a tab layout.
 * @author Mathieu Favez
 * Created 15/04/2010
 */
public class FeedTabActivity extends TabActivity implements OnItemClickListener {
	private static final String LOG_TAG = "FeedTabActivity";
	private long errorId = 0;
	
	private static final String TAB_CHANNEL_TAG = "tab_tag_channel";
	private static final String TAB_FAV_TAG = "tab_tag_favorite";
	
	private static final int KILL_ACTIVITY_CODE = 1;
	
	private DbFeedAdapter mDbFeedAdapter;
	
	private boolean mIsOnline = true;
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mDbFeedAdapter = new DbFeedAdapter(this);
        mDbFeedAdapter.open();
        
        TrackerAnalyticsHelper.createTracker(this);

        setContentView(R.layout.main);
        
        /*
         * To test ads in emulator, remove ads:loadAdOnCreate="true" in layout
        if (SharedPreferencesHelper.useAdmob(this)) {
        	int resourceId = getResources().getIdentifier("adView", "id", this.getPackageName());
        	AdView adView = (AdView)this.findViewById(resourceId);
	        //AdView adView = (AdView)this.findViewById(R.id.adView);
	        AdRequest request = new AdRequest();
	        request.addTestDevice(AdRequest.TEST_EMULATOR);
	        adView.loadAd(request);
        }
        */
        
        long feedId = SharedPreferencesHelper.getPrefTabFeedId(this, mDbFeedAdapter.getFirstFeed().getId());
        
        Bundle extras = getIntent().getExtras();
		if (extras != null) {
			feedId = extras.getLong(DbSchema.FeedSchema._ID);
			SharedPreferencesHelper.setPrefTabFeedId(this, feedId);
		}
        
        Feed currentTabFeed = mDbFeedAdapter.getFeed(feedId);
        setTabs(TAB_CHANNEL_TAG, currentTabFeed.getTitle());
        
        getTabHost().setOnTabChangedListener(new OnTabChangeListener(){
        	@Override
        	public void onTabChanged(String tabId) {
        	    if(tabId.equals(TAB_FAV_TAG)) {
        	    	List<Item> items = fillListData(R.id.favoritelist);
        	        if (items.isEmpty())
            			Toast.makeText(FeedTabActivity.this, R.string.no_fav_msg, Toast.LENGTH_LONG).show();
        		} else if(tabId.equals(TAB_CHANNEL_TAG)) {
        	    	Feed currentTabFeed = mDbFeedAdapter.getFeed(SharedPreferencesHelper.getPrefTabFeedId(FeedTabActivity.this, mDbFeedAdapter.getFirstFeed().getId()));
        	    	if (currentTabFeed != null && outofdate(currentTabFeed.getId()))
	        	    	refreshFeed(currentTabFeed,false);
        	    	else
        	    		fillListData(R.id.feedlist);
        	    }
        	    setTabsBackgroundColor();
        	}
        });
        
        ListView feedListView = (ListView)findViewById(R.id.feedlist);	
        ListView favoriteListView = (ListView)findViewById(R.id.favoritelist);
        
        registerForContextMenu(feedListView);
        registerForContextMenu(favoriteListView);
        
        feedListView.setOnItemClickListener(this);
        favoriteListView.setOnItemClickListener(this);
    }
    
    @Override
    protected void onStart() {
    	super.onStart();
    	TrackerAnalyticsHelper.startTracker(this);
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	
    	if (getTabHost().getCurrentTabTag().equals(TAB_CHANNEL_TAG)) {
	    	Feed currentTabFeed = mDbFeedAdapter.getFeed(SharedPreferencesHelper.getPrefTabFeedId(FeedTabActivity.this, mDbFeedAdapter.getFirstFeed().getId()));
	    	if (currentTabFeed != null && outofdate(currentTabFeed.getId()))
		    	refreshFeed(currentTabFeed,false);
	    	else {
	    		fillListData(R.id.feedlist);
	    		// Show Startup Dialog on update when refresh was recently executed and not required to be executed again
	        	if (SharedPreferencesHelper.getPrefStartupDialogOnUpdate(this, false)) {
		        	showDialog(SharedPreferencesHelper.DIALOG_STARTUP);
		        	SharedPreferencesHelper.setPrefStartupDialogOnUpdate(FeedTabActivity.this, false);
	        	}
	    	}
    	} else
    		fillData(); // case on fav tab with not read fav item selected by the user => when back from web view, fav tab view needs to be refreshed in order to mark item as read
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	//mDbFeedAdapter.close();
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
        
    //Called when device orientation changed (see: android:configChanges="orientation" in manifest file)
    //Avoiding to restart the activity (which causes a crash) when orientation changes during refresh in AsyncTask
    @Override
    public void onConfigurationChanged(Configuration newConfig){        
        super.onConfigurationChanged(newConfig);
    }
    
    private boolean outofdate(long feedId) {
    	Date now = new Date();
    	Feed feed = mDbFeedAdapter.getFeed(feedId);
    	//Item lastItem = mDbFeedAdapter.getLastItem(feedId);
    	long diffTime = 0;
    	long updatePeriod = SharedPreferencesHelper.getPrefUpdatePeriod(this) * 60 * 1000; // Period expressed in milliseconds
    	/*
    	if (lastItem != null)
    		diffTime = now.getTime() - lastItem.getPubdate().getTime();
    		
    	else
    		return true;
    	*/
    	
    	// If manual update preference is set, update period < 0
    	if (feed == null || updatePeriod < 0)
    		return false;
    	
    	if (feed.getRefresh() != null)
    		diffTime = now.getTime() - feed.getRefresh().getTime();
    	else
    		return true;
    	
		// check if feed is out of date
		if (diffTime > updatePeriod)
			return true;
		else
			return false;
    }
    
    private void setTabs(String activeTab, String title) {
    	getTabHost().addTab(getTabHost().newTabSpec(TAB_CHANNEL_TAG).setIndicator(title).setContent(R.id.feedlist));
    	getTabHost().addTab(getTabHost().newTabSpec(TAB_FAV_TAG).setIndicator(getResources().getText(R.string.favorites),getResources().getDrawable(R.drawable.fav)).setContent(R.id.favoritelist));
    	getTabHost().setCurrentTabByTag(activeTab);
    	setTabsBackgroundColor();
    }
    
    // Set TabWidget color background
    private void setTabsBackgroundColor() {
    	for(int i=0;i<getTabHost().getTabWidget().getChildCount();i++) {
    		getTabHost().getTabWidget().getChildAt(i).setBackgroundColor(Color.DKGRAY); //unselected
        }
    	getTabHost().getTabWidget().getChildAt(getTabHost().getCurrentTab()).setBackgroundColor(Color.TRANSPARENT); // selected
    }
    
    private void refreshFeed(Feed feed, boolean alwaysDisplayOfflineDialog) {
    	if (SharedPreferencesHelper.isOnline(this)) {
    		mIsOnline = true;
    		new UpdateFeedTask().execute(feed);
    	} else {
    		if (mIsOnline || alwaysDisplayOfflineDialog) // May only display once the offline dialog for a better user experience
    			showDialog(SharedPreferencesHelper.DIALOG_NO_CONNECTION);
    		mIsOnline = false;
    		fillListData(R.id.feedlist);
    	}
    }
/*    
    private void updateFeed(Feed feed) throws SAXException, ParserConfigurationException, IOException {	
    	long feedId = feed.getId();
    	
    	FeedHandler feedHandler = new FeedHandler(this);
    	Feed refreshedFeed = feedHandler.handleFeed(feed.getURL());

    	refreshedFeed.setId(feedId);
    	mDbFeedAdapter.updateFeed(refreshedFeed);
    	//mDbFeedAdapter.updateFeed(feedId, mDbFeedAdapter.getUpdateContentValues(feed), feed.getItems());
    	mDbFeedAdapter.cleanDbItems(feedId);

    	FeedSharedPreferences.setPrefTabFeedId(this,feedId);
    	
    	//getTabHost().getTabWidget().removeAllViews();
    	//getTabHost().clearAllTabs();
    	//setTabs(TAB_FEED_TAG, mDbFeedAdapter.getFeed(feedId).getTitle());
    }
   
    private void addFeed(URL url) throws SAXException, ParserConfigurationException, IOException {
    	FeedHandler feedHandler = new FeedHandler(this);
    	Feed handledFeed = feedHandler.handleFeed(url);
    	
    	long feedId = mDbFeedAdapter.addFeed(handledFeed);
    	//long feedId = mDbFeedAdapter.addFeed(mDbFeedAdapter.getContentValues(feed), feed.getItems());
    	if (feedId != -1) {
	    	mDbFeedAdapter.cleanDbItems(feedId);
	    	
	    	SharedPreferencesHelper.setPrefTabFeedId(this,feedId);
	    	
	    	getTabHost().getTabWidget().removeAllViews();
	    	getTabHost().clearAllTabs();
	    	setTabs(TAB_CHANNEL_TAG, mDbFeedAdapter.getFeed(feedId).getTitle());
    	}
    }
*/ 
    private List<Item> fillData() {
    	if (getTabHost().getCurrentTabTag().equals(TAB_FAV_TAG))
    		return fillListData(R.id.favoritelist);
    	else
    		return fillListData(R.id.feedlist);
    }
    
    private List<Item> fillListData(int listResource) {
		ListView feedListView = (ListView)findViewById(listResource);
		
		List<Item> items = null;
		if (listResource == R.id.favoritelist) {
			TrackerAnalyticsHelper.trackPageView(this,"/favoriteListView");
			//items = mDbFeedAdapter.getFavoriteItems(SharedPreferencesHelper.getPrefMaxItems(this));
			items = mDbFeedAdapter.getFavoriteItems(0);
		} else {
			TrackerAnalyticsHelper.trackPageView(this,"/itemListView");
			Feed currentFeed = mDbFeedAdapter.getFeed(SharedPreferencesHelper.getPrefTabFeedId(this, mDbFeedAdapter.getFirstFeed().getId()));
			if (currentFeed != null && currentFeed.getRefresh() != null) {
				CharSequence formattedUpdate = DateFormat.format(getResources().getText(R.string.update_format_pattern), currentFeed.getRefresh());
        		//getWindow().setTitle(getString(R.string.app_name) + " - " + getString(R.string.last_update) + " " + formattedUpdate);
				getWindow().setTitle(getString(R.string.app_name) + " - " + formattedUpdate);
			}
        	items = mDbFeedAdapter.getItems(SharedPreferencesHelper.getPrefTabFeedId(this, mDbFeedAdapter.getFirstFeed().getId()), 1, SharedPreferencesHelper.getPrefMaxItems(this));
		}

		FeedArrayAdapter arrayAdapter = new FeedArrayAdapter(this, R.id.title, items);
		feedListView.setAdapter(arrayAdapter);
		
		//feedListView.setSelection(0);
		
		return items;
    }
    
    public void onItemClick (AdapterView<?> parent, View v, int position, long id) {
    	Item item = mDbFeedAdapter.getItem(id);

    	//if (item != null) {
	    	//item.read();
	    	ContentValues values = new ContentValues();
	    	values.put(DbSchema.ItemSchema.COLUMN_READ, DbSchema.ON);
	    	mDbFeedAdapter.updateItem(id, values, null);
	    	//mDbFeedAdapter.updateItem(FeedSharedPreferences.getPrefTabFeedId(this), item);
	    	Intent intent = null;
	    	if (SharedPreferencesHelper.getItemView(this) == 0) {
	    		//if ((item.getDescription() == null && item.getContent() == null) || (item.getDescription() != null && item.getDescription().trim().equals("") && item.getContent() != null && item.getContent().trim().equals(""))) {
	    		if (item.getContent() == null || item.getContent().trim().equals("") || item.getContent().length() <= SharedPreferencesHelper.MIN_CONTENT_LENGTH) {
	    	    	TrackerAnalyticsHelper.trackEvent(this,LOG_TAG,"List_Select_ItemOnline",item.getLink().toString(),1);
	    			intent = new Intent(this, FeedWebActivity.class);
	    		} else {
	    	    	TrackerAnalyticsHelper.trackEvent(this,LOG_TAG,"List_Select_ItemOffline",item.getLink().toString(),1);
	    			intent = new Intent(this, FeedItemActivity.class);
	    		}
	    	} else {
    	    	TrackerAnalyticsHelper.trackEvent(this,LOG_TAG,"List_Select_ItemOnline",item.getLink().toString(),1);
	    		intent = new Intent(this, FeedWebActivity.class);
	    	}
	    	
	        intent.putExtra(DbSchema.ItemSchema._ID, id);
	        startActivityForResult(intent, KILL_ACTIVITY_CODE);
    	//}
	}
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    	super.onActivityResult(requestCode, resultCode, intent);

    	switch(requestCode) {
	    	case KILL_ACTIVITY_CODE:
	    	    if (resultCode == RESULT_OK)
	    	    	finish();
	    	    break;
	    	}
    }
    
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        if (SharedPreferencesHelper.isDynamicMode(this)) {
        	inflater.inflate(R.menu.opt_tab_menu_public_mode, menu);
        	MenuItem channelsMenuItem = (MenuItem) menu.findItem(R.id.menu_opt_channels);
        	channelsMenuItem.setIntent(new Intent(this,FeedChannelsActivity.class));
        } else {
        	inflater.inflate(R.menu.opt_tab_menu_private_mode, menu);
        	
            if (mDbFeedAdapter.getFeeds().size() > 1) {
    	        MenuItem channelsMenuItem = (MenuItem) menu.findItem(R.id.menu_opt_channels);
    	        SubMenu subMenu = channelsMenuItem.getSubMenu();
    	        
    	        List<Feed> feeds = mDbFeedAdapter.getFeeds();
    	        Iterator<Feed> feedIterator = feeds.iterator();
    	        Feed feed = null;
    	        MenuItem channelSubMenuItem = null;
    	        Intent intent = null;
    	        int order = 0;
    			while (feedIterator.hasNext()) {
    				feed = feedIterator.next();
    				channelSubMenuItem = subMenu.add(SharedPreferencesHelper.CHANNEL_SUBMENU_GROUP, Menu.NONE, order, feed.getTitle());
    				
    				if (feed.getId() == SharedPreferencesHelper.getPrefTabFeedId(this, mDbFeedAdapter.getFirstFeed().getId()))
    					channelSubMenuItem.setChecked(true);
    				
    				intent = new Intent(this, FeedTabActivity.class);
    		        intent.putExtra(DbSchema.FeedSchema._ID, feed.getId());
    				channelSubMenuItem.setIntent(intent);
    				
    				order++;
    			}

    			subMenu.setGroupCheckable(SharedPreferencesHelper.CHANNEL_SUBMENU_GROUP, true, true);
            } else {
            	menu.removeItem(R.id.menu_opt_channels);
            }
        }
        
        // Preferences menu item
        MenuItem preferencesMenuItem = (MenuItem) menu.findItem(R.id.menu_opt_preferences);
        preferencesMenuItem.setIntent(new Intent(this,FeedPrefActivity.class));
        
        return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
	        case R.id.menu_opt_refresh:
	        	if (getTabHost().getCurrentTabTag().equals(TAB_FAV_TAG)) {
		        	TrackerAnalyticsHelper.trackEvent(this,LOG_TAG,"OptionMenu_RefreshFavoriteList","Refresh",1);
	        		// Refreshing favorites will never find new favorite items, because they are local (not updated from Internet)
	        		fillListData(R.id.favoritelist);
	        		Toast.makeText(this, R.string.no_new_fav_item_msg, Toast.LENGTH_LONG).show();
	        	} else if (getTabHost().getCurrentTabTag().equals(TAB_CHANNEL_TAG)) {
	        		Feed currentTabFeed = mDbFeedAdapter.getFeed(SharedPreferencesHelper.getPrefTabFeedId(this, mDbFeedAdapter.getFirstFeed().getId()));
			    	if (currentTabFeed != null) {
			        	TrackerAnalyticsHelper.trackEvent(this,LOG_TAG,"OptionMenu_RefreshItemList",currentTabFeed.getURL().toString(),1);
				    	refreshFeed(currentTabFeed,true);
			    	}
	        	}
	            return true;
	        case R.id.menu_opt_channels:
	        	TrackerAnalyticsHelper.trackEvent(this,LOG_TAG,"OptionMenu_Channels","Channels",1);
	        	if (SharedPreferencesHelper.isDynamicMode(this)) {
	        		startActivityForResult(item.getIntent(), KILL_ACTIVITY_CODE);
	        	} else {
	        		//do nothing
	        	}
	            return true;
	        case R.id.menu_opt_preferences:
	        	TrackerAnalyticsHelper.trackEvent(this,LOG_TAG,"OptionMenu_Preferences","Preferences",1);
	        	startActivity(item.getIntent());
	            return true;
	        case R.id.menu_opt_about:
	        	TrackerAnalyticsHelper.trackEvent(this,LOG_TAG,"OptionMenu_AboutDialog","About",1);
	        	showDialog(SharedPreferencesHelper.DIALOG_ABOUT);
	            return true;
	        default:
	        	if (item.getGroupId() == SharedPreferencesHelper.CHANNEL_SUBMENU_GROUP) {
	        		startActivity(item.getIntent());
	        		finish();
	        		return true;
	        	}
	    }
        return super.onOptionsItemSelected(item);
    }
    
    public void onCreateContextMenu (ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    	super.onCreateContextMenu(menu, v, menuInfo);
    	
		if (v.getId() == R.id.feedlist || v.getId() == R.id.favoritelist) {
			menu.setHeaderTitle (R.string.ctx_menu_title);
			MenuInflater inflater = getMenuInflater();
			AdapterView.AdapterContextMenuInfo acmi = (AdapterView.AdapterContextMenuInfo) menuInfo;
			Item item = mDbFeedAdapter.getItem(acmi.id);
			if (item != null) {
				if (item.isFavorite())
		    		inflater.inflate(R.menu.ctx_menu_notfav, menu);
		    	else
		    		inflater.inflate(R.menu.ctx_menu_fav, menu);
			}
		}
    }

    
    public boolean onContextItemSelected(MenuItem menuItem) {
    	final AdapterContextMenuInfo acmi = (AdapterContextMenuInfo) menuItem.getMenuInfo();
    	Item item = mDbFeedAdapter.getItem(acmi.id);
    	ContentValues values = null;
    	switch (menuItem.getItemId()) {
    		case R.id.add_fav:
	        	TrackerAnalyticsHelper.trackEvent(this,LOG_TAG,"ContextMenu_AddFavorite",item.getLink().toString(),1);
    			//item.favorite();
    			values = new ContentValues();
    	    	values.put(DbSchema.ItemSchema.COLUMN_FAVORITE, DbSchema.ON);
    	    	mDbFeedAdapter.updateItem(acmi.id, values, null);
    			fillData();
    			Toast.makeText(this, R.string.add_fav_msg, Toast.LENGTH_SHORT).show();
    			return true;
    		case R.id.remove_fav:
	        	TrackerAnalyticsHelper.trackEvent(this,LOG_TAG,"ContextMenu_RemoveFavorite",item.getLink().toString(),1);
    			//item.unfavorite();
    			Date now = new Date();
    			long diffTime = now.getTime() - item.getPubdate().getTime();
    			long maxTime = SharedPreferencesHelper.getPrefMaxHours(this) * 60 * 60 * 1000; // Max hours expressed in milliseconds
    			// test if item has expired
    			if (maxTime > 0 && diffTime > maxTime) {
	    			AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    			builder.setMessage(R.string.remove_fav_confirmation)
	    			       .setCancelable(false)
	    			       .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
	    			           public void onClick(DialogInterface dialog, int id) {
									ContentValues values = new ContentValues();
									values.put(DbSchema.ItemSchema.COLUMN_FAVORITE, DbSchema.OFF);
									mDbFeedAdapter.updateItem(acmi.id, values, null);
									fillData();
									Toast.makeText(FeedTabActivity.this, R.string.remove_fav_msg, Toast.LENGTH_SHORT).show();
	    			           }
	    			       })
	    			       .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
	    			           public void onClick(DialogInterface dialog, int id) {
	    			                dialog.cancel();
	    			           }
	    			       });
	    			builder.create().show();
    			} else {
    				values = new ContentValues();
        	    	values.put(DbSchema.ItemSchema.COLUMN_FAVORITE, DbSchema.OFF);
        	    	mDbFeedAdapter.updateItem(acmi.id, values, null);
        			fillData();
        			Toast.makeText(this, R.string.remove_fav_msg, Toast.LENGTH_SHORT).show();
    			}
    			return true;
    		case R.id.share:
    			if (item != null) {
    	        	TrackerAnalyticsHelper.trackEvent(this,LOG_TAG,"ContextMenu_Share",item.getLink().toString(),1);
	    			Intent shareIntent = new Intent(Intent.ACTION_SEND);
	                shareIntent.putExtra(Intent.EXTRA_SUBJECT, String.format(getString(R.string.share_subject), getString(R.string.app_name)));
	                shareIntent.putExtra(Intent.EXTRA_TEXT, item.getTitle() + " " + item.getLink().toString());
	                shareIntent.setType("text/plain");
	                startActivity(Intent.createChooser(shareIntent, getString(R.string.share)));
    			}
    			return true;
    		default:
    			return super.onContextItemSelected(menuItem);
    	}
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
    	Dialog dialog = null;
    	CharSequence title = null;
    	LayoutInflater inflater = null;
    	View dialogLayout = null;
    	AlertDialog.Builder builder = null;
        switch (id) {
        	case SharedPreferencesHelper.DIALOG_UPDATE_PROGRESS:
	            dialog = new ProgressDialog(this);
	            ((ProgressDialog)dialog).setTitle(getResources().getText(R.string.updating));
	            ((ProgressDialog)dialog).setIcon(R.drawable.ic_dialog);
	            ((ProgressDialog)dialog).setMessage(getResources().getText(R.string.downloading));
	            ((ProgressDialog)dialog).setIndeterminate(true);
	            dialog.setCancelable(false);
	            break;
        	case SharedPreferencesHelper.DIALOG_ABOUT:
        		//title = getResources().getText(R.string.app_name) + " - " + getResources().getText(R.string.version) + " " + SharedPreferencesHelper.getVersionName(this);
	        	title = getString(R.string.app_name) + " - " + getString(R.string.version) + " " + SharedPreferencesHelper.getVersionName(this);
        		
	        	/*
	        	 * Without cancel button
	        	dialog = new Dialog(this);
	        	dialog.setContentView(R.layout.dialog_about);
	        	dialog.setTitle(title);
	        	*/
        		inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        		dialogLayout = inflater.inflate(R.layout.dialog_about, null);
        		TextView childView = null;
        		if (getString(R.string.website).equals("")) {
        			childView = (TextView) dialogLayout.findViewById(R.id.website);
        			childView.setVisibility(View.GONE);
        		}
        		if (getString(R.string.email).equals("")) {
        			childView = (TextView) dialogLayout.findViewById(R.id.email);
        			childView.setVisibility(View.GONE);
        		}
        		if (getString(R.string.contact).equals("")) {
        			childView = (TextView) dialogLayout.findViewById(R.id.contact);
        			childView.setVisibility(View.GONE);
        		}
        		if (getString(R.string.powered).equals("")) {
        			childView = (TextView) dialogLayout.findViewById(R.id.powered);
        			childView.setVisibility(View.GONE);
        		}
        		builder = new AlertDialog.Builder(this);
        		builder.setView(dialogLayout)
        			   .setTitle(title)
        			   .setIcon(R.drawable.ic_dialog)
        			   .setNeutralButton(R.string.close, new DialogInterface.OnClickListener() {
        		           public void onClick(DialogInterface dialog, int id) {
        		                dialog.cancel();
        		           }
        		       });
        		dialog = builder.create();
        		break;
        	case SharedPreferencesHelper.DIALOG_NO_CONNECTION:
        		title = getString(R.string.error);
        		inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        		dialogLayout = inflater.inflate(R.layout.dialog_no_connection, null);
        		builder = new AlertDialog.Builder(this);
        		builder.setView(dialogLayout)
        			   .setTitle(title)
        			   .setIcon(R.drawable.ic_dialog)
        			   .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
        		           public void onClick(DialogInterface dialog, int id) {
        		                dialog.cancel();
        		           }
        		       });
        		dialog = builder.create();
        		break;
        	case SharedPreferencesHelper.DIALOG_STARTUP:
        		title = getString(R.string.app_name) + " - " + getString(R.string.version) + " " + SharedPreferencesHelper.getVersionName(this);
        		inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        		if (SharedPreferencesHelper.getPrefStartupDialogOnInstall(this, false))
        			dialogLayout = inflater.inflate(R.layout.dialog_startup_install, null);
        		else if (SharedPreferencesHelper.getPrefStartupDialogOnUpdate(this, false))
        			dialogLayout = inflater.inflate(R.layout.dialog_startup_update, null);
        		final CheckBox sendUsageData = (CheckBox)dialogLayout.findViewById(R.id.checkbox_usage_data);
        		if (sendUsageData != null && !SharedPreferencesHelper.areAnalyticsEnabled(this))
        			sendUsageData.setVisibility(View.GONE);
        		//sendUsageData.setChecked(SharedPreferencesHelper.getPrefUsageData(FeedTabActivity.this));
        		builder = new AlertDialog.Builder(this);
        		builder.setView(dialogLayout)
        			   .setTitle(title)
        			   .setIcon(R.drawable.ic_dialog)
        			   .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
        		           public void onClick(DialogInterface dialog, int id) {
        		        	   if (sendUsageData != null && SharedPreferencesHelper.areAnalyticsEnabled(FeedTabActivity.this)) {
	        		        	   if (sendUsageData.isChecked()) {
	        		        		   SharedPreferencesHelper.setPrefUsageData(FeedTabActivity.this, true);
	        		        		   TrackerAnalyticsHelper.startTracker(FeedTabActivity.this);
	        		        		   TrackerAnalyticsHelper.trackEvent(FeedTabActivity.this,LOG_TAG,"Startup_Dialog_Data_Usage","true",1);
	        		        	   } else {
	        		        		   SharedPreferencesHelper.setPrefUsageData(FeedTabActivity.this, false);
	        		        	   }
        		        	   }
        		               dialog.cancel();
        		           }
        		       });
        		dialog = builder.create();
        		break;
            default:
            	dialog = null;
        }
        return dialog;
    }
    
    private class FeedArrayAdapter extends ArrayAdapter<Item> {
    	private LayoutInflater mInflater;

    	public FeedArrayAdapter(Context context, int textViewResourceId, List<Item> objects) {
    		super(context, textViewResourceId, objects);
    		// Cache the LayoutInflate to avoid asking for a new one each time.
            mInflater = LayoutInflater.from(context);
    	}
    	
    	@Override
    	public long getItemId(int position) {
    		return getItem(position).getId();
    	}

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
        	int[] item_rows = {R.layout.channel_item_row_notselected_notfavorite, R.layout.channel_item_row_selected_notfavorite,R.layout.channel_item_row_notselected_favorite,R.layout.channel_item_row_selected_favorite,R.layout.fav_item_row_notselected_favorite,R.layout.fav_item_row_selected_favorite,};
        	int item_row = item_rows[0]; // Default initialization
        	
        	Item item = getItem(position);
            
        	View view = convertView;
        	// Always inflate view, in order to display correctly the 'read' and 'favorite' states of the items => to apply the right layout+style.
            //if (view == null) {
	            //LayoutInflater li = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
	            if (item.isRead())
	            	if (item.isFavorite())
	            		if (getTabHost().getCurrentTabTag().equals(TAB_FAV_TAG))
	            			item_row = item_rows[5];
	            		else
	            			item_row = item_rows[3];
	            	else
	            		item_row = item_rows[1];
	            else
	            	if (item.isFavorite())
	            		if (getTabHost().getCurrentTabTag().equals(TAB_FAV_TAG))
	            			item_row = item_rows[4];
	            		else
	            			item_row = item_rows[2];
	            	else
	            		item_row = item_rows[0];
	            view = mInflater.inflate(item_row, null);
            //}
            
            TextView titleView = (TextView) view.findViewById(R.id.title);
            TextView channelView = (TextView) view.findViewById(R.id.channel); // only displayed in favorite view
            TextView pubdateView = (TextView) view.findViewById(R.id.pubdate);
            if (titleView != null)
            	titleView.setText(item.getTitle());
            if (channelView != null) {
            	Feed feed = mDbFeedAdapter.getFeed(mDbFeedAdapter.getItemFeedId(item.getId()));
            	if (feed != null)
            		channelView.setText(feed.getTitle());
            } if (pubdateView != null) {
            	//DateFormat df = new SimpleDateFormat(getResources().getText(R.string.pubdate_format_pattern);
            	//pubdateView.setText(df.format(item.getPubdate()));
            	CharSequence formattedPubdate = DateFormat.format(getResources().getText(R.string.pubdate_format_pattern), item.getPubdate());
            	pubdateView.setText(formattedPubdate);
            }
            
            return view;
        }
    }
    
    private class UpdateFeedTask extends AsyncTask<Feed, Void, Boolean> {
    	private long feedId = -1;
    	private long lastItemIdBeforeUpdate = -1;
    	
    	public UpdateFeedTask() {
    		super();
    	}
    	
        protected Boolean doInBackground(Feed...params) {
        	feedId = params[0].getId();
        	Item lastItem = mDbFeedAdapter.getLastItem(feedId);
        	if (lastItem != null)
        		lastItemIdBeforeUpdate = lastItem.getId();
        	
        	FeedHandler feedHandler = new FeedHandler(FeedTabActivity.this);
        	
        	try {
	        	Feed handledFeed = feedHandler.handleFeed(params[0].getURL());
	
	        	handledFeed.setId(feedId);
	        	
	        	mDbFeedAdapter.updateFeed(handledFeed);
	        	//mDbFeedAdapter.updateFeed(handledFeed.getId(), mDbFeedAdapter.getUpdateContentValues(handledFeed), handledFeed.getItems());
	        	mDbFeedAdapter.cleanDbItems(feedId);
	
	        	SharedPreferencesHelper.setPrefTabFeedId(FeedTabActivity.this,feedId);
	        	
        	} catch (IOException ioe) {
        		Log.e(LOG_TAG,"",ioe);
        		errorId = errorId + 1;
        		TrackerAnalyticsHelper.trackError(FeedTabActivity.this, Long.toString(errorId), ioe.getMessage(), LOG_TAG);
        		return new Boolean(false);
            } catch (SAXException se) {
            	Log.e(LOG_TAG,"",se);
        		errorId = errorId + 1;
        		TrackerAnalyticsHelper.trackError(FeedTabActivity.this, Long.toString(errorId), se.getMessage(), LOG_TAG);
            	return new Boolean(false);
            } catch (ParserConfigurationException pce) {
            	Log.e(LOG_TAG,"",pce);
        		errorId = errorId + 1;
        		TrackerAnalyticsHelper.trackError(FeedTabActivity.this, Long.toString(errorId), pce.getMessage(), LOG_TAG);
            	return new Boolean(false);
            }
            
            return new Boolean(true);
        }
        
        protected void onPreExecute() {
        	showDialog(SharedPreferencesHelper.DIALOG_UPDATE_PROGRESS);
        }

        protected void onPostExecute(Boolean result) {				
        	fillListData(R.id.feedlist);
        	dismissDialog(SharedPreferencesHelper.DIALOG_UPDATE_PROGRESS);
        	
        	// Show Startup Dialog
        	if (SharedPreferencesHelper.getPrefStartupDialogOnInstall(FeedTabActivity.this, false) || SharedPreferencesHelper.getPrefStartupDialogOnUpdate(FeedTabActivity.this, false)) {
	        	showDialog(SharedPreferencesHelper.DIALOG_STARTUP);
	        	SharedPreferencesHelper.setPrefStartupDialogOnInstall(FeedTabActivity.this, false);
	        	SharedPreferencesHelper.setPrefStartupDialogOnUpdate(FeedTabActivity.this, false);
        	}
        	
        	long lastItemIdAfterUpdate = -1;
        	Item lastItem = mDbFeedAdapter.getLastItem(feedId);
        	if (lastItem != null)
        		lastItemIdAfterUpdate = lastItem.getId();
        	if (lastItemIdAfterUpdate > lastItemIdBeforeUpdate)
        		Toast.makeText(FeedTabActivity.this, R.string.new_item_msg, Toast.LENGTH_LONG).show();
        	else
        		Toast.makeText(FeedTabActivity.this, R.string.no_new_item_msg, Toast.LENGTH_LONG).show();
        }
    }
}