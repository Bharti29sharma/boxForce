package com.metacube.boxforce;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.metacube.boxforce.R;
import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.rest.RestRequest;
import com.salesforce.androidsdk.rest.RestResponse;
import com.salesforce.androidsdk.rest.RestClient.AsyncRequestCallback;
import com.salesforce.androidsdk.util.EventsObservable;
import com.salesforce.androidsdk.util.EventsObservable.EventType;

public class ForceRecordTypeList extends Activity implements OnItemClickListener, AsyncRequestCallback {

	private ListView listView1;
	ArrayList<String> filesList;
	private String apiVersion;
	private RestClient client;
	private String[] accountName;
	private String[] accountID;
	ArrayList<TreeListItem> items;
	RestRequest attchReq,objectRequest;
	TemplateApp fileAttch;
	String objectName;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tree);
		apiVersion = getString(R.string.api_version);
		client = Constants.client;
		items = new ArrayList<TreeListItem>();
		fileAttch = ((TemplateApp) getApplicationContext());

		objectName = (String) getIntent().getExtras().get("Object Name");

		getAccountList(objectName);
		

		listView1 = (ListView) findViewById(R.id.list);

		View header = (View) getLayoutInflater().inflate(
				R.layout.listview_header_row, null);
		listView1.addHeaderView(header);
	}

	/*
	 * @Override public boolean onCreateOptionsMenu(Menu menu) {
	 * getMenuInflater().inflate(R.menu.activity_main, menu); return true; }
	 */

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		// TODO Auto-generated method stub
		String fileName = ((TextView) view).getText().toString();

		TreeListItem item = items.get(position-1);
		
		
		sendToSalesForce(fileAttch.getList(),item.id);

	}

	private void getAccountList(String object) {

		try {

			String soql = "select id, name from " + object;

			 objectRequest = RestRequest.getRequestForQuery(apiVersion,
					soql);

			client.sendAsync(objectRequest, this);

		} catch (Exception e) {
			e.printStackTrace();
			displayError(e.getMessage());
		}
	}

	private void displayError(String error) {

	}

	private class TreeListItem {

		public String id;
		public String name;

	}

	private class MyArrayAdapter extends ArrayAdapter<TreeListItem> {

		private final Context context;
		int listItemResourceId;
		ArrayList<TreeListItem> objects;
		TextView tv;

		public MyArrayAdapter(Context contextt, int listItemResourceId,
				ArrayList<TreeListItem> objects) {
			
			super(contextt, listItemResourceId, objects);
			this.objects = objects;
			this.listItemResourceId = listItemResourceId;
			context = contextt;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			View row = convertView;

		
				LayoutInflater inflater = ((Activity) context)
						.getLayoutInflater();
				row = inflater.inflate(listItemResourceId, null);
				tv = (TextView) row.findViewById(R.id.list_item_main_text);

				tv.setText(objects.get(position).name);

				// tv.setTypeface(Typeface.DEFAULT_BOLD);
			
			return tv;
		}

		@Override
		public int getCount() {
			return objects.size();
		}
	}

	private void sendToSalesForce(ArrayList<File> filesList,String parentID) {

		int count = filesList.size();
		String encodedImage = null;
		String mimeType;
		String boxFileName;
		String boxFilePath;
	
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
					//encodeImgList.add(encodedImage);

				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				String objectType = "Attachment";
				Map<String, Object> fields = new HashMap<String, Object>();
				fields.put("ParentID", parentID);
				fields.put("Body", encodedImage);
				fields.put("Name", boxFileName);
				// fields.put("ContentType", "image/jpeg");
				fields.put("ContentType", mimeType);

				try {

					
					 attchReq = RestRequest.getRequestForCreate(
							apiVersion, objectType, fields);
					
					client.sendAsync(attchReq, this);

					/*com.metacube.boxforce.MainActivity
							.sendRequest(req);*/

				} catch (Exception e) {
				}

			}
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
	public void onSuccess(RestRequest request, RestResponse response) {
		
		try {
			if (response == null || response.asJSONObject() == null)
				return;
			
			if(request==objectRequest)
			{
				
			
			

			JSONArray records = response.asJSONObject()
					.getJSONArray("records");

			if (records.length() == 0)
				return;

			accountName = new String[records.length()];
			accountID = new String[records.length()];

			for (int i = 0; i < records.length(); i++) {
				JSONObject account = (JSONObject) records.get(i);
				// accountName[i] = account.getString("Name");
				// accountID[i] = account.getString("Id");
				TreeListItem item = new TreeListItem();
				item.id = account.getString("Id");
				item.name = account.getString("Name");
				items.add(item);

			}
			MyArrayAdapter adapter = new MyArrayAdapter(this, R.layout.list_item,
					items);
			
			listView1.setOnItemClickListener(this);
			listView1.setAdapter(adapter);
			}
			EventsObservable.get().notifyEvent(
					EventType.RenditionComplete);
		} catch (Exception e) {
			e.printStackTrace();
			displayError(e.getMessage());
		}
		
	}

	@Override
	public void onError(Exception exception) {
		displayError(exception.getMessage());
		EventsObservable.get().notifyEvent(
				EventType.RenditionComplete);
	
		
	}
	
	

}
