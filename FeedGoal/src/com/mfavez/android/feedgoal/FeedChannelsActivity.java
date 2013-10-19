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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

import com.google.ads.AdRequest;
import com.google.ads.AdView;
import com.mfavez.android.feedgoal.common.Feed;
import com.mfavez.android.feedgoal.common.TrackerAnalyticsHelper;
import com.mfavez.android.feedgoal.storage.DbFeedAdapter;
import com.mfavez.android.feedgoal.storage.DbSchema;
import com.mfavez.android.feedgoal.storage.SharedPreferencesHelper;

/**
 * Manages a list of feeds (add/remove feed).
 * @author Mathieu Favez
 * Created 09/03/2011
 */

public class FeedChannelsActivity extends Activity implements OnItemClickListener {
	private static final String LOG_TAG = "FeedChannelsActivity";
	private long errorId = 0;
	private DbFeedAdapter mDbFeedAdapter;
	private long mChannelToRemove = -1;
	boolean mMustParseFeed = false;
	URL mUrl = null; 
	Uri mUri = null;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        
        mDbFeedAdapter = new DbFeedAdapter(this);
        mDbFeedAdapter.open();
        
        TrackerAnalyticsHelper.createTracker(this);
        
        setContentView(R.layout.channels);
        
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
        AdRequest request = new AdRequest();
        request.addTestDevice(AdRequest.TEST_EMULATOR);
        
        ListView channelListView = (ListView)findViewById(R.id.channel_list);	
        registerForContextMenu(channelListView);
        channelListView.setOnItemClickListener(this);
        
