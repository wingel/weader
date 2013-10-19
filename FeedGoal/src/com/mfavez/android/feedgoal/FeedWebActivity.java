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

import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.Window;
import android.view.View.OnLongClickListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import com.mfavez.android.feedgoal.common.Feed;
import com.mfavez.android.feedgoal.common.Item;
import com.mfavez.android.feedgoal.common.TrackerAnalyticsHelper;
import com.mfavez.android.feedgoal.storage.DbFeedAdapter;
import com.mfavez.android.feedgoal.storage.DbSchema;
import com.mfavez.android.feedgoal.storage.SharedPreferencesHelper;

/**
 * Displays the content of an item in a web client.
 * @author Mathieu Favez
 * Created 15/04/2010
 */
public class FeedWebActivity extends Activity {
	
	private static final String LOG_TAG = "FeedWebActivity";
	private long errorId = 0;
	private static final int KILL_ACTIVITY_CODE = 1;
	
	private DbFeedAdapter mDbFeedAdapter;
	private WebView webView;
	private long mItemId = -1;
	
	private class ItemWebViewClient extends WebViewClient {
		
		@Override
	    public boolean shouldOverrideUrlLoading(WebView view, String url) {
			// Handles Android Market URL's
			if (url != null &&  url.indexOf("market://") == 0) {
				try {
					TrackerAnalyticsHelper.trackEvent(FeedWebActivity.this,LOG_TAG,"Android Market URL",url,1);
					startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
					return true;
				} catch(Exception e) {
					Log.e(LOG_TAG,"",e);
					errorId = errorId + 1;
	        		TrackerAnalyticsHelper.trackError(FeedWebActivity.this, Long.toString(errorId), e.getMessage(), LOG_TAG);
					return super.shouldOverrideUrlLoading(view,url);
				}
            } else {
            	//view.loadUrl(url);
                return super.shouldOverrideUrlLoading(view,url);
            }
	    }
		
		@Override
		public void onPageStarted (WebView view, String url, Bitmap favicon) {
			setProgressBarIndeterminateVisibility(true);
		}
		
		@Override
		public void onPageFinished (WebView view, String url) {
			setProgressBarIndeterminateVisibility(false);
		}
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	
    	// Request progress bar
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    	
        mDbFeedAdapter = new DbFeedAdapter(this);
        mDbFeedAdapter.open();
        
        TrackerAnalyticsHelper.createTracker(this);
    	
        setContentView(R.layout.webview);
        
