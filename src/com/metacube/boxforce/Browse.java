// Copyright 2011 Box.net.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package com.metacube.boxforce;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.box.androidlib.Box;
import com.box.androidlib.DAO.BoxFile;
import com.box.androidlib.DAO.BoxFolder;
import com.box.androidlib.ResponseListeners.FileDownloadListener;
import com.box.androidlib.ResponseListeners.GetAccountTreeListener;
import com.metacube.boxforce.R;
import com.salesforce.androidsdk.rest.RestRequest;

public class Browse extends Activity implements OnClickListener,
		OnItemClickListener {

	private MyArrayAdapter adapter;
	private TreeListItem[] items;
	private String authToken;
	private long folderId;
	private String apiVersion;
	String boxFileName;
	String boxFilePath;
	String encodedImage;
	String mimeType;
	Button saveToSF,okButton;
	ListView lv;
	public static ArrayList<File> fList;
	TemplateApp fileAttch;
	// Menu button options
	/*
	 * private static final int MENU_ID_UPLOAD = 1; private static final int
	 * MENU_ID_CREATE_FOLDER = 2;
	 */

	ArrayList<String> encodeImgList;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tree);
		
		apiVersion = getString(R.string.api_version);
		fileAttch = ((TemplateApp) getApplicationContext());
		fList = new ArrayList<File>();

		// BOX --------------------

		// Check if we have an Auth Token stored.
		final SharedPreferences prefs = getSharedPreferences(
				Constants.PREFS_FILE_NAME, 0);
		authToken = prefs.getString(Constants.PREFS_KEY_AUTH_TOKEN, null);
		if (authToken == null) {
			Toast.makeText(getApplicationContext(), "You are not logged in.",
					Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

		// View your root folder by default (folder_id = 0l), or this activity
		// can also be launched to view subfolders
		folderId = 0l;
		Bundle extras = getIntent().getExtras();
		if (extras != null && extras.containsKey("folder_id")) {
			folderId = extras.getLong("folder_id");
		}

		// Initialize list items and set adapter
		items = new TreeListItem[0];
		lv = (ListView) findViewById(R.id.list);

		
		  View footer = (View) getLayoutInflater().inflate(
		  R.layout.footer, null);		  
		  lv.addHeaderView(footer);
		  
		  saveToSF = (Button) findViewById(R.id.saveToSF_button);
		 
		  saveToSF.setOnClickListener(this);

	//	adapter = new MyArrayAdapter(this, R.layout.list_item, items);
		  adapter = new MyArrayAdapter(this, items);
		lv.setOnItemClickListener(this);
		lv.setAdapter(adapter);

		refresh();

	}

	/**
	 * Refresh the tree.
	 */
	private void refresh() {
		final Box box = Box.getInstance(Constants.API_KEY);
		box.getAccountTree(authToken, folderId,
				new String[] { Box.PARAM_ONELEVEL },
				new GetAccountTreeListener() {

					@Override
					public void onComplete(BoxFolder boxFolder, String status) {
						if (!status
								.equals(GetAccountTreeListener.STATUS_LISTING_OK)) {
							Toast.makeText(getApplicationContext(),
									"There was an error.", Toast.LENGTH_SHORT)
									.show();
							finish();
							return;
						}

						/**
						 * Box.getAccountTree() was successful. boxFolder
						 * contains a list of subfolders and files. Shove those
						 * into an array so that our list adapter displays them.
						 */

						// items = new
						// TreeListItem[boxFolder.getFoldersInFolder().size() +
						// boxFolder.getFilesInFolder().size()];
						items = new TreeListItem[boxFolder.getFilesInFolder()
								.size()];

						int i = 0;

						/*
						 * Iterator<? extends BoxFolder> foldersIterator =
						 * boxFolder.getFoldersInFolder().iterator(); while
						 * (foldersIterator.hasNext()) { BoxFolder subfolder =
						 * foldersIterator.next(); TreeListItem item = new
						 * TreeListItem(); item.id = subfolder.getId();
						 * item.name = subfolder.getFolderName(); item.type =
						 * TreeListItem.TYPE_FOLDER; item.folder = subfolder;
						 * item.updated = subfolder.getUpdated(); items[i] =
						 * item; i++; }
						 */
						Iterator<? extends BoxFile> filesIterator = boxFolder
								.getFilesInFolder().iterator();
						while (filesIterator.hasNext()) {
							BoxFile boxFile = filesIterator.next();
							TreeListItem item = new TreeListItem();
							item.id = boxFile.getId();
							item.name = boxFile.getFileName();
							item.updated = boxFile.getUpdated();
							item.checked = false;
							items[i] = item;
							i++;
						}

						adapter.notifyDataSetChanged();
						/*
						 * ProgressBar progressBar = (ProgressBar)
						 * findViewById(R.id.progressBar);
						 * progressBar.setVisibility(View.GONE);
						 */
					}

					@Override
					public void onIOException(final IOException e) {
						Toast.makeText(getApplicationContext(),
								"Failed to get tree - " + e.getMessage(),
								Toast.LENGTH_LONG).show();
					}
				});
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {

		((TextView) view).getText().toString();
		if (items[position].checked ==true) {
			items[position].checked = false;

		} else if (items[position].checked == false) {
			items[position].checked = true;
		}

	}

	/**
	 * Just a utility class to store BoxFile and BoxFolder objects, which can be
	 * passed as the source data of our list adapter.
	 */
	private class TreeListItem {

		public static final int TYPE_FILE = 1;
		public long id;
		public String name;
		public boolean checked;
		@SuppressWarnings("unused")
		public BoxFolder folder;
		public long updated;
	}

	/*private class MyArrayAdapter extends ArrayAdapter<TreeListItem> {

		private final Context context;
		int listItemResourceId;
		// TreeListItem[] objects;
		TextView tv;

		public MyArrayAdapter(Context contextt, int listItemResourceId,
				TreeListItem[] objects) {
			super(contextt, listItemResourceId, objects);
			// this.objects =objects;
			this.listItemResourceId = listItemResourceId;
			context = contextt;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			View row = convertView;

			LayoutInflater inflater = ((Activity) context).getLayoutInflater();
			row = inflater.inflate(listItemResourceId, null);
			tv = (TextView) row.findViewById(R.id.list_item_main_text);

			
			 * if (items[position].type == TreeListItem.TYPE_FOLDER) {
			 * tv.append("FOLDER: "); } else if (items[position].type ==
			 * TreeListItem.TYPE_FILE) { tv.append("FILE: "); }
			 
			tv.append(items[position].name);
			tv.append("\n");
			tv.append(DateFormat.getDateFormat(getApplicationContext()).format(
					new Date(items[position].updated * 1000)));
			tv.setPadding(10, 20, 10, 20);
			tv.setTypeface(Typeface.DEFAULT_BOLD);
			// }
			return tv;
		}

		@Override
		public int getCount() {
			return items.length;
		}
	}*/
	
	
	

	private class MyArrayAdapter extends BaseAdapter {

		private  Context context;
		int listItemResourceId;
		// TreeListItem[] objects;
		TextView tv;

	public MyArrayAdapter(Context contextt, TreeListItem[] objects) {
			//super(contextt, objects);
			// this.objects =objects;
			///this.listItemResourceId = listItemResourceId;
			context = contextt;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			View row = convertView;

			LayoutInflater inflater = ((Activity) context).getLayoutInflater();
			row = inflater.inflate( R.layout.list_item, null);
			
			
			
			
			tv = (TextView) row.findViewById(R.id.list_item_main_text);

			
			
			tv.append(items[position].name);
			tv.append("\n");
			tv.append(DateFormat.getDateFormat(getApplicationContext()).format(
					new Date(items[position].updated * 1000)));
			tv.setPadding(10, 20, 10, 20);
			tv.setTypeface(Typeface.DEFAULT_BOLD);
			// }
			return tv;
		}

		@Override
		public int getCount() {
			return items.length;
		}

		@Override
		public Object getItem(int arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return 0;
		}
	}

	
	
	

	private void sendToSalesForce(ArrayList<File> filesList) {

		int count = filesList.size();
		encodeImgList = new ArrayList<String>();
		for (int index = 0; index < count; index++) {

			File boxFile = filesList.get(index);

			if (boxFile.canRead()) {

				boxFileName = boxFile.getName();
				boxFilePath = boxFile.getPath();

				mimeType = getMimeType(boxFile.getPath());

				FileInputStream fileInputStream = null;

				byte[] bFile = new byte[(int) boxFile.length()];

				// convert file into array of bytes
				try {
					fileInputStream = new FileInputStream(boxFile);
					fileInputStream.read(bFile);
					fileInputStream.close();
					encodedImage = Base64.encodeToString(bFile, Base64.DEFAULT);
					encodeImgList.add(encodedImage);

				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				String objectType = "Attachment";
				Map<String, Object> fields = new HashMap<String, Object>();
				fields.put("ParentID", "0019000000F8a9b");
				fields.put("Body", encodedImage);
				fields.put("Name", boxFileName);
				// fields.put("ContentType", "image/jpeg");
				fields.put("ContentType", mimeType);

				try {

					// RestRequest req =
					// RestRequest.getRequestForUpsert(apiVersion, objectType,
					// "AccountID", "0019000000F8a9b", fields);
					RestRequest req = RestRequest.getRequestForCreate(
							apiVersion, objectType, fields);
					// Attachment a = new Attachment (ParentId = caseId, Body =
					// pic, ContentType ="image/jpg",Name = "SendViaMyPhone");

					com.metacube.boxforce.MainActivity.sendRequest(req);

				} catch (Exception e) {
				}

			}
		}

	}



	

	private void displayError(String error) {

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

	@Override
	public void onClick(View v) {
		if (v == saveToSF) {
			

			int fileCount = items.length;
			for (int position = 0; position < fileCount; position++) 
			{
				if (items[position].checked) {
					downloadfile(items[position]);
				}
			}
			
		/*	if(fList.size()==0)
			{
				Toast.makeText(Browse.this, "Select Files", 1).show();
			}
			else*/
			
				Intent intent = new Intent(Browse.this,
						SalesForceObjectChooser.class);
				startActivity(intent);
		}
			
			

			
	}

	private void downloadfile(TreeListItem fileItem) {
		
		final Box box = Box.getInstance(Constants.API_KEY);
		final java.io.File destinationFile = new java.io.File(
				Environment.getExternalStorageDirectory() + "/"
						+ URLEncoder.encode(fileItem.name));

		/*
		 * final ProgressDialog downloadDialog = new
		 * ProgressDialog(Browse.this); downloadDialog.setMessage("Downloading "
		 * + items[position].name);
		 * downloadDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		 * downloadDialog.setMax((int) items[position].file.getSize());
		 * downloadDialog.setCancelable(true); downloadDialog.show();
		 */

		/*Toast.makeText(getApplicationContext(),
				"Click BACK to cancel the download.", Toast.LENGTH_SHORT)
				.show();*/

		box.download(authToken, fileItem.id,
				destinationFile, null, new FileDownloadListener() {

					@Override
					public void onComplete(final String status) {
						// downloadDialog.dismiss();
						if (status
								.equals(FileDownloadListener.STATUS_DOWNLOAD_OK)) {

							fList.add(destinationFile);
							fileAttch.setList(fList);

							

							/*Toast.makeText(
									getApplicationContext(),
									"File downloaded to "
											+ destinationFile.getAbsolutePath(),
									Toast.LENGTH_LONG).show();*/
						} else if (status
								.equals(FileDownloadListener.STATUS_DOWNLOAD_CANCELLED)) {
							/*Toast.makeText(getApplicationContext(),
									"Download canceled.", Toast.LENGTH_LONG)
									.show();*/
						}
					}

					@Override
					public void onIOException(final IOException e) {
						e.printStackTrace();
						// downloadDialog.dismiss();
						Toast.makeText(getApplicationContext(),
								"Download failed " + e.getMessage(),
								Toast.LENGTH_LONG).show();
					}

					@Override
					public void onProgress(final long bytesDownloaded) {
						// downloadDialog.setProgress((int) bytesDownloaded);
					}
				});
	}
}
