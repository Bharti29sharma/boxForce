package com.metacube.boxforce;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.rest.RestClient.AsyncRequestCallback;
import com.salesforce.androidsdk.rest.RestRequest;
import com.salesforce.androidsdk.rest.RestRequest.RestMethod;
import com.salesforce.androidsdk.rest.RestResponse;

import android.app.Activity;
import android.net.ParseException;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Spinner;
import android.widget.Toast;

public class SalesForceObjectChooser extends Activity implements OnClickListener, AsyncRequestCallback, OnItemSelectedListener 
	
{
	private ListView listView1;
	ArrayList<String> objList;
	private String API_VERSION;
	private RestClient salesforceRestClient;
	boolean flag=true;
	Spinner objectSpinner, fieldSpinner;
	RestRequest sobjectsRequest, recordsRequest,attchReq;
	CommonSpinnerAdapter objectsSpinnerAdapter, fieldsSpinnerAdapter;
	TemplateApp templateApp;
	


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.salesforce_object_chooser_layout);
		
		templateApp = ((TemplateApp) getApplicationContext());
		
		API_VERSION = getString(R.string.api_version);
		salesforceRestClient = Constants.client;
	
		
    	objectSpinner = (Spinner)findViewById(R.id.object_list_spinner);
        fieldSpinner = (Spinner)findViewById(R.id.field_list_spinner);
	}
	
	

	@Override
	public void onResume() 
	{
		super.onResume();
		//baseActivity.salesforceObjectsButton.setVisibility(View.GONE);
		if (salesforceRestClient != null)
		{
			//showProgresIndicator();	
			
			//RestResponse res  = publishNoteToUserGroup(salesforceRestClient,null,null,API_VERSION);
			/*UrlEncodedFormEntity entity = new UrlEncodedFormEntity(postParams);
		    entity.setContentEncoding(HTTP.UTF_8);
		    entity.setContentType("application/json");
		    
			String url = "/services/data/" + API_VERSION + "/sobjects/Attachment/describe";
			sobjectsRequest  = RestRequest(GET,url, entity)
			salesforceRestClient.sendAsync(sobjectsRequest, this);*/
		
			
			sobjectsRequest = RestRequest.getRequestForDescribeGlobal(API_VERSION);
			salesforceRestClient.sendAsync(sobjectsRequest, this);		
		}
	}
	
	

	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int position,
			long id) {
	if(fieldsSpinnerAdapter==(CommonSpinnerAdapter)arg0.getAdapter())
	{
		if(flag)
		{
		Toast.makeText(SalesForceObjectChooser.this, "Record", 1).show();
		flag= false;
		}
		else
		{
			
		CommonListItems item = (CommonListItems) fieldsSpinnerAdapter.getItem(position-1);	
		sendToSalesForce(templateApp.getList(),item.getId());
		
		
		}
			
		
		
		
	}
	else
	{
		
		CommonListItems item = (CommonListItems) objectsSpinnerAdapter.getItem(position);	
		if (salesforceRestClient != null)
		{
			//showProgresIndicator();
			//	baseActivity.saveButton.setVisibility(View.GONE);
			
			
			try {
				String soql = "select id, name from " + item.getName();
				recordsRequest = RestRequest.getRequestForQuery(API_VERSION,soql);
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			
			salesforceRestClient.sendAsync(recordsRequest, this);			
		}
	}
	}



	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void onSuccess(RestRequest request, RestResponse response) {
		JSONObject responseObject;
		if (request == sobjectsRequest)
		{
			try 
			{
				responseObject = response.asJSONObject();
				
				ArrayList<CommonListItems> items = new ArrayList<CommonListItems>();
				JSONArray sobjects = responseObject.getJSONArray("sobjects");
				for (int i=0; i < sobjects.length(); i++)
				{
					CommonListItems item = new CommonListItems();
					JSONObject object = sobjects.getJSONObject(i);
					
					if (checkObjectItem(object) && setSupportedObject(object))
					{
						item.setLabel(object.optString("label"));
						item.setName(object.optString("name"));						
						items.add(item);
					}					
				}
				objectsSpinnerAdapter = new CommonSpinnerAdapter(getLayoutInflater(), items);
				//objectsSpinnerAdapter.changeOrdering(Constants.SORT_BY_LABEL);
				objectSpinner.setAdapter(objectsSpinnerAdapter);
				objectSpinner.setOnItemSelectedListener(this);
			} 
			catch (ParseException e) 
			{
				e.printStackTrace();
			} 
			catch (JSONException e) 
			{
				e.printStackTrace();
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}
		
		else if (request == recordsRequest)
		{
			try 
			{
				responseObject = response.asJSONObject();
				//NotepriseLogger.logMessage("fields"+responseObject.toString());
				ArrayList<CommonListItems> items = new ArrayList<CommonListItems>();
				JSONArray fields = responseObject.getJSONArray("records");
				for (int i=0; i < fields.length(); i++)
				{
					CommonListItems item = new CommonListItems();
					JSONObject field = fields.getJSONObject(i);
					
					/*if (filterObjectFieldForStringType(field))
					{*/
					
						item.setId(field.optString("Id"));
						item.setLabel(field.optString("Name"));
						items.add(item);
					
					
				}
				
				if(fields.length()==0)
				{
					//fieldsSpinnerAdapter=null;
					ArrayList<CommonListItems> emptyList = new ArrayList<CommonListItems>();
					fieldsSpinnerAdapter = new CommonSpinnerAdapter(getLayoutInflater(), emptyList);
					fieldSpinner.setAdapter(fieldsSpinnerAdapter);
					flag=true;
					
				}
				else
				{
				fieldsSpinnerAdapter = new CommonSpinnerAdapter(getLayoutInflater(), items);
				
				//fieldsSpinnerAdapter.changeOrdering(Constants.SORT_BY_LABEL);
				fieldSpinner.setAdapter(fieldsSpinnerAdapter);
				fieldSpinner.setOnItemSelectedListener(this);
				}
				
				//baseActivity.saveButton.setVisibility(View.VISIBLE);
				//hideProgresIndicator();
			} 
			catch (ParseException e) 
			{
				e.printStackTrace();
			} 
			catch (JSONException e) 
			{
				e.printStackTrace();
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}	
		
		else if(request == attchReq)
		{
			 
			

		}
		
	}



	@Override
	public void onError(Exception exception) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		
	}
	
	
	public static Boolean checkObjectItem(JSONObject object)
	{
		if (		object.optString("triggerable").equalsIgnoreCase("true")
				&& 	object.optString("searchable").equalsIgnoreCase("true")
				&& 	object.optString("queryable").equalsIgnoreCase("true")
			)
		{
			return true;
		}		
		return false;
	}
	
	
	public static boolean setSupportedObject(JSONObject object) {
		
		ArrayList<String>	objList= new ArrayList<String>(); 
		
		//boolean result = false;
		objList.add("Account");
		objList.add("Asset");
		objList.add("Campaign");
		objList.add("Case");
		objList.add("Contact");
		objList.add("Contract");
		objList.add("Custom objects");
		objList.add("EmailMessage");
		objList.add("EmailTemplate");
		objList.add("Event");
		objList.add("Lead");
		objList.add("Opportunity");
		objList.add("Product2");
		objList.add("Solution");
		objList.add("Task");
		
	
				
				for (int j=0; j < objList.size(); j++)
				{
					
					if(objList.get(j).equalsIgnoreCase( object.optString("name"))) 
					return true;
			
					
				
				}
		
		return false;

	
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
							 API_VERSION, objectType, fields);
					
					 salesforceRestClient.sendAsync(attchReq, this);

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
	
	
	
	
	
	

	/*public static RestResponse publishNoteToUserGroup(RestClient salesforceRestClient, String groupId, String noteContent, String SF_API_VERSION)
	{
		RestResponse publishResponse = null;
		if (salesforceRestClient != null)
		{
			try 
			{				
				
				String url = "/services/data/" + SF_API_VERSION + "/sobjects/Attachment/describe";
				
				publishResponse = salesforceRestClient.sendSync(RestMethod.GET, url, null);
				
			} 
			catch (UnsupportedEncodingException e) 
			{
				//NotepriseLogger.logError("UnsupportedEncodingException while publishing chatter feed to group.", NotepriseLogger.ERROR, e);
			} 
			catch (IOException e) 
			{
				//NotepriseLogger.logError("IOException while publishing chatter feed to group.", NotepriseLogger.ERROR, e);
			}	
		}
		return publishResponse;
	}	
	
	
	
	public static String getStringFromBundle(Bundle bundle, String identifier)
	{
		String bundleContents = "";
		try 
		{			
			bundleContents = bundle.getString(identifier);		
		} 
		catch (Exception e) 
		{
			//NotepriseLogger.logError("Exception getting arguements.", NotepriseLogger.WARNING, e);
		}
		return bundleContents;
	}*/
	
}