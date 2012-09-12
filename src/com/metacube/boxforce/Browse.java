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
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.box.androidlib.Box;
import com.box.androidlib.DAO.BoxFile;
import com.box.androidlib.DAO.BoxFolder;
import com.box.androidlib.ResponseListeners.FileDownloadListener;
import com.box.androidlib.ResponseListeners.GetAccountTreeListener;
import com.box.androidlib.ResponseListeners.LogoutListener;
import com.metacube.boxforce.R;

public class Browse extends Activity implements OnClickListener,
		OnItemClickListener {

	private MyArrayAdapter adapter;
	// private TreeListItem[] items;
	ArrayList<CommonListItems> items;
	private String authToken;
	private long folderId;
	String boxFileName;
	String boxFilePath;
	String encodedImage;
	String mimeType;
	Button editButton, saveToSF, logoutButton;
	ImageView checkButtonImg;
	ListView lv;
	public static ArrayList<File> fList;
	TemplateApp fileAttch;
	View footer;
	ProgressBar progressBar;
	boolean editMode = true;

	// File fileForPreview;
	int REQUEST_CODE = 1;
	ArrayList<String> encodeImgList;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.boxfile_chooser_layout);

		// getString(R.string.api_version);

		final SharedPreferences prefs = getSharedPreferences(
				Constants.PREFS_FILE_NAME, 0);
		authToken = prefs.getString(Constants.PREFS_KEY_AUTH_TOKEN, null);
		if (authToken == null) {
			Toast.makeText(getApplicationContext(), "You are not logged in.",
					Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

		folderId = 0l;
		Bundle extras = getIntent().getExtras();
		if (extras != null && extras.containsKey("folder_id")) {
			folderId = extras.getLong("folder_id");
		}

		lv = (ListView) findViewById(R.id.file_list);

		saveToSF = (Button) findViewById(R.id.saveTosf);
		logoutButton = (Button) findViewById(R.id.box_logout_button);
		logoutButton.setOnClickListener(Browse.this);

		editButton = (Button) findViewById(R.id.Edit);
		editButton.setOnClickListener(Browse.this);
		saveToSF.setOnClickListener(Browse.this);
		progressBar = (ProgressBar) findViewById(R.id.progressBar);

	}

	@Override
	protected void onResume() {
		super.onResume();
		editMode = true;
		fileAttch = ((TemplateApp) getApplicationContext());
		fList = new ArrayList<File>();

		items = new ArrayList<CommonListItems>();
		saveToSF = (Button) findViewById(R.id.saveTosf);

		saveToSF.setOnClickListener(Browse.this);
		editButton = (Button) findViewById(R.id.Edit);
		editButton.setOnClickListener(this);
		adapter = new MyArrayAdapter(this, false, items);
		lv.setOnItemClickListener(this);
		lv.setAdapter(adapter);
		refresh();

	}

	private void refresh() {
		progressBar.setVisibility(View.VISIBLE);
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
							progressBar.setVisibility(View.INVISIBLE);
							finish();
							return;
						}

						int i = 0;

						Iterator<? extends BoxFile> filesIterator = boxFolder
								.getFilesInFolder().iterator();
						while (filesIterator.hasNext()) {
							BoxFile boxFile = filesIterator.next();
							CommonListItems item = new CommonListItems();
							item.setId(String.valueOf(boxFile.getId()));
							item.setName(boxFile.getFileName());
							item.setSize(boxFile.getSize());
							boxFile.getUpdated();
							item.setIsChecked(false);
							item.setShowListArrow(true);
							items.add(item);
							i++;
						}

						items = Constants.changeOrdering(
								Constants.SORT_BY_NAME, items);

						adapter.notifyDataSetChanged();

						progressBar.setVisibility(View.INVISIBLE);

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
		if (!editMode) {

			checkButtonImg = (ImageView) view
					.findViewById(R.id.list_item_checkbox_image);
			if (items.get(position).getIsChecked() == true) {
				items.get(position).setIsChecked(false);
				checkButtonImg
						.setBackgroundResource(R.drawable.button_unchecked);

			} else if (items.get(position).getIsChecked() == false) {
				items.get(position).setIsChecked(true);
				checkButtonImg.setBackgroundResource(R.drawable.button_checked);
			}
		} else {
			progressBar.setVisibility(View.VISIBLE);
			downloadfile(items.get(position), true);

		}

	}

	private class MyArrayAdapter extends BaseAdapter {

		private Context context;

		TextView title;

		boolean isCheckBoxShow;

		public MyArrayAdapter(Context contextt, boolean isCheckBoxShow,
				ArrayList<CommonListItems> objects) {

			this.isCheckBoxShow = isCheckBoxShow;
			context = contextt;

		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			View row = convertView;
			ImageView arrowImg;

			LayoutInflater inflater = ((Activity) context).getLayoutInflater();
			row = inflater.inflate(R.layout.list_row, null);

			title = (TextView) row.findViewById(R.id.title);

			title.append(items.get(position).getName());
			title.append("\n");
			title.append(fileSize(items.get(position).getSize()));

			checkButtonImg = (ImageView) row
					.findViewById(R.id.list_item_checkbox_image);
			arrowImg = (ImageView) row.findViewById(R.id.arrow_img);

			if (isCheckBoxShow) {
				items.get(position).setIsChecked(false);
				checkButtonImg.setVisibility(View.VISIBLE);
				arrowImg.setVisibility(View.INVISIBLE);
			} else {
				checkButtonImg.setVisibility(View.INVISIBLE);
				items.get(position).setIsChecked(false);
				arrowImg.setVisibility(View.VISIBLE);
			}

			return row;
		}

		@Override
		public int getCount() {
			return items.size();
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

	public static String getMimeType(String url) {
		String type = null;
		String extension = MimeTypeMap.getFileExtensionFromUrl(url);
		if (extension != null) {
			MimeTypeMap mime = MimeTypeMap.getSingleton();
			type = mime.getMimeTypeFromExtension(extension.toLowerCase());
		}
		return type;
	}

	@Override
	public void onClick(View v) {
		if (v == editButton) {

			if (editMode) {

				editMode = false;
				adapter = new MyArrayAdapter(this, true, items);
				lv.setOnItemClickListener(this);
				lv.setAdapter(adapter);

			} else {

				adapter = new MyArrayAdapter(this, false, items);
				lv.setOnItemClickListener(this);
				lv.setAdapter(adapter);
				editMode = true;

			}
		}

		else if (v == saveToSF) {
			if (!editMode) {

				if (checkedItems(items) == 0) {
					Toast.makeText(Browse.this, "Select Files",
							Toast.LENGTH_LONG).show();
				} else {

					int fileCount = items.size();
					progressBar.setVisibility(View.VISIBLE);
					for (int position = 0; position < fileCount; position++) {
						if (items.get(position).getIsChecked()) {
							downloadfile(items.get(position), false);
						}
					}
					progressBar.setVisibility(View.INVISIBLE);

					Intent intent = new Intent(Browse.this,
							SalesForceObjectChooser.class);
					startActivity(intent);
				}
			} else {
				Toast.makeText(Browse.this, "Select any file",
						Toast.LENGTH_LONG).show();
			}
		}

		else if (v == logoutButton) {
			Box.getInstance(Constants.API_KEY).logout(authToken,
					new LogoutListener() {

						@Override
						public void onIOException(IOException e) {
							Toast.makeText(getApplicationContext(),
									"Logout failed - " + e.getMessage(),
									Toast.LENGTH_LONG).show();
						}

						@Override
						public void onComplete(String status) {
							if (status.equals(LogoutListener.STATUS_LOGOUT_OK)) {
								// Delete stored auth token and send user back
								// to
								// splash page
								final SharedPreferences prefs = getSharedPreferences(
										Constants.PREFS_FILE_NAME, 0);
								final SharedPreferences.Editor editor = prefs
										.edit();
								editor.remove(Constants.PREFS_KEY_AUTH_TOKEN);
								editor.commit();
								Toast.makeText(getApplicationContext(),
										"Logged out", Toast.LENGTH_LONG).show();
								Intent i = new Intent(Browse.this,
										MainActivity.class);
								startActivity(i);
								finish();
							} else {
								Toast.makeText(getApplicationContext(),
										"Logout failed - " + status,
										Toast.LENGTH_LONG).show();
							}
						}
					});

		}

	}

	private void downloadfile(CommonListItems fileItem, final boolean isPreview) {

		final Box box = Box.getInstance(Constants.API_KEY);
		final java.io.File destinationFile = new java.io.File(
				Environment.getExternalStorageDirectory() + "/"
						+ URLEncoder.encode(fileItem.getName()));

		box.download(authToken, Long.parseLong(fileItem.getId()),
				destinationFile, null, new FileDownloadListener() {

					@Override
					public void onComplete(final String status) {
						// downloadDialog.dismiss();
						if (status
								.equals(FileDownloadListener.STATUS_DOWNLOAD_OK)) {

							if (!isPreview) {
								fList.add(destinationFile);

								fileAttch.setList(fList);
							} else {
								progressBar.setVisibility(View.INVISIBLE);
								mimeType = getMimeType(destinationFile
										.getPath());
								if (mimeType != null) {

									try {
										Intent intent = new Intent();
										intent.setAction(android.content.Intent.ACTION_VIEW);
										intent.setDataAndType(
												Uri.fromFile(destinationFile),
												mimeType);
										startActivity(intent);
									} catch (Exception e) {
										e.printStackTrace();
										Toast.makeText(Browse.this,
												"File Can't Open", 1).show();

									}

								} else {
									Toast.makeText(Browse.this,
											"File Can't Open", 1).show();
									progressBar.setVisibility(View.INVISIBLE);
								}

							}

						} else if (status
								.equals(FileDownloadListener.STATUS_DOWNLOAD_CANCELLED)) {
							/*
							 * Toast.makeText(getApplicationContext(),
							 * "Download canceled.", Toast.LENGTH_LONG) .show();
							 */
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

	String fileSize(long bytes) {
		if (bytes < 1024)
			return String.valueOf(bytes) + "bytes";

		else if (bytes >= 1024 && bytes <= 1048575)
			return String.valueOf(bytes / 1024) + "KB";

		else if (bytes >= 1048576 && bytes <= 1073741823)
		{
			String  num = String.format("%.2f",  bytes / (1024.0 * 1024.0) );
					
		
			return String.valueOf(num) + "MB";
			
		}
			

		return String.valueOf(bytes / (1024.0 * 1024.0 * 1024.0)) + "GB";

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == REQUEST_CODE) {

		}
	}

	private int checkedItems(ArrayList<CommonListItems> recordItem) {
		int j = 0;
		for (int i = 0; i < recordItem.size(); i++) {

			if (recordItem.get(i).getIsChecked()) {
				j = j + 1;
			}

		}

		return j;
	}

}
