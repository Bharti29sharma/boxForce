/*
 * Copyright (c) 2011, salesforce.com, inc.
 * All rights reserved.
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 * - Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * - Neither the name of salesforce.com, inc. nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission of salesforce.com, inc.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.metacube.boxforce;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.webkit.CookieSyncManager;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;




import com.metacube.boxforce.R;
import com.salesforce.androidsdk.app.ForceApp;
import com.salesforce.androidsdk.rest.ClientManager;
import com.salesforce.androidsdk.rest.RestRequest;
import com.salesforce.androidsdk.rest.RestResponse;
import com.salesforce.androidsdk.rest.ClientManager.LoginOptions;
import com.salesforce.androidsdk.rest.ClientManager.RestClientCallback;
import com.salesforce.androidsdk.rest.RestClient.AsyncRequestCallback;
import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.security.PasscodeManager;
import com.salesforce.androidsdk.util.EventsObservable;
import com.salesforce.androidsdk.util.EventsObservable.EventType;

/**
 * Main activity
 */
public class MainActivity extends Activity implements OnClickListener {

	private PasscodeManager passcodeManager;
	private String apiVersion;
	private static RestClient client;
	private String[] accountName;
	private String[] accountID;
	private Button attachFileButton; 
	private byte[] byteArray;
	 private static int RESULT_LOAD_IMAGE = 1;
	 private Button attachPDFFileButton; 
	 private String fileName="";
	
	 String encodedImage = "";
	 File fileS;
	 int PICK_REQUEST_CODE = 0;
	 String pathS;
	 ArrayList<String> str = new ArrayList<String>();

		// Check if the first level of the directory structure is the one showing
		private Boolean firstLvl = true;

		private static final String TAG = "F_PATH";

		private Item[] fileList;
		// private File path = new File(Environment.getExternalStorageDirectory() +
		// "");
		private File path = new File("/");
		private String chosenFile;
		private static final int DIALOG_LOAD_FILE = 1000;

		ListAdapter adapter;
		

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
	
		 apiVersion = getString(R.string.api_version);
		
		
    	// Ensure we have a CookieSyncManager
    	CookieSyncManager.createInstance(this);
		
		// Passcode manager
		passcodeManager = ForceApp.APP.getPasscodeManager();		
		