        //registerForContextMenu(findViewById(R.id.webview));
        WebView webview = (WebView)this.findViewById(R.id.webview);
        webview.setOnLongClickListener(new OnLongClickListener() {
            public boolean onLongClick(View v) {
            	final ArrayList<CharSequence> options = new ArrayList<CharSequence>();
            	Item item = mDbFeedAdapter.getItem(mItemId);
    			
    			if (item != null) {
    				long feedId = mDbFeedAdapter.getItemFeedId(mItemId);
    				boolean isFirstItem = false;
    				boolean isLastItem = false;
    				if (mItemId == mDbFeedAdapter.getFirstItem(feedId).getId())
    					isFirstItem = true;
    				else if (mItemId == mDbFeedAdapter.getLastItem(feedId).getId())
    					isLastItem = true;
    				
    				if (item.isFavorite()) {
    					if (isFirstItem) {
    						options.add(getString(R.string.remove_fav));
    						options.add(getString(R.string.next_item));
    						options.add(getString(R.string.share));
    					} else if (isLastItem) {
    						options.add(getString(R.string.remove_fav));
    						options.add(getString(R.string.previous_item));
    						options.add(getString(R.string.share));
    					} else {
    						options.add(getString(R.string.remove_fav));
    						options.add(getString(R.string.next_item));
    						options.add(getString(R.string.previous_item));
    						options.add(getString(R.string.share));
    					}
    				} else {
    					if (isFirstItem) {
    						options.add(getString(R.string.add_fav));
    						options.add(getString(R.string.next_item));
    						options.add(getString(R.string.share));
    					} else if (isLastItem) {
    						options.add(getString(R.string.add_fav));
    						options.add(getString(R.string.previous_item));
    						options.add(getString(R.string.share));
    					} else {
    						options.add(getString(R.string.add_fav));
    						options.add(getString(R.string.next_item));
    						options.add(getString(R.string.previous_item));
    						options.add(getString(R.string.share));
    					}
    				}
    			}
            	
                AlertDialog.Builder alertListBuilder = new AlertDialog.Builder(FeedWebActivity.this);
                alertListBuilder.setTitle(R.string.ctx_menu_title)
                	            .setIcon(R.drawable.ic_dialog);
                alertListBuilder.setItems((CharSequence[])options.toArray(new CharSequence[options.size()]), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int optionId) {
                        Item item = mDbFeedAdapter.getItem(mItemId);
                    	ContentValues values = null;
                    	Intent intent = null;
                    	long feedId = -1;
                    	
                    	CharSequence option = (CharSequence)options.get(optionId);
                    	if (option.equals(getString(R.string.add_fav))) {
            	        	TrackerAnalyticsHelper.trackEvent(FeedWebActivity.this,LOG_TAG,"OptionDialog_AddFavorite",item.getLink().toString(),1);
                			//item.favorite();
                			values = new ContentValues();
                	    	values.put(DbSchema.ItemSchema.COLUMN_FAVORITE, DbSchema.ON);
                	    	mDbFeedAdapter.updateItem(mItemId, values, null);
                			Toast.makeText(FeedWebActivity.this, R.string.add_fav_msg, Toast.LENGTH_SHORT).show();
                    	} else if (option.equals(getString(R.string.remove_fav))) {
                    		TrackerAnalyticsHelper.trackEvent(FeedWebActivity.this,LOG_TAG,"OptionDialog_RemoveFavorite",item.getLink().toString(),1);
                			//item.unfavorite();
                			Date now = new Date();
                			long diffTime = now.getTime() - item.getPubdate().getTime();
                			long maxTime = SharedPreferencesHelper.getPrefMaxHours(FeedWebActivity.this) * 60 * 60 * 1000; // Max hours expressed in milliseconds
                			// test if item has expired
                			if (maxTime > 0 && diffTime > maxTime) {
            	    			AlertDialog.Builder builder = new AlertDialog.Builder(FeedWebActivity.this);
            	    			builder.setMessage(R.string.remove_fav_confirmation)
            	    			       .setCancelable(false)
            	    			       .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            	    			           public void onClick(DialogInterface dialog, int id) {
            									ContentValues values = new ContentValues();
            									values.put(DbSchema.ItemSchema.COLUMN_FAVORITE, DbSchema.OFF);
            									mDbFeedAdapter.updateItem(mItemId, values, null);
            									Toast.makeText(FeedWebActivity.this, R.string.remove_fav_msg, Toast.LENGTH_SHORT).show();
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
                    	    	mDbFeedAdapter.updateItem(mItemId, values, null);
                    			Toast.makeText(FeedWebActivity.this, R.string.remove_fav_msg, Toast.LENGTH_SHORT).show();
                			}
                    	} else if (option.equals(getString(R.string.next_item))) {
                    		TrackerAnalyticsHelper.trackEvent(FeedWebActivity.this,LOG_TAG,"OptionDialog_NextItem",item.getLink().toString(),1);
                			feedId = mDbFeedAdapter.getItemFeedId(mItemId);
                			intent = new Intent(FeedWebActivity.this, FeedWebActivity.class);
                	        intent.putExtra(DbSchema.ItemSchema._ID, mDbFeedAdapter.getNextItemId(feedId, mItemId));
                	        startActivity(intent);
                	        finish();
                    	} else if (option.equals(getString(R.string.previous_item))) {
                    		TrackerAnalyticsHelper.trackEvent(FeedWebActivity.this,LOG_TAG,"OptionDialog_PreviousItem",item.getLink().toString(),1);
                			feedId = mDbFeedAdapter.getItemFeedId(mItemId);
                			intent = new Intent(FeedWebActivity.this, FeedWebActivity.class);
                	        intent.putExtra(DbSchema.ItemSchema._ID, mDbFeedAdapter.getPreviousItemId(feedId, mItemId));
                	        startActivity(intent);
                	        finish();
                    	} else if (option.equals(getString(R.string.share))) {
                    		if (item != null) {
                	        	TrackerAnalyticsHelper.trackEvent(FeedWebActivity.this,LOG_TAG,"OptionDialog_Share",item.getLink().toString(),1);
            	    			Intent shareIntent = new Intent(Intent.ACTION_SEND);
            	                shareIntent.putExtra(Intent.EXTRA_SUBJECT, String.format(getString(R.string.share_subject), getString(R.string.app_name)));
            	                shareIntent.putExtra(Intent.EXTRA_TEXT, item.getTitle() + " " + item.getLink().toString());
            	                shareIntent.setType("text/plain");
            	                startActivity(Intent.createChooser(shareIntent, getString(R.string.share)));
                			}
                    	}
                    }
                });
                alertListBuilder.create().show();            	
            	return true;
            }
            	
         });   

