package com.example.simplelists;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.NoSuchPaddingException;

import com.microsoft.adal.AuthenticationCallback;
import com.microsoft.adal.AuthenticationContext;
import com.microsoft.adal.AuthenticationResult;
import com.example.simplelists.Constants;
import com.example.simplelists.R;
import com.microsoft.office365.Action;
import com.microsoft.office365.http.OAuthCredentials;
import com.microsoft.office365.lists.SPListItem;
import com.microsoft.office365.lists.SharepointListsClient;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends Activity {
	private static final String TAG = "MainActivity";
	
	private static final int MENU_RESET_TOKEN = 0;
	private static final int MENU_SHOW_TOKEN = 1;
	
	private AuthenticationContext mAuthContext;
	private OAuthCredentials credentials;
	
	Button btnGetListItems;
	TextView txtOutput;
	TextView txtListTitle;
	ListView lvOutput;
	Button btnGetAccessToken;
	ArrayList<String> listItems;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		txtListTitle = (TextView) findViewById(R.id.editText_ListTitle);
		btnGetAccessToken = (Button) findViewById(R.id.button_getAccessToken);
		lvOutput = (ListView) findViewById(R.id.listView_Output);
		btnGetListItems = (Button) findViewById(R.id.button_getListItems);
		txtOutput = (TextView) findViewById(R.id.textView_Output);
		
		btnGetAccessToken.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				getToken();
				
			}
		});
		
		btnGetListItems.setEnabled(false);
		btnGetListItems.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				String listTitle = txtListTitle.getText().toString();
				if (listTitle == null || listTitle.isEmpty()) {
					txtOutput.setText("Please enter a list title");
					return;
				} else {
					getListItems(listTitle);
				}				
			}
		});
		
	}

	
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
        mAuthContext.onActivityResult(requestCode, resultCode, data);
    }
    
    protected AuthenticationContext getAuthContext(){
    	if (mAuthContext == null) {
    		try {
    			mAuthContext = new AuthenticationContext(this, Constants.AUTHORITY_URL, false);
    		} catch (NoSuchAlgorithmException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		} catch (NoSuchPaddingException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
    	}
    	return mAuthContext;
    }
    
    protected void getToken() {
		getAuthContext().acquireToken(this, Constants.RESOURCE_ID,
				Constants.CLIENT_ID, Constants.REDIRECT_URL, Constants.USER_HINT,
				new AuthenticationCallback<AuthenticationResult>() {

					@Override
					public void onError(Exception exc) {
					}

					@Override
					public void onSuccess(AuthenticationResult result) {
                        if (result != null && !result.getAccessToken().isEmpty()) {
                        	credentials = new OAuthCredentials(result.getAccessToken());
                        	txtOutput.setText("Got token!");
                        	Log.i(TAG,credentials.toString());
                        	btnGetListItems.setEnabled(true);
                        }
					}
				});
    }
    
    
    private void getListItems(String listTitle) {
    	
    	txtOutput.setText("Getting items...");
    	// construct a SP Lists client with previously set credentials
    	SharepointListsClient client = new SharepointListsClient(Constants.RESOURCE_ID, "/", credentials);
    	
    	// asynchronous path, takes advantage of futures
    	// call getListItems, which returns a OfficeFuture, then hook up to its ".done()" method
    	client.getListItems(listTitle, null).done(new Action<List<SPListItem>>() {

			@Override
			public void run(List<SPListItem> items) throws Exception {
				// build up a list of list item titles
				final ArrayList<String> itemTitles = new ArrayList<String>();
				for (SPListItem item : items) {
					Log.i(TAG, item.toString());
					itemTitles.add(item.getTitle());
				}
				
				// we're not on the UI thread right now, so call back 
				// to the UI thread to update the ListView and set text
				MainActivity.this.runOnUiThread(new Runnable() {

					@Override
					public void run() {
						// TODO Auto-generated method stub
						ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this,
								android.R.layout.simple_list_item_1,
								itemTitles);
						lvOutput.setAdapter(adapter);
						txtOutput.setText("Done!");
					}
				});
			}
    	});
    }
    
    
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		
        menu.add(Menu.NONE, MENU_RESET_TOKEN, Menu.NONE, "Clear Token Cache");
        menu.add(Menu.NONE, MENU_SHOW_TOKEN, Menu.NONE, "Show Token");
		return true;
	}
	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        	case MENU_RESET_TOKEN: {
        		resetToken();
        		clearCookies();
        		return true;
        	}
        	case MENU_SHOW_TOKEN: {
        		if (credentials != null && !credentials.toString().isEmpty()){
        			txtOutput.setText(credentials.getToken());
        			return true;
        		}
        	}
	    default: return super.onOptionsItemSelected(item);
        }
    }
    
	public void resetToken(){
		Log.i(TAG, "Clearing cached tokens");
		getAuthContext().getCache().removeAll();
		txtOutput.setText("");
		btnGetListItems.setEnabled(false);
	}
	
    public void clearCookies() {
        CookieSyncManager.createInstance(getApplicationContext());
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();
        CookieSyncManager.getInstance().sync();
    }

}
