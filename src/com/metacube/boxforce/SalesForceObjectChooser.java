package com.metacube.boxforce;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.box.androidlib.Box;
import com.box.androidlib.ResponseListeners.LogoutListener;
import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.rest.RestClient.AsyncRequestCallback;
import com.salesforce.androidsdk.rest.RestRequest;
import com.salesforce.androidsdk.rest.RestResponse;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ParseException;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class SalesForceObjectChooser extends Activity implements
		OnClickListener, AsyncRequestCallback, OnItemSelectedListener,
		OnItemClickListener

{
	ArrayList<String> objList;
	private String API_VERSION;
	private String authToken;
	private RestClient salesforceRestClient;
	boolean flag = true;
	Spinner objectSpinner;
	RestRequest sobjectsRequest, recordsRequest, attchReq;
	CommonSpinnerAdapter objectsSpinnerAdapter;// , fieldsSpinnerAdapter;
	TemplateApp templateApp;
	ListView list;
	ArrayList<CommonListItems> recordItems;
	ArrayList<String> parentIdList;
	AdapterBaseClass adapter;
	Button save, logoutButton;
	ProgressBar progressBar;
	ProgressDialog dialog;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.object_chooser_list);

		final SharedPreferences prefs = getSharedPreferences(
				Constants.PREFS_FILE_NAME, 0);
		authToken = prefs.getString(Constants.PREFS_KEY_AUTH_TOKEN, null);
		if (authToken == null) {
			Toast.makeText(getApplicationContext(), "You are not logged in.",
					Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

		templateApp = ((TemplateApp) getApplicationContext());

		API_VERSION = getString(R.string.api_version);
		salesforceRestClient = Constants.client;
		parentIdList = new ArrayList<String>();

		save = (Button) findViewById(R.id.save);
		save.setOnClickListener(this);
		logoutButton = (Button) findViewById(R.id.box_logout_button);
		logoutButton.setOnClickListener(SalesForceObjectChooser.this);
		list = (ListView) findViewById(R.id.record_list);
		objectSpinner = (Spinner) findViewById(R.id.object_list_spinner);
		progressBar = (ProgressBar) findViewById(R.id.progressBar);

	}

	@Override
	public void onResume() {
		super.onResume();

		if (salesforceRestClient != null) {
			progressBar.setVisibility(View.VISIBLE);
			
			sobjectsRequest = RestRequest
					.getRequestForDescribeGlobal(API_VERSION);
			salesforceRestClient.sendAsync(sobjectsRequest, this);
		}
	}

	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int position,
			long id) {

		CommonListItems item = (CommonListItems) objectsSpinnerAdapter
				.getItem(position);

		if (salesforceRestClient != null) {
			progressBar.setVisibility(View.VISIBLE);
			//parentIdList = new ArrayList<String>();
			try {
				String soql = "select id, name from " + item.getName();
				recordsRequest = RestRequest.getRequestForQuery(API_VERSION,
						soql);
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			salesforceRestClient.sendAsync(recordsRequest, this);
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSuccess(RestRequest request, RestResponse response) {
		JSONObject responseObject;
		if (request == sobjectsRequest) {
			try {
				responseObject = response.asJSONObject();

				ArrayList<CommonListItems> items = new ArrayList<CommonListItems>();
				JSONArray sobjects = responseObject.getJSONArray("sobjects");
				for (int i = 0; i < sobjects.length(); i++) {
					CommonListItems item = new CommonListItems();
					JSONObject object = sobjects.getJSONObject(i);

					if (checkObjectItem(object) && setSupportedObject(object)) {
						item.setLabel(object.optString("label"));
						item.setName(object.optString("name"));
						items.add(item);
					}
				}
				progressBar.setVisibility(View.INVISIBLE);
				objectsSpinnerAdapter = new CommonSpinnerAdapter(
						getLayoutInflater(), items);
			 objectsSpinnerAdapter.changeOrdering(Constants.SORT_BY_LABEL);
				objectSpinner.setAdapter(objectsSpinnerAdapter);
				objectSpinner.setOnItemSelectedListener(this);

			} catch (ParseException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
				progressBar.setVisibility(View.INVISIBLE);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		else if (request == recordsRequest) {
			try {
				if (!response.isSuccess()) {
					response.toString();

					ArrayList<CommonListItems> emptyList = new ArrayList<CommonListItems>();
					adapter = new AdapterBaseClass(this, emptyList);
					list.setOnItemClickListener(this);
					list.setAdapter(adapter);
					progressBar.setVisibility(View.INVISIBLE);
					flag = true;

				} else {
					responseObject = response.asJSONObject();
					// NotepriseLogger.logMessage("fields"+responseObject.toString());
					recordItems = new ArrayList<CommonListItems>();
					JSONArray fields = responseObject.getJSONArray("records");
					for (int i = 0; i < fields.length(); i++) {
						CommonListItems item = new CommonListItems();
						JSONObject field = fields.getJSONObject(i);

						//parentIdList.add(field.optString("Id"));
						item.setId(field.optString("Id"));
						item.setLabel(field.optString("Name"));
						item.setIsChecked(false);
						recordItems.add(item);

					}
					
					recordItems =	Constants.changeOrdering(Constants.SORT_BY_LABEL,recordItems);
					
					
					progressBar.setVisibility(View.INVISIBLE);
					adapter = new AdapterBaseClass(
							SalesForceObjectChooser.this, recordItems);
					list.setOnItemClickListener(SalesForceObjectChooser.this);
					list.setAdapter(adapter);

				}
			} catch (ParseException e) {
				e.printStackTrace();
				progressBar.setVisibility(View.INVISIBLE);
			} catch (JSONException e) {
				e.printStackTrace();
				progressBar.setVisibility(View.INVISIBLE);
			} catch (IOException e) {
				e.printStackTrace();

			}
		}

		else if (request == attchReq) {
			//progressBar.setVisibility(View.INVISIBLE);
			dialog.dismiss();	

			if (!response.isSuccess()) {
				Toast.makeText(SalesForceObjectChooser.this, response.toString(),
						Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(SalesForceObjectChooser.this,
						"Successfully Attached", Toast.LENGTH_LONG).show();
				finish();
			}

		}

	}

	@Override
	public void onError(Exception exception) {
		Log.v("Error",exception.getMessage().toString());
		exception.printStackTrace();
		Toast.makeText(SalesForceObjectChooser.this, exception.getMessage().toString(), Toast.LENGTH_LONG)
				.show();
		progressBar.setVisibility(View.INVISIBLE);

	}
	
	
	
	
	public  ArrayList<CommonListItems> changeOrdering(String orderType,ArrayList<CommonListItems> items)
	{
		// Sort By Name
		if(orderType.equalsIgnoreCase(Constants.SORT_BY_NAME))
		{
			Collections.sort(items, new CommonListComparator(CommonListComparator.COMPARE_BY_NAME));
		}
		// Sort By Date
		else if(orderType.equalsIgnoreCase(""))
		{
			Collections.sort(items, new CommonListComparator(CommonListComparator.COMPARE_BY_SORT_DATA));
		}
		// Sort By id
		else if (orderType.equalsIgnoreCase(Constants.SORT_BY_ID))
		{
			Collections.sort(items, new CommonListComparator(CommonListComparator.COMPARE_BY_ID));
		}
		// By default sort by Label
		else
		{
			Collections.sort(items, new CommonListComparator(CommonListComparator.COMPARE_BY_LABEL));
		}
		//notifyDataSetChanged();
		
		return items;
	}
	
	

	@Override
	public void onClick(View v) {
		if (v == save) {
			if (checkedItems(recordItems) == 0) {
				Toast.makeText(SalesForceObjectChooser.this,
						"Select Any Record", Toast.LENGTH_LONG).show();
			}

			else {
				//progressBar.setVisibility(View.VISIBLE);
				dialog	 = ProgressDialog.show(SalesForceObjectChooser.this, "", 
			            "Attaching files..", true);

				sendToSalesForce(templateApp.getList(), parentIdList);
			}
		} else if (v == logoutButton) {

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
								Intent i = new Intent(SalesForceObjectChooser.this,
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

	public static Boolean checkObjectItem(JSONObject object) {
		if (object.optString("triggerable").equalsIgnoreCase("true")
				&& object.optString("searchable").equalsIgnoreCase("true")
				&& object.optString("queryable").equalsIgnoreCase("true")) {
			return true;
		}
		return false;
	}

	public static boolean setSupportedObject(JSONObject object) {

		ArrayList<String> objList = new ArrayList<String>();

		// boolean result = false;
		objList.add("Account");
		objList.add("Asset");
		objList.add("Campaign");
		objList.add("Case");
		objList.add("Contact");
		/* objList.add("Contract"); */
		objList.add("Custom objects");
		objList.add("EmailMessage");
		objList.add("EmailTemplate");
		objList.add("Event");
		objList.add("Lead");
		objList.add("Opportunity");
		objList.add("Product");
		objList.add("Solution");
		objList.add("Task");

		for (int j = 0; j < objList.size(); j++) {

			if (objList.get(j).equalsIgnoreCase(object.optString("name")))
				return true;

		}

		return false;

	}

	private void sendToSalesForce(ArrayList<File> filesList,
			ArrayList<String> parentIDLst) {

		int count = filesList.size();
		String encodedImage = null;
		String mimeType;
		String boxFileName;
		for (int index = 0; index < count; index++) {

			File boxFile = filesList.get(index);

			if (boxFile.canRead()) {

				boxFileName = boxFile.getName();
				boxFile.getPath();

				mimeType = getMimeType(boxFile.getPath());

				FileInputStream fileInputStream = null;

				byte[] bFile = new byte[(int) boxFile.length()];

				// convert file into array of bytes
				try {
					fileInputStream = new FileInputStream(boxFile);
					fileInputStream.read(bFile);
					fileInputStream.close();
					encodedImage = Base64.encodeToString(bFile, Base64.DEFAULT);
					// encodeImgList.add(encodedImage);

				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				for (int parentIndex = 0; parentIndex < parentIDLst.size(); parentIndex++) {

					String objectType = "Attachment";
					Map<String, Object> fields = new HashMap<String, Object>();
					fields.put("ParentID", parentIDLst.get(parentIndex));
					fields.put("Body", encodedImage);
					fields.put("Name", boxFileName);
					// fields.put("ContentType", "image/jpeg");
					fields.put("ContentType", mimeType);

					try {

						attchReq = RestRequest.getRequestForCreate(API_VERSION,
								objectType, fields);

						salesforceRestClient.sendAsync(attchReq, this);

					} catch (Exception e) {
						
						//progressBar.setVisibility(View.INVISIBLE);
						dialog.dismiss();
					}

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

	public class AdapterBaseClass extends BaseAdapter {

		// private Activity activity;
		private ArrayList<CommonListItems> itemList;

		private LayoutInflater inflater = null;

		// public ImageLoader imageLoader;

		public AdapterBaseClass(Activity activity,
				ArrayList<CommonListItems> itemList) {

			this.itemList = itemList;
			// this.activity = activity;
			inflater = (LayoutInflater) activity
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		}

		public int getCount() {
			return itemList.size();
		}

		public Object getItem(int position) {
			return position;
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View vi = convertView;

			vi = inflater.inflate(R.layout.list_row, null);
			TextView title = (TextView) vi.findViewById(R.id.title);
			title.setText(itemList.get(position).getLabel());
			ImageView arrowImg = (ImageView) vi.findViewById(R.id.arrow_img);
			arrowImg.setVisibility(View.GONE);

			return vi;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View view, int position,
			long id) {
		ImageView img = (ImageView) view
				.findViewById(R.id.list_item_checkbox_image);

		if (recordItems.get(position).getIsChecked() == true) {
			recordItems.get(position).setIsChecked(false);
			img.setBackgroundResource(R.drawable.button_unchecked);

		} else if (recordItems.get(position).getIsChecked() == false) {
			recordItems.get(position).setIsChecked(true);
			img.setBackgroundResource(R.drawable.button_checked);
		}

	}

 private int checkedItems(ArrayList<CommonListItems> recordItem)
 {
	 int j=0;
	 parentIdList = new ArrayList<String>();
	 for(int i=0 ; i<recordItem.size(); i++)
	 {
		 
		 if(recordItem.get(i).getIsChecked())
		 {
			 parentIdList.add(recordItem.get(i).getId());
			 j=j+1;
		 }

	 }
		
	 return j;
 }
 }