        mItemId = savedInstanceState != null ? savedInstanceState.getLong(DbSchema.ItemSchema._ID) : -1;

		if (mItemId == -1) {
			Bundle extras = getIntent().getExtras();            
			mItemId = extras != null ? extras.getLong(DbSchema.ItemSchema._ID) : -1;
		}
    }
    
    private void displayWebView() {
    	if (!isOnline())
			showDialog(SharedPreferencesHelper.DIALOG_NO_CONNECTION);
		else if (mItemId != -1) {
			URL link = mDbFeedAdapter.getItem(mItemId).getLink();
			
			webView = (WebView) findViewById(R.id.webview);
	        webView.getSettings().setJavaScriptEnabled(true);
	        webView.getSettings().setBuiltInZoomControls(true);
	        webView.setInitialScale(70);
	        webView.loadUrl(link.toString());
	        webView.setWebViewClient(new ItemWebViewClient());
	        
	        // set item as read (case when item is displayed from next/previous contextual menu or buttons)
            ContentValues values = new ContentValues();
	    	values.put(DbSchema.ItemSchema.COLUMN_READ, DbSchema.ON);
	    	mDbFeedAdapter.updateItem(mItemId, values, null);
	    	
    		TrackerAnalyticsHelper.trackPageView(this,"/onlineItemView");
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
    	displayWebView();
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
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(DbSchema.ItemSchema._ID, mItemId);
    }
    
    private boolean isOnline() {
    	ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    	NetworkInfo ni = cm.getActiveNetworkInfo();
    	if (ni != null)
    		return ni.isConnectedOrConnecting();
    	else return false;
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
        	webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        if (SharedPreferencesHelper.isDynamicMode(this)) {
        	inflater.inflate(R.menu.opt_item_menu_public_mode, menu);
        	MenuItem channelsMenuItem = (MenuItem) menu.findItem(R.id.menu_opt_channels);
        	channelsMenuItem.setIntent(new Intent(this,FeedChannelsActivity.class));
        } else {
        	inflater.inflate(R.menu.opt_item_menu_private_mode, menu);
        	// Channels menu item
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
        
     	// Home menu item
        MenuItem menuItem = (MenuItem) menu.findItem(R.id.menu_opt_home);
        menuItem.setIntent(new Intent(this, FeedTabActivity.class));    
        
     	// Preferences menu item
        MenuItem preferencesMenuItem = (MenuItem) menu.findItem(R.id.menu_opt_preferences);
        preferencesMenuItem.setIntent(new Intent(this,FeedPrefActivity.class));
       
        return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
	        case R.id.menu_opt_home:
	        	TrackerAnalyticsHelper.trackEvent(this,LOG_TAG,"OptionMenu_Home","Home",1);
	        	// Kill the FeedTabActivity that started this FeedWebActivity, because tab channel id may have changed and wouldn't be correct (wouldn't be the initial FeedTabActivity channel id) if back button is pressed
	        	setResult(RESULT_OK);
	        	startActivity(item.getIntent());
		    	//finish();
	        	return true;
	        case R.id.menu_opt_channels:
	        	TrackerAnalyticsHelper.trackEvent(this,LOG_TAG,"OptionMenu_Channels","Channels",1);
	        	if (SharedPreferencesHelper.isDynamicMode(this)) {
	        		// Kill the FeedTabActivity that started this FeedWebActivity, because tab channel id may have changed and wouldn't be correct (wouldn't be the initial FeedTabActivity channel id) if back button is pressed
	        		setResult(RESULT_OK);
	        		startActivityForResult(item.getIntent(),KILL_ACTIVITY_CODE);
	        	} else {
	        		//do nothing, default case will be handled
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
	        		// Kill the FeedTabActivity that started this FeedWebActivity, because tab channel id is now changing and won't be correct (won't be the initial FeedChannelActivity channel id) if back button is pressed
	        		setResult(RESULT_OK);
	        		startActivity(item.getIntent());
	        		//finish();
	        		return true;
	        	}
	    }
        return super.onOptionsItemSelected(item);
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
    
    /*
    public void onCreateContextMenu (ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    	super.onCreateContextMenu(menu, v, menuInfo);
    	
		if (v.getId() == R.id.webview) {
			menu.setHeaderTitle (R.string.ctx_menu_title);
			MenuInflater inflater = getMenuInflater();
			
			Item item = mDbFeedAdapter.getItem(mItemId);
			
			if (item != null) {
				long feedId = mDbFeedAdapter.getItemFeedId(mItemId);
				boolean isFirstItem = false;
				boolean isLastItem = false;
				if (mItemId == mDbFeedAdapter.getFirstItem(feedId).getId())
					isFirstItem = true;
				else if (mItemId == mDbFeedAdapter.getLastItem(feedId).getId())
					isLastItem = true;
				
				if (item.isFavorite()) {
					if (isFirstItem)
						inflater.inflate(R.menu.ctx_menu_item_online_notfav_next, menu);
					else if (isLastItem)
						inflater.inflate(R.menu.ctx_menu_item_online_notfav_prev, menu);
					else
						inflater.inflate(R.menu.ctx_menu_item_online_notfav_next_prev, menu);
				} else {
					if (isFirstItem)
						inflater.inflate(R.menu.ctx_menu_item_online_fav_next, menu);
					else if (isLastItem)
						inflater.inflate(R.menu.ctx_menu_item_online_fav_prev, menu);
					else
						inflater.inflate(R.menu.ctx_menu_item_online_fav_next_prev, menu);
				}
			}
		}
    }

    
    public boolean onContextItemSelected(MenuItem menuItem) {
    	Item item = mDbFeedAdapter.getItem(mItemId);
    	ContentValues values = null;
    	Intent intent = null;
    	long feedId = -1;
    	
    	switch (menuItem.getItemId()) {
    		case R.id.add_fav:
	        	TrackerAnalyticsHelper.trackEvent(this,LOG_TAG,"ContextMenu_AddFavorite",item.getLink().toString(),1);
    			//item.favorite();
    			values = new ContentValues();
    	    	values.put(DbSchema.ItemSchema.COLUMN_FAVORITE, DbSchema.ON);
    	    	mDbFeedAdapter.updateItem(mItemId, values, null);
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
									mDbFeedAdapter.updateItem(mItemId, values, null);
									Toast.makeText(FeedWebActivity.this, R.string.remove_fav_msg, Toast.LENGTH_SHORT).show();
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
        	    	mDbFeedAdapter.updateItem(mItemId, values, null);
        			Toast.makeText(this, R.string.remove_fav_msg, Toast.LENGTH_SHORT).show();
    			}
    			return true;
    		case R.id.next:
	        	TrackerAnalyticsHelper.trackEvent(this,LOG_TAG,"ContextMenu_NextItem",item.getLink().toString(),1);
    			feedId = mDbFeedAdapter.getItemFeedId(mItemId);
    			intent = new Intent(this, FeedWebActivity.class);
    	        intent.putExtra(DbSchema.ItemSchema._ID, mDbFeedAdapter.getNextItemId(feedId, mItemId));
    	        startActivity(intent);
    	        finish();
    			return true;
    		case R.id.previous:
	        	TrackerAnalyticsHelper.trackEvent(this,LOG_TAG,"ContextMenu_PreviousItem",item.getLink().toString(),1);
    			feedId = mDbFeedAdapter.getItemFeedId(mItemId);
    			intent = new Intent(this, FeedWebActivity.class);
    	        intent.putExtra(DbSchema.ItemSchema._ID, mDbFeedAdapter.getPreviousItemId(feedId, mItemId));
    	        startActivity(intent);
    	        finish();
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
    */
    
    @Override
    protected Dialog onCreateDialog(int id) {
    	Dialog dialog = null;
    	CharSequence title = null;
    	LayoutInflater inflater = null;
    	View dialogLayout = null;
    	AlertDialog.Builder builder = null;
        switch (id) {
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
        		                finish();
        		           }
        		       });
        		dialog = builder.create();
        		break;
            default:
            	dialog = null;
        }
        return dialog;
    }
}