		// Setup view
		setContentView(R.layout.main);
	}
	
	@Override 
	public void onResume() {
		super.onResume();
		
		// Hide everything until we are logged in
		findViewById(R.id.root).setVisibility(View.INVISIBLE);
		
		// Bring up passcode screen if needed
		if (passcodeManager.onResume(this)) {
		
			// Login options
			String accountType = ForceApp.APP.getAccountType();
	    	LoginOptions loginOptions = new LoginOptions(
	    			null, // login host is chosen by user through the server picker 
	    			ForceApp.APP.getPasscodeHash(),
	    			getString(R.string.oauth_callback_url),
	    			getString(R.string.oauth_client_id),
	    			new String[] {"api"});
			
			// Get a rest client
			new ClientManager(this, accountType, loginOptions).getRestClient(this, new RestClientCallback() {
				@Override
				public void authenticatedRestClient(RestClient client) {
					if (client == null) {
						ForceApp.APP.logout(MainActivity.this);
						return;
					}
					MainActivity.this.client = client;
					Constants.client=client;
					getAccount();
					// Show everything
					//findViewById(R.id.root).setVisibility(View.VISIBLE);
					
					/*Intent intent = new Intent(MainActivity.this, Browse.class);
			        //intent.putExtra("API_KEY", Constants.API_KEY); // API_KEY is required
			        startActivity(intent);
			        finish();*/
			        
					findViewById(R.id.root).setVisibility(View.VISIBLE);
					attachFileButton= (Button)findViewById(R.id.evernote_login_button);
					attachFileButton.setOnClickListener(MainActivity.this);
					
					/*attachPDFFileButton= (Button)findViewById(R.id.pdfAttachment);
					attachPDFFileButton.setOnClickListener(MainActivity.this);
					*/
					
	
					// Show welcome
					//((TextView) findViewById(R.id.welcome_text)).setText(getString(R.string.welcome, client.getClientInfo().username));
					
				}
			});
		}
	}

	
	
	
	private void getAccount(){

		try {
			
			String soql = "select id, name from Account";
			RestRequest request = RestRequest.getRequestForQuery(apiVersion, soql);

			client.sendAsync(request, new AsyncRequestCallback() {

				@Override
				public void onSuccess(RestRequest request, RestResponse response) {
					try {
						if (response == null || response.asJSONObject() == null)
							return;
						
						JSONArray records = response.asJSONObject().getJSONArray("records");
	
						if (records.length() == 0)
							return;
										
				    	accountName = new String[records.length()];
				    	accountID = new String[records.length()];
						
					for (int i = 0; i < records.length(); i++){
							JSONObject account = (JSONObject)records.get(i);
							accountName[i] = account.getString("Name");
							accountID[i] = account.getString("Id");
							Log.v("accountName", accountName[i].toString());
							Log.v("accountID", accountID[i].toString());
						}
					
//			        ArrayAdapter<String> ad = new ArrayAdapter<String>(AlbumListActivity.this, 
//				        												   R.layout.list_item, 
//				        												   albums);
//				        setListAdapter(ad);
//						
						
				        EventsObservable.get().notifyEvent(EventType.RenditionComplete);
					} catch (Exception e) {
						e.printStackTrace();
						displayError(e.getMessage());
					}
				}
				
				@Override
				public void onError(Exception exception) {
					displayError(exception.getMessage());
					EventsObservable.get().notifyEvent(EventType.RenditionComplete);
				}
			});
			
		} catch (Exception e) {
			e.printStackTrace();
			displayError(e.getMessage());
		}
	}
	
	private static void displayError(String error)	{
	
	}


	
	
	
	
	@Override
	public void onUserInteraction() {
		passcodeManager.recordUserInteraction();
	}
	
    @Override
    public void onPause() {
    	passcodeManager.onPause(this);
        super.onPause();
    }
	

	/**
	 * Called when "Logout" button is clicked. 
	 * 
	 * @param v
	 */
	public void onLogoutClick(View v) {
		ForceApp.APP.logout(this);
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		if(v==attachFileButton)
		{
		
			
			/*loadFileList();

			showDialog(DIALOG_LOAD_FILE);*/
			
			Intent intent = new Intent(MainActivity.this, Splash.class);
	        //intent.putExtra("API_KEY", Constants.API_KEY); // API_KEY is required
	        startActivity(intent);
	       // finish();
			  
		}
		
		if(v==attachPDFFileButton)
		{
			
			
		}

		
	}
	
	   
 
    
    
  public static  void sendRequest(RestRequest request){

		try {
			

			client.sendAsync(request, new AsyncRequestCallback() {

				@Override
				public void onSuccess(RestRequest request, RestResponse response) {
					try {
						if (response == null || response.asJSONObject() == null)
							return;
						
					
						
				        EventsObservable.get().notifyEvent(EventType.RenditionComplete);
					} catch (Exception e) {
						e.printStackTrace();
						displayError(e.getMessage());
					}
				}
				
				@Override
				public void onError(Exception exception) {
					displayError(exception.getMessage());
					EventsObservable.get().notifyEvent(EventType.RenditionComplete);
				}
			});
			
		} catch (Exception e) {
			e.printStackTrace();
			displayError(e.getMessage());
		}
	}
	
    
    private void loadFileList() {
		try {
			path.mkdirs();
		} catch (SecurityException e) {
			Log.e(TAG, "unable to write on the sd card ");
		}

		// Checks whether path exists
		if (path.exists()) {
			FilenameFilter filter = new FilenameFilter() {
				@Override
				public boolean accept(File dir, String filename) {
					File sel = new File(dir, filename);
					// Filters based on whether the file is hidden or not
					return (sel.isFile() || sel.isDirectory())
							&& !sel.isHidden();

				}
			};

			String[] fList = path.list(filter);
			fileList = new Item[fList.length];
			for (int i = 0; i < fList.length; i++) {
				fileList[i] = new Item(fList[i], R.drawable.file_icon);

				// Convert into file path
				File sel = new File(path, fList[i]);

				// Set drawables
				if (sel.isDirectory()) {
					fileList[i].icon = R.drawable.directory_icon;
					Log.d("DIRECTORY", fileList[i].file);
				} else {
					Log.d("FILE", fileList[i].file);
				}
			}

			if (!firstLvl) {
				Item temp[] = new Item[fileList.length + 1];
				for (int i = 0; i < fileList.length; i++) {
					temp[i + 1] = fileList[i];
				}
				temp[0] = new Item("Up", R.drawable.directory_up);
				fileList = temp;
			}
		} else {
			Log.e(TAG, "path does not exist");
		}

		adapter = new ArrayAdapter<Item>(this,
				android.R.layout.select_dialog_item, android.R.id.text1,
				fileList) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				// creates view
				View view = super.getView(position, convertView, parent);
				TextView textView = (TextView) view
						.findViewById(android.R.id.text1);

				// put the image on the text view
				textView.setCompoundDrawablesWithIntrinsicBounds(
						fileList[position].icon, 0, 0, 0);

				// add margin between image and text (support various screen
				// densities)
				int dp5 = (int) (5 * getResources().getDisplayMetrics().density + 0.5f);
				textView.setCompoundDrawablePadding(dp5);

				return view;
			}
		};

	}

	private class Item {
		public String file;
		public int icon;

		public Item(String file, Integer icon) {
			this.file = file;
			this.icon = icon;
		}

		@Override
		public String toString() {
			return file;
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = null;
		AlertDialog.Builder builder = new Builder(this);

		if (fileList == null) {
			Log.e(TAG, "No files loaded");
			dialog = builder.create();
			return dialog;
		}

		switch (id) {
		case DIALOG_LOAD_FILE:
			builder.setTitle("Choose your file");
			builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					chosenFile = fileList[which].file;
					File sel = new File(path + "/" + chosenFile);
					if (sel.isDirectory()) {

						if (sel.canRead()) {
							firstLvl = false;

							// Adds chosen directory to list
							str.add(chosenFile);
							fileList = null;
							path = new File(sel + "");

							loadFileList();

							removeDialog(DIALOG_LOAD_FILE);
							showDialog(DIALOG_LOAD_FILE);
							Log.d(TAG, path.getAbsolutePath());
						}

						else {
							Toast.makeText(
									MainActivity.this,"[" + sel.getName()+ "] folder can't be read!", 1).show();

						}

					}

					// Checks if 'up' was clicked
					else if (chosenFile.equalsIgnoreCase("up") && !sel.exists()) {

						// present directory removed from list
						String s = str.remove(str.size() - 1);

						// path modified to exclude present directory
						path = new File(path.toString().substring(0,
								path.toString().lastIndexOf(s)));
						fileList = null;

						// if there are no more directories in the list, then
						// its the first level
						if (str.isEmpty()) {
							firstLvl = true;
						}
						loadFileList();

						removeDialog(DIALOG_LOAD_FILE);
						showDialog(DIALOG_LOAD_FILE);
						Log.d(TAG, path.getAbsolutePath());

					}
					// File picked
					else {
						// Perform action with file picked
						if (sel.canRead()) {
						

							//Intent intent = new Intent();
							//intent.setAction(android.content.Intent.ACTION_VIEW);
							 fileS = new File(sel.getPath());
							 fileName = fileS.getName();
							pathS = sel.getPath();
							String mimeType = getMimeType(sel.getPath());

			            	FileInputStream fileInputStream=null;
			            	 
			               
			                byte[] bFile = new byte[(int) fileS.length()];
			         
			                
			                    //convert file into array of bytes
			        	    try {
								fileInputStream = new FileInputStream(fileS);
								fileInputStream.read(bFile);
				        	    fileInputStream.close();
				        	    encodedImage = Base64.encodeToString(bFile, Base64.DEFAULT);
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
			        	    
							
							
							
							
							
							 String objectType = "Attachment";
				             Map<String, Object> fields = new HashMap<String, Object>();             
				             fields.put("ParentID","0019000000F8a9b");
				             fields.put("Body",encodedImage);
				             fields.put("Name", fileName);
				             //fields.put("ContentType", "image/jpeg");
				             fields.put("ContentType", mimeType);
				            
				             
				             
				                
				             try {
				            
				                 //RestRequest req = RestRequest.getRequestForUpsert(apiVersion, objectType, "AccountID", "0019000000F8a9b", fields);
				                 RestRequest req = RestRequest.getRequestForCreate(apiVersion, objectType, fields);
				               // Attachment a = new Attachment (ParentId = caseId, Body = pic, ContentType ="image/jpg",Name = "SendViaMyPhone");

				                 sendRequest(req);
				             
				             } catch (Exception e) {}

							
							
							
							
							
							
							
						}
						
						else {
							//Toast.makeText(M.this,"[" + sel.getName()+ "] file can't be read!", 1).show();
						}

					}

				}
			});
			break;
		}
		dialog = builder.show();
		return dialog;
	}

	public static String getMimeType(String url) {
		String type = null;
		String extension = MimeTypeMap.getFileExtensionFromUrl(url);
		if (extension != null) {
			MimeTypeMap mime = MimeTypeMap.getSingleton();
			type = mime.getMimeTypeFromExtension(extension);
		}
		return type;
	}

    
    
    
    
}
