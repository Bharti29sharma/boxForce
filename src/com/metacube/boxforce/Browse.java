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
import java.util.Iterator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.metacube.boxforce.R;

public class Browse extends Activity implements OnClickListener,
		OnItemClickListener {

	private MyArrayAdapter adapter;
	private TreeListItem[] items;
	private String authToken;
	private long folderId;
	String boxFileName;
	String boxFilePath;
	String encodedImage;
	String mimeType;
	Button saveToSF;
	ListView lv;
	public static ArrayList<File> fList;
	TemplateApp fileAttch;
	View footer;
	ProgressBar progressBar;

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
		saveToSF = (Button) findViewById(R.id.sendToSF);
		saveToSF.setOnClickListener(Browse.this);
		progressBar = (ProgressBar) findViewById(R.id.progressBar);

	}

	@Override
	protected void onResume() {
		super.onResume();

		fileAttch = ((TemplateApp) getApplicationContext());
		fList = new ArrayList<File>();

		items = new TreeListItem[0];
		saveToSF = (Button) findViewById(R.id.sendToSF);

		saveToSF.setOnClickListener(this);
		adapter = new MyArrayAdapter(this, items);
		lv.setOnItemClickListener(this);
		lv.setAdapter(adapter);
		refresh();

	}

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

						progressBar.setVisibility(View.VISIBLE);

						items = new TreeListItem[boxFolder.getFilesInFolder()
								.size()];

						int i = 0;

						Iterator<? extends BoxFile> filesIterator = boxFolder
								.getFilesInFolder().iterator();
						while (filesIterator.hasNext()) {
							BoxFile boxFile = filesIterator.next();
							TreeListItem item = new TreeListItem();
							item.id = boxFile.getId();
							item.name = boxFile.getFileName();
							boxFile.getUpdated();
							item.checked = false;
							items[i] = item;
							i++;
						}

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

		ImageView img = (ImageView) view
				.findViewById(R.id.list_item_checkbox_image);
		if (items[position].checked == true) {
			items[position].checked = false;
			img.setBackgroundResource(R.drawable.button_unchecked);

		} else if (items[position].checked == false) {
			items[position].checked = true;
			img.setBackgroundResource(R.drawable.button_checked);
		}

	}

	private class TreeListItem {

		public long id;
		public String name;
		public boolean checked;
		@SuppressWarnings("unused")
		public BoxFolder folder;
	}

	private class MyArrayAdapter extends BaseAdapter {

		private Context context;
		int listItemResourceId;
		// TreeListItem[] objects;
		TextView title;

		public MyArrayAdapter(Context contextt, TreeListItem[] objects) {
			// super(contextt, objects);
			// this.objects =objects;
			// /this.listItemResourceId = listItemResourceId;
			context = contextt;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			View row = convertView;

			LayoutInflater inflater = ((Activity) context).getLayoutInflater();
			row = inflater.inflate(R.layout.list_row, null);

			title = (TextView) row.findViewById(R.id.title);

			title.setText(items[position].name);

			/*
			 * tv.append("\n");
			 * tv.append(DateFormat.getDateFormat(getApplicationContext
			 * ()).format( new Date(items[position].updated * 1000)));
			 * tv.setPadding(10, 20, 10, 20);
			 * tv.setTypeface(Typeface.DEFAULT_BOLD);
			 */
			// saveToSF.setOnClickListener(Browse.this);
			// }

			return row;
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
			for (int position = 0; position < fileCount; position++) {
				if (items[position].checked) {
					downloadfile(items[position]);
				}
			}

			if (items.length == 0) {
				Toast.makeText(Browse.this, "Select Files", 1).show();
			} else {

				Intent intent = new Intent(Browse.this,
						SalesForceObjectChooser.class);
				startActivity(intent);
			}
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

		/*
		 * Toast.makeText(getApplicationContext(),
		 * "Click BACK to cancel the download.", Toast.LENGTH_SHORT) .show();
		 */

		box.download(authToken, fileItem.id, destinationFile, null,
				new FileDownloadListener() {

					@Override
					public void onComplete(final String status) {
						// downloadDialog.dismiss();
						if (status
								.equals(FileDownloadListener.STATUS_DOWNLOAD_OK)) {

							fList.add(destinationFile);

							fileAttch.setList(fList);

							/*
							 * Toast.makeText( getApplicationContext(),
							 * "File downloaded to " +
							 * destinationFile.getAbsolutePath(),
							 * Toast.LENGTH_LONG).show();
							 */
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
}
