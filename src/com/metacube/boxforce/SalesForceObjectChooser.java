package com.metacube.boxforce;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.rest.RestClient.AsyncRequestCallback;
import com.salesforce.androidsdk.rest.RestRequest;
import com.salesforce.androidsdk.rest.RestResponse;
import android.app.Activity;
import android.content.Context;
import android.net.ParseException;
import android.os.Bundle;
import android.util.Base64;
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
	Button save;
	ProgressBar progressBar;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.object_chooser_list);

		templateApp = ((TemplateApp) getApplicationContext());

		API_VERSION = getString(R.string.api_version);
		salesforceRestClient = Constants.client;
		parentIdList = new ArrayList<String>();

		save = (Button) findViewById(R.id.save);
		save.setOnClickListener(this);
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

		CommonListItems item = (CommonListItems) objectsSpinnerAdapter.getItem(position);
		
		if (salesforceRestClient != null) {
			progressBar.setVisibility(View.VISIBLE);
			parentIdList = new ArrayList<String>();
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
				// objectsSpinnerAdapter.changeOrdering(Constants.SORT_BY_LABEL);
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

						parentIdList.add(field.optString("Id"));
						item.setId(field.optString("Id"));
						item.setLabel(field.optString("Name"));
						item.setIsChecked(false);
						recordItems.add(item);

					}
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
			progressBar.setVisibility(View.INVISIBLE);
			if (!response.isSuccess()) {
				Toast.makeText(SalesForceObjectChooser.this, "Error",
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
		Toast.makeText(SalesForceObjectChooser.this, "Error", Toast.LENGTH_LONG)
				.show();
		progressBar.setVisibility(View.INVISIBLE);

	}

	@Override
	public void onClick(View v) {

		if (parentIdList.size() == 0)
		{
			Toast.makeText(SalesForceObjectChooser.this, "Select Any Record",
					Toast.LENGTH_LONG).show();
		}

		else
		{
			progressBar.setVisibility(View.VISIBLE);
		sendToSalesForce(templateApp.getList(), parentIdList);
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
		/*objList.add("Contract");*/
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
			ArrayList<String> parentIDList) {

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

				for (int parentIndex = 0; parentIndex < parentIDList.size(); parentIndex++) {

					String objectType = "Attachment";
					Map<String, Object> fields = new HashMap<String, Object>();
					fields.put("ParentID", parentIDList.get(parentIndex));
					fields.put("Body", encodedImage);
					fields.put("Name", boxFileName);
					// fields.put("ContentType", "image/jpeg");
					fields.put("ContentType", mimeType);

					try {

						attchReq = RestRequest.getRequestForCreate(API_VERSION,
								objectType, fields);

						salesforceRestClient.sendAsync(attchReq, this);

					} catch (Exception e) {
						progressBar.setVisibility(View.INVISIBLE);
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

}