        Button addChannelButton = (Button)findViewById(R.id.button_add_channel);
        addChannelButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
            	showDialog(SharedPreferencesHelper.DIALOG_ADD_CHANNEL);
            }
        });
        
        // Gets the action and data that triggered the intent filter for this Activity
        final Intent intent = getIntent();
        final String action = intent.getAction();
        if (Intent.ACTION_VIEW.equals(action)) {
    		mUri = intent.getData();
        	showDialog(SharedPreferencesHelper.DIALOG_ADD_CHANNEL);
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
    	fillData();
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
    
    private void fillData() {
		TrackerAnalyticsHelper.trackPageView(this,"/channelListView");
		
		getWindow().setTitle(getString(R.string.app_name) + " - " + getString(R.string.menu_channels));
		
		ListView channelListView = (ListView)findViewById(R.id.channel_list);
		List<Feed> feeds = mDbFeedAdapter.getFeeds();
		ChannelsArrayAdapter channelsAdapter = new ChannelsArrayAdapter(this, R.id.title, feeds);
		channelListView.setAdapter(channelsAdapter);
		
		channelListView.setSelection(0);
    }
    
    private void removeChannel(long feedId) {
    	if (mDbFeedAdapter.getFeeds().size() <= 1) {
    		showDialog(SharedPreferencesHelper.DIALOG_MIN_CHANNEL_REQUIRED);
    	} else {
    		mChannelToRemove = feedId;
    		TrackerAnalyticsHelper.trackEvent(this,LOG_TAG,"Remove_Channel",mDbFeedAdapter.getFeed(feedId).getURL().toString(),1);
    		showDialog(SharedPreferencesHelper.DIALOG_REMOVE_CHANNEL);
    	}
    }
    
    public void onItemClick (AdapterView<?> parent, View v, int position, long id) {
    	Feed feed = mDbFeedAdapter.getFeed(id);
    	TrackerAnalyticsHelper.trackEvent(this,LOG_TAG,"Select_Channel",feed.getURL().toString(),1);
    	Intent intent = new Intent(this, FeedTabActivity.class);
        intent.putExtra(DbSchema.FeedSchema._ID, id);
        // Kill the FeedTabActivity that started this FeedChannelsActivity, because tab channel id is now changing and won't be correct (won't be the initial FeedChannelActivity channel id) if back button is pressed
        setResult(RESULT_OK);
        startActivity(intent);
	}
    
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.opt_channel_menu_public_mode, menu);
               
        // Preferences menu item
        MenuItem preferencesMenuItem = (MenuItem) menu.findItem(R.id.menu_opt_preferences);
        preferencesMenuItem.setIntent(new Intent(this,FeedPrefActivity.class));
        
        return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
	        case R.id.menu_opt_preferences:
	        	TrackerAnalyticsHelper.trackEvent(this,LOG_TAG,"OptionMenu_Preferences","Preferences",1);
	        	startActivity(item.getIntent());
	            return true;
	        case R.id.menu_opt_about:
	        	TrackerAnalyticsHelper.trackEvent(this,LOG_TAG,"OptionMenu_AboutDialog","About",1);
	        	showDialog(SharedPreferencesHelper.DIALOG_ABOUT);
	            return true;
	        default:
	        	return super.onOptionsItemSelected(item);
	    }
    }
    
    public void onCreateContextMenu (ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    	super.onCreateContextMenu(menu, v, menuInfo);
    	
		if (v.getId() == R.id.channel_list) {
			menu.setHeaderTitle (R.string.ctx_menu_title);
			MenuInflater inflater = getMenuInflater();
			AdapterView.AdapterContextMenuInfo acmi = (AdapterView.AdapterContextMenuInfo) menuInfo;
			Feed feed = mDbFeedAdapter.getFeed(acmi.id);
			if (feed != null) {
		    	inflater.inflate(R.menu.ctx_menu_channel, menu);
			}
		}
    }
   
    public boolean onContextItemSelected(MenuItem menuItem) {
    	final AdapterContextMenuInfo acmi = (AdapterContextMenuInfo) menuItem.getMenuInfo();
    	Feed feed = mDbFeedAdapter.getFeed(acmi.id);
    	switch (menuItem.getItemId()) {
    		case R.id.remove_channel:
	        	removeChannel(feed.getId());
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
        		View dialogLayoutAbout = inflater.inflate(R.layout.dialog_about, null);
        		TextView childView = null;
        		if (getString(R.string.website).equals("")) {
        			childView = (TextView) dialogLayoutAbout.findViewById(R.id.website);
        			childView.setVisibility(View.GONE);
        		}
        		if (getString(R.string.email).equals("")) {
        			childView = (TextView) dialogLayoutAbout.findViewById(R.id.email);
        			childView.setVisibility(View.GONE);
        		}
        		if (getString(R.string.contact).equals("")) {
        			childView = (TextView) dialogLayoutAbout.findViewById(R.id.contact);
        			childView.setVisibility(View.GONE);
        		}
        		if (getString(R.string.powered).equals("")) {
        			childView = (TextView) dialogLayoutAbout.findViewById(R.id.powered);
        			childView.setVisibility(View.GONE);
        		}
        		builder = new AlertDialog.Builder(this);
        		builder.setView(dialogLayoutAbout)
        			   .setTitle(title)
        			   .setIcon(R.drawable.ic_dialog)
        			   .setNeutralButton(R.string.close, new DialogInterface.OnClickListener() {
        		           public void onClick(DialogInterface dialog, int id) {
        		                dialog.cancel();
        		           }
        		       });
        		dialog = builder.create();
	        	break;
        	case SharedPreferencesHelper.DIALOG_REMOVE_CHANNEL:
        		builder = new AlertDialog.Builder(this);
    			builder.setMessage(R.string.channel_remove_confirmation)
    			       .setCancelable(false)
    			       .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
    			           public void onClick(DialogInterface dialog, int id) {
    			        	   if (mChannelToRemove != -1) {
    			        		   	Feed feed = mDbFeedAdapter.getFeed(mChannelToRemove);
    			        		   	if (mDbFeedAdapter.removeFeed(feed.getId())) {
    			        		   		TrackerAnalyticsHelper.trackEvent(FeedChannelsActivity.this,LOG_TAG,"Remove_Channel",feed.getURL().toString(),1);
    			        		   		// Kill the FeedTabActivity, FeedItemActivity or FeedWebActivity that started this FeedChannelActivity, because corresponding channel has been removed and may be inconsistent if back button is pressed
    			        		   		setResult(RESULT_OK);
    			        		   		long updatedFeedId = mDbFeedAdapter.getFirstFeed().getId();
    			        		   		SharedPreferencesHelper.setPrefTabFeedId(FeedChannelsActivity.this, updatedFeedId);
    			        		   		long currentPrefStartChannel = SharedPreferencesHelper.getPrefStartChannel(FeedChannelsActivity.this);
    			        		   		if (mDbFeedAdapter.getFeed(currentPrefStartChannel) == null)
    			        		   			SharedPreferencesHelper.setPrefStartChannel(FeedChannelsActivity.this,updatedFeedId);
    			        		   		fillData();
    	    			        		String feedTitle = String.format(getString(R.string.channel_removed), feed.getTitle());
    	    			        		Toast.makeText(FeedChannelsActivity.this, feedTitle, Toast.LENGTH_SHORT).show();
    			        		   	} else {
    			        		   		fillData();
           			        			String error = getString(R.string.error) + " - " + String.format(getString(R.string.channel_not_removed), feed.getTitle());
           			        			Toast.makeText(FeedChannelsActivity.this, error, Toast.LENGTH_LONG).show();
    			        		   	}
    			        		   	mChannelToRemove = -1;
    			        	   } else {
    			        		   	fillData();
       			        			String error = getString(R.string.error) + " - " + String.format(getString(R.string.channel_not_removed), "");
       			        			Toast.makeText(FeedChannelsActivity.this, error, Toast.LENGTH_LONG).show();
    			        	   }
    			           }
    			       })
    			       .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
    			           public void onClick(DialogInterface dialog, int id) {
    			        	   mChannelToRemove = -1; 
    			        	   dialog.cancel();
    			           }
    			       });
    			dialog = builder.create();
        		break;
        	case SharedPreferencesHelper.DIALOG_MIN_CHANNEL_REQUIRED:
        		builder = new AlertDialog.Builder(this);
        		builder.setMessage(R.string.channel_remove_min_required)
        			   .setTitle(R.string.warning)
        			   .setIcon(R.drawable.ic_dialog)
        			   .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
		 		           public void onClick(DialogInterface dialog, int id) {
		 		                dialog.cancel();
		 		           }
 		        });
        		dialog = builder.create();
        		break;
        	case SharedPreferencesHelper.DIALOG_ADD_CHANNEL:
        		builder = new AlertDialog.Builder(this);
        		inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
            	final View dialogLayoutAddChannel = inflater.inflate(R.layout.dialog_add_channel, null);
            	// If intent filter has been triggered
            	if (mUri != null) {
            		EditText inputText = (EditText) dialogLayoutAddChannel.findViewById(R.id.dialog_input);
            		inputText.setText(mUri.toString());
            	}
        		builder.setView(dialogLayoutAddChannel)
        			   .setCancelable(false)
        			   .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface dialog, int id) {
				        	   mMustParseFeed = false;
				        	   dialog.cancel();
				           }
				       })
		        	   .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
		 		           public void onClick(DialogInterface dialog, int id) {
		 		        	  EditText inputText = (EditText) dialogLayoutAddChannel.findViewById(R.id.dialog_input);
		 		        	  String inputURL = inputText.getText().toString();
		 		        	  mMustParseFeed = true;
		 		        	  mUrl = null; 
		 		        	  try {
		 		        		 mUrl = new URL(inputURL);
		 		        	  } catch (MalformedURLException mue) {
		 		        		 mMustParseFeed = false;
		 		        		 showDialog(SharedPreferencesHelper.DIALOG_ADD_CHANNEL_ERROR_URL);
		 		        	  }
		 		        	  if (!SharedPreferencesHelper.isOnline(FeedChannelsActivity.this)) {
		 		        		 mMustParseFeed = false;
		 	            		 showDialog(SharedPreferencesHelper.DIALOG_NO_CONNECTION);
		 		        	  }
		 		        	  // Dialog is automatically dismissed, call to dimiss() method is not necessary
			 		          // http://stackoverflow.com/questions/2620444/android-how-to-prevent-dialog-closed-or-remain-dialog-when-button-is-clicked
		 		        	  dismissDialog(SharedPreferencesHelper.DIALOG_ADD_CHANNEL);
		 		           }
		 		       });
        		dialog = builder.create();
        		dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
        			public void onDismiss(DialogInterface dialog) {
        				if (mMustParseFeed) {
        					//showDialog(SharedPreferencesHelper.DIALOG_UPDATE_PROGRESS);
        					ProgressDialog progresssDialog = ProgressDialog.show(FeedChannelsActivity.this, getResources().getText(R.string.updating), getResources().getText(R.string.downloading), true, false);
        					progresssDialog.setIcon(R.drawable.ic_dialog);
        					UpdateThread updateThread = new UpdateThread(progresssDialog,mUrl);
        		            updateThread.start();
        				}
        	        }
        		});
        		break;
        	case SharedPreferencesHelper.DIALOG_ADD_CHANNEL_ERROR_URL:
        		builder = new AlertDialog.Builder(this);
        		builder.setMessage(R.string.channel_add_dialog_error_url)
        			   .setTitle(R.string.error)
        			   .setIcon(R.drawable.ic_dialog)
        			   .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
		 		           public void onClick(DialogInterface dialog, int id) {
		 		                dialog.cancel();
		 		           }
 		        });
        		dialog = builder.create();
        		break;
        	case SharedPreferencesHelper.DIALOG_ADD_CHANNEL_ERROR_PARSING:
        		builder = new AlertDialog.Builder(this);
        		builder.setMessage(R.string.channel_add_dialog_error_parsing)
        			   .setTitle(R.string.error)
        			   .setIcon(R.drawable.ic_dialog)
        			   .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
		 		           public void onClick(DialogInterface dialog, int id) {
		 		                dialog.cancel();
		 		           }
 		        });
        		dialog = builder.create();
        		break;
        	/*
        	// Bug when showing dialog this way - showDialog(SharedPreferencesHelper.DIALOG_UPDATE_PROGRESS):
        	// when new feed is added a second time, dialog stays blocked and does not release !!???
        	case SharedPreferencesHelper.DIALOG_UPDATE_PROGRESS:
        		dialog = new ProgressDialog(this);
	            ((ProgressDialog)dialog).setTitle(getResources().getText(R.string.updating));
	            ((ProgressDialog)dialog).setMessage(getResources().getText(R.string.downloading));
	            ((ProgressDialog)dialog).setIndeterminate(true);
	            dialog.setCancelable(false);
	            UpdateThread updateThread = new UpdateThread((ProgressDialog)dialog,mUrl);
	            updateThread.start();
        		break;
        	*/
        	case SharedPreferencesHelper.DIALOG_NO_CONNECTION:
        		title = getString(R.string.error);
        		inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        		final View dialogLayout = inflater.inflate(R.layout.dialog_no_connection, null);
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
        	default:
            	dialog = null;
        }
        return dialog;
    }
    
    private class UpdateThread extends Thread {

        private URL url;
        private ProgressDialog pd;

        public UpdateThread(ProgressDialog pd, URL url) {
            this.pd = pd;
        	this.url = url;
        }

        @Override
        public void run() {
        	try {
	        	FeedHandler feedHandler = new FeedHandler(FeedChannelsActivity.this);
	    		Feed handledFeed = feedHandler.handleFeed(url);
	    		mDbFeedAdapter.addFeed(handledFeed);
	    		TrackerAnalyticsHelper.trackEvent(FeedChannelsActivity.this,LOG_TAG,"Add_Channel",handledFeed.getURL().toString(),1);
	            handler.sendEmptyMessage(0);
        	} catch (Exception e) {
        		Log.e(LOG_TAG,"",e);
        		errorId = errorId + 1;
        		TrackerAnalyticsHelper.trackError(FeedChannelsActivity.this, Long.toString(errorId), e.getMessage(), LOG_TAG);
        		handler.sendEmptyMessage(1);
        	}
        }

        private Handler handler = new Handler() {

            public void handleMessage(Message msg) {
            	fillData();
                pd.dismiss();
            	if (msg.what == 1)
            		showDialog(SharedPreferencesHelper.DIALOG_ADD_CHANNEL_ERROR_PARSING);
            }
        };
    }
    
    private static class ChannelsArrayAdapter extends ArrayAdapter<Feed> {
    	private LayoutInflater mInflater;
    	
    	public ChannelsArrayAdapter(Context context, int textViewResourceId, List<Feed> objects) {
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
        	// A ViewHolder keeps references to children views to avoid unneccessary calls to findViewById() on each row.
            ViewHolder holder;
    
        	// When convertView is not null, we can reuse it directly, there is no need
            // to reinflate it. We only inflate a new View when the convertView supplied is null.
            if (convertView == null) {
            	convertView = mInflater.inflate(R.layout.channel, null);
	            
	            // Creates a ViewHolder and store references to the children views we want to bind data to.
                holder = new ViewHolder();
                holder.title = (TextView) convertView.findViewById(R.id.title);
                holder.button = (Button) convertView.findViewById(R.id.button_delete_channel);

                convertView.setTag(holder);
            } else {
                // Get the ViewHolder back to get fast access to the TextView and Button.
                holder = (ViewHolder) convertView.getTag();
            }
          
            Feed feed = getItem(position);
            
            // Bind the data efficiently with the holder.
            holder.title.setText(feed.getTitle());
            holder.button.setTag(feed.getId());
            holder.button.setOnClickListener(new OnClickListener() {
                public void onClick(View view) {
                	((FeedChannelsActivity)getContext()).removeChannel((Long) view.getTag());
                }
            });
            
            return convertView;
        }
        
        static class ViewHolder {
            TextView title;
            Button button;
        }
    }
}
