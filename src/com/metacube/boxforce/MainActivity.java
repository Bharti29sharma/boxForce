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
import java.io.IOException;
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
import android.content.SharedPreferences;
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

import com.box.androidlib.Box;
import com.box.androidlib.DAO.User;
import com.box.androidlib.ResponseListeners.GetAccountInfoListener;
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
	private TextView statusText;
	private Button homeButton;
	private Button authenticateButton;

	String encodedImage = "";
	File fileS;
	int PICK_REQUEST_CODE = 0;
	String pathS;
	ArrayList<String> str = new ArrayList<String>();

	ListAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getString(R.string.api_version);

		// Ensure we have a CookieSyncManager
		CookieSyncManager.createInstance(this);

		// Passcode manager
		passcodeManager = ForceApp.APP.getPasscodeManager();

		// Setup view
		

	}

	@Override
	public void onResume() {
		super.onResume();

		// Hide everything until we are logged in
		// findViewById(R.id.root).setVisibility(View.INVISIBLE);

		// Bring up passcode screen if needed
		if (passcodeManager.onResume(this)) {

			// Login options
			String accountType = ForceApp.APP.getAccountType();
			LoginOptions loginOptions = new LoginOptions(
					null, // login host is chosen by user through the server
							// picker
					ForceApp.APP.getPasscodeHash(),
					getString(R.string.oauth_callback_url),
					getString(R.string.oauth_client_id), new String[] { "api" });

			// Get a rest client
			new ClientManager(this, accountType, loginOptions).getRestClient(
					this, new RestClientCallback() {
						@Override
						public void authenticatedRestClient(RestClient client) {
							if (client == null) {
								ForceApp.APP.logout(MainActivity.this);
								return;
							}
							Constants.client = client;

							
							setContentView(R.layout.splash);
							statusText = (TextView) findViewById(R.id.statusText);
							homeButton = (Button) findViewById(R.id.homeButton);
							authenticateButton = (Button) findViewById(R.id.authenticateButton);
							homeButton.setOnClickListener(MainActivity.this);
							authenticateButton.setOnClickListener(MainActivity.this);
							
							BoxAuthenticationFunctionality();
							
						}
					});
		}
	}

	/*
	 * private void getAccount(){
	 * 
	 * try {
	 * 
	 * String soql = "select id, name from Account"; RestRequest request =
	 * RestRequest.getRequestForQuery(apiVersion, soql);
	 * 
	 * client.sendAsync(request, new AsyncRequestCallback() {
	 * 
	 * @Override public void onSuccess(RestRequest request, RestResponse
	 * response) { try { if (response == null || response.asJSONObject() ==
	 * null) return;
	 * 
	 * JSONArray records = response.asJSONObject().getJSONArray("records");
	 * 
	 * if (records.length() == 0) return;
	 * 
	 * accountName = new String[records.length()]; accountID = new
	 * String[records.length()];
	 * 
	 * for (int i = 0; i < records.length(); i++){ JSONObject account =
	 * (JSONObject)records.get(i); accountName[i] = account.getString("Name");
	 * accountID[i] = account.getString("Id"); Log.v("accountName",
	 * accountName[i].toString()); Log.v("accountID", accountID[i].toString());
	 * }
	 * 
	 * // ArrayAdapter<String> ad = new
	 * ArrayAdapter<String>(AlbumListActivity.this, // R.layout.list_item, //
	 * albums); // setListAdapter(ad); //
	 * 
	 * EventsObservable.get().notifyEvent(EventType.RenditionComplete); } catch
	 * (Exception e) { e.printStackTrace(); displayError(e.getMessage()); } }
	 * 
	 * @Override public void onError(Exception exception) {
	 * displayError(exception.getMessage());
	 * EventsObservable.get().notifyEvent(EventType.RenditionComplete); } });
	 * 
	 * } catch (Exception e) { e.printStackTrace();
	 * displayError(e.getMessage()); } }
	 */
	/*
	 * private static void displayError(String error) {
	 * 
	 * }
	 */

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
		if (v == authenticateButton) {

			Intent intent = new Intent(MainActivity.this, Authentication.class);
			startActivity(intent);

		}
		if (v == homeButton) {
			Intent intent = new Intent(MainActivity.this, Browse.class);
			startActivity(intent);
			finish();
		}

	}

	/*
	 * public static void sendRequest(RestRequest request){
	 * 
	 * try {
	 * 
	 * 
	 * client.sendAsync(request, new AsyncRequestCallback() {
	 * 
	 * @Override public void onSuccess(RestRequest request, RestResponse
	 * response) { try { if (response == null || response.asJSONObject() ==
	 * null) return;
	 * 
	 * 
	 * 
	 * EventsObservable.get().notifyEvent(EventType.RenditionComplete); } catch
	 * (Exception e) { e.printStackTrace(); displayError(e.getMessage()); } }
	 * 
	 * @Override public void onError(Exception exception) {
	 * displayError(exception.getMessage());
	 * EventsObservable.get().notifyEvent(EventType.RenditionComplete); } });
	 * 
	 * } catch (Exception e) { e.printStackTrace();
	 * displayError(e.getMessage()); } }
	 */

	/*
	 * private void loadFileList() { try { path.mkdirs(); } catch
	 * (SecurityException e) { Log.e(TAG, "unable to write on the sd card "); }
	 * 
	 * // Checks whether path exists if (path.exists()) { FilenameFilter filter
	 * = new FilenameFilter() {
	 * 
	 * @Override public boolean accept(File dir, String filename) { File sel =
	 * new File(dir, filename); // Filters based on whether the file is hidden
	 * or not return (sel.isFile() || sel.isDirectory()) && !sel.isHidden();
	 * 
	 * } };
	 * 
	 * String[] fList = path.list(filter); fileList = new Item[fList.length];
	 * for (int i = 0; i < fList.length; i++) { fileList[i] = new Item(fList[i],
	 * R.drawable.file_icon);
	 * 
	 * // Convert into file path File sel = new File(path, fList[i]);
	 * 
	 * // Set drawables if (sel.isDirectory()) { fileList[i].icon =
	 * R.drawable.directory_icon; Log.d("DIRECTORY", fileList[i].file); } else {
	 * Log.d("FILE", fileList[i].file); } }
	 * 
	 * if (!firstLvl) { Item temp[] = new Item[fileList.length + 1]; for (int i
	 * = 0; i < fileList.length; i++) { temp[i + 1] = fileList[i]; } temp[0] =
	 * new Item("Up", R.drawable.directory_up); fileList = temp; } } else {
	 * Log.e(TAG, "path does not exist"); }
	 * 
	 * adapter = new ArrayAdapter<Item>(this,
	 * android.R.layout.select_dialog_item, android.R.id.text1, fileList) {
	 * 
	 * @Override public View getView(int position, View convertView, ViewGroup
	 * parent) { // creates view View view = super.getView(position,
	 * convertView, parent); TextView textView = (TextView) view
	 * .findViewById(android.R.id.text1);
	 * 
	 * // put the image on the text view
	 * textView.setCompoundDrawablesWithIntrinsicBounds(
	 * fileList[position].icon, 0, 0, 0);
	 * 
	 * // add margin between image and text (support various screen //
	 * densities) int dp5 = (int) (5 *
	 * getResources().getDisplayMetrics().density + 0.5f);
	 * textView.setCompoundDrawablePadding(dp5);
	 * 
	 * return view; } };
	 * 
	 * }
	 */
	private class Item {
		public String file;

		public Item(String file, Integer icon) {
			this.file = file;
		}

		@Override
		public String toString() {
			return file;
		}
	}

	private void BoxAuthenticationFunctionality() {
		if (Constants.API_KEY == null) {
			Toast.makeText(
					getApplicationContext(),
					"You must set your API key into Constants.java before you can use this demo app. Register at https://www.box.net/developers.",
					Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		statusText.setText(getResources().getString(
				R.string.checking_login_status));
		homeButton.setVisibility(View.GONE);
		authenticateButton.setVisibility(View.GONE);

		final SharedPreferences prefs = getSharedPreferences(
				Constants.PREFS_FILE_NAME, 0);
		final String authToken = prefs.getString(
				Constants.PREFS_KEY_AUTH_TOKEN, null);
		if (authToken == null) {
			onNotLoggedIn();

		} else {
			// We have an auth token. Let's execute getAccountInfo() and put the
			// user's e-mail address up on the screen.
			// This request will also serve as a way for us to verify that the
			// auth token is actually still valid.
			final Box box = Box.getInstance(Constants.API_KEY);
			box.getAccountInfo(authToken, new GetAccountInfoListener() {
				@Override
				public void onComplete(final User boxUser, final String status) {
					// see
					// http://developers.box.net/w/page/12923928/ApiFunction_get_account_info
					// for possible status codes
					if (status
							.equals(GetAccountInfoListener.STATUS_GET_ACCOUNT_INFO_OK)
							&& boxUser != null) {
						statusText.setText("Logged in as\n"
								+ boxUser.getEmail());
						homeButton.setVisibility(View.VISIBLE);
						authenticateButton
								.setText("Log in as a different user");
						authenticateButton.setVisibility(View.VISIBLE);
					} else {
						// Could not get user info. It's possible the auth token
						// was no longer valid. Check the status code that was
						// returned.
						onNotLoggedIn();
					}
				}

				@Override
				public void onIOException(IOException e) {
					// No network connection?
					e.printStackTrace();
					onNotLoggedIn();
				}
			});
		}

	}

	private void onNotLoggedIn() {
		statusText.setText("You are not logged in");
		homeButton.setVisibility(View.GONE);
		authenticateButton.setText("Log in");
		authenticateButton.setVisibility(View.VISIBLE);
	}

}
