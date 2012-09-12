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
import java.io.IOException;
import java.util.ArrayList;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.CookieSyncManager;
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
import com.salesforce.androidsdk.rest.ClientManager.LoginOptions;
import com.salesforce.androidsdk.rest.ClientManager.RestClientCallback;
import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.security.PasscodeManager;

/**
 * Main activity
 */
public class MainActivity extends Activity implements OnClickListener {

	private PasscodeManager passcodeManager;
	private Button loginButton;

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

		
		CookieSyncManager.createInstance(this);

		
		passcodeManager = ForceApp.APP.getPasscodeManager();

		// Setup view

	}

	@Override
	public void onResume() {
		super.onResume();

		
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

						

							BoxAuthenticationFunctionality();

						}
					});
		}
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
		if (v == loginButton) {

			Intent intent = new Intent(MainActivity.this, Authentication.class);
			startActivity(intent);
			//finish();

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
						
						Intent intent = new Intent(MainActivity.this, Browse.class);
						startActivity(intent);
						finish();
						
						
						
					} else {
						
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
		 setContentView(R.layout.splash);
		 loginButton = (Button) findViewById(R.id.loginButton);
		 loginButton.setOnClickListener(MainActivity.this);
		
		
	}

}
