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

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.box.androidlib.Box;
import com.box.androidlib.DAO.BoxFile;
import com.box.androidlib.DAO.BoxFolder;
import com.box.androidlib.ResponseListeners.FileDownloadListener;
import com.box.androidlib.ResponseListeners.GetAccountTreeListener;
import com.box.androidlib.Utils.Cancelable;




import com.metacube.boxforce.R;
import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.rest.RestRequest;
import com.salesforce.androidsdk.rest.RestResponse;
import com.salesforce.androidsdk.rest.RestClient.AsyncRequestCallback;
import com.salesforce.androidsdk.util.EventsObservable;
import com.salesforce.androidsdk.util.EventsObservable.EventType;


public class Browse extends Activity implements OnClickListener,OnItemClickListener{

    private MyArrayAdapter adapter;
    private TreeListItem[] items;
    private String authToken;
    private long folderId;
    private String apiVersion;
    String boxFileName;
	String boxFilePath;	
	String encodedImage;
	String mimeType;
	private RestClient client;
	private String[] accountName;
	private String[] accountID;
	Button saveToSF;
	  ListView lv ;
	public static  ArrayList<File> fList;
	TemplateApp fileAttch ;
    // Menu button options
    /*private static final int MENU_ID_UPLOAD = 1;
    private static final int MENU_ID_CREATE_FOLDER = 2;*/

    ArrayList<String> encodeImgList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tree);
        
        apiVersion = getString(R.string.api_version);
        fileAttch = ((TemplateApp)getApplicationContext());
		
		
	//	BOX --------------------
		
        // Check if we have an Auth Token stored.
        final SharedPreferences prefs = getSharedPreferences(Constants.PREFS_FILE_NAME, 0);
        authToken = prefs.getString(Constants.PREFS_KEY_AUTH_TOKEN, null);
        if (authToken == null) {
            Toast.makeText(getApplicationContext(), "You are not logged in.", Toast.LENGTH_SHORT).show();
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
        lv = (ListView)findViewById(R.id.list);
        
        
      
        
        adapter = new MyArrayAdapter(this, R.layout.list_item, items);
        lv.setOnItemClickListener(this);
        lv.setAdapter(adapter);
        
    
    
        refresh();

  
    }

    
    
 
    /**
     * Refresh the tree.
     */
    private void refresh() {
        final Box box = Box.getInstance(Constants.API_KEY);
        box.getAccountTree(authToken, folderId, new String[] {Box.PARAM_ONELEVEL}, new GetAccountTreeListener() {

            @Override
            public void onComplete(BoxFolder boxFolder, String status) {
                if (!status.equals(GetAccountTreeListener.STATUS_LISTING_OK)) {
                    Toast.makeText(getApplicationContext(), "There was an error.", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                /**
                 * Box.getAccountTree() was successful. boxFolder contains a list of subfolders and files. Shove those into an array so that our list adapter
                 * displays them.
                 */

                items = new TreeListItem[boxFolder.getFoldersInFolder().size() + boxFolder.getFilesInFolder().size()];

                int i = 0;

                Iterator<? extends BoxFolder> foldersIterator = boxFolder.getFoldersInFolder().iterator();
                while (foldersIterator.hasNext()) {
                    BoxFolder subfolder = foldersIterator.next();
                    TreeListItem item = new TreeListItem();
                    item.id = subfolder.getId();
                    item.name = subfolder.getFolderName();
                    item.type = TreeListItem.TYPE_FOLDER;
                    item.folder = subfolder;
                    item.updated = subfolder.getUpdated();
                    items[i] = item;
                    i++;
                }

                Iterator<? extends BoxFile> filesIterator = boxFolder.getFilesInFolder().iterator();
                while (filesIterator.hasNext()) {
                    BoxFile boxFile = filesIterator.next();
                    TreeListItem item = new TreeListItem();
                    item.id = boxFile.getId();
                    item.name = boxFile.getFileName();
                    item.type = TreeListItem.TYPE_FILE;
                    item.file = boxFile;
                    item.updated = boxFile.getUpdated();
                    items[i] = item;
                    i++;
                }

                adapter.notifyDataSetChanged();
               /* ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
                progressBar.setVisibility(View.GONE);*/
            }

            @Override
            public void onIOException(final IOException e) {
                Toast.makeText(getApplicationContext(), "Failed to get tree - " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

   
         @Override
     	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
         {
        	 
        	  String fileName = ((TextView) view).getText().toString();
        	  String  fileN=  items[position].name;
        	  
        	  
                    /**
                     * Download a file and put it into the SD card. In your app, you can put the file wherever you have access to.
                     */
                    final Box box = Box.getInstance(Constants.API_KEY);
                    final java.io.File destinationFile = new java.io.File(Environment.getExternalStorageDirectory() + "/"
                                                                          + URLEncoder.encode(items[position].name));

                  /*  final ProgressDialog downloadDialog = new ProgressDialog(Browse.this);
                    downloadDialog.setMessage("Downloading " + items[position].name);
                    downloadDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    downloadDialog.setMax((int) items[position].file.getSize());
                    downloadDialog.setCancelable(true);
                    downloadDialog.show();*/

                    Toast.makeText(getApplicationContext(), "Click BACK to cancel the download.", Toast.LENGTH_SHORT).show();

                    final Cancelable cancelable = box.download(authToken, items[position].id, destinationFile, null, new FileDownloadListener() {

                        @Override
                        public void onComplete(final String status) {
                            //downloadDialog.dismiss();
                            if (status.equals(FileDownloadListener.STATUS_DOWNLOAD_OK)) {
                            	fList= new ArrayList<File> ();
                            	fList.add(destinationFile);
                            	fileAttch.setList(fList);
                            	//sendToSalesForce(fList);
                            	Intent intent = new Intent(Browse.this ,ForceObjectList.class);
                            	startActivity(intent);
                            	 /*Bundle bundle = new Bundle();
                                 bundle.p("FileList", fList);
                                 
                                 intent.putExtras(bundle);
                            	intent.putExtra("FileList", value)*/
                            	
                            	
                                Toast.makeText(getApplicationContext(), "File downloaded to " + destinationFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
                            }
                            else if (status.equals(FileDownloadListener.STATUS_DOWNLOAD_CANCELLED)) {
                                Toast.makeText(getApplicationContext(), "Download canceled.", Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onIOException(final IOException e) {
                            e.printStackTrace();
                           // downloadDialog.dismiss();
                            Toast.makeText(getApplicationContext(), "Download failed " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onProgress(final long bytesDownloaded) {
                           // downloadDialog.setProgress((int) bytesDownloaded);
                        }
                    });
                   /* downloadDialog.setOnCancelListener(new OnCancelListener() {

                        @Override
                        public void onCancel(DialogInterface dialog) {
                            cancelable.cancel();
                        }
                    });*/
                }
        /*      
            }
        }).show();
    }*/

    /**
     * Just a utility class to store BoxFile and BoxFolder objects, which can be passed as the source data of our list adapter.
     */
    private class TreeListItem {

        public static final int TYPE_FILE = 1;
        public static final int TYPE_FOLDER = 2;
        public int type;
        public long id;
        public String name;
        public BoxFile file;
        @SuppressWarnings("unused")
        public BoxFolder folder;
        public long updated;
    }

    private class MyArrayAdapter extends ArrayAdapter<TreeListItem> {

        private final Context context;
        int listItemResourceId;
        //TreeListItem[] objects;
        TextView tv ;

        public MyArrayAdapter(Context contextt, int listItemResourceId, TreeListItem[] objects) {
            super(contextt, listItemResourceId, objects);
            //this.objects =objects;
            this.listItemResourceId= listItemResourceId;
            context = contextt;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
        	    	
        	View row = convertView;            
                         
        	/*if(row == null)
             {
            */     LayoutInflater inflater = ((Activity)context).getLayoutInflater();
                 row = inflater.inflate(listItemResourceId, null);                             
                 tv = (TextView)row.findViewById(R.id.list_item_main_text);
              
                 	
           // TextView tv = new TextView(context);
            if (items[position].type == TreeListItem.TYPE_FOLDER) {
                tv.append("FOLDER: ");
            }
            else if (items[position].type == TreeListItem.TYPE_FILE) {
                tv.append("FILE: ");
            }
            tv.append(items[position].name);
            tv.append("\n");
            tv.append(DateFormat.getDateFormat(getApplicationContext()).format(new Date(items[position].updated * 1000)));
            tv.setPadding(10, 20, 10, 20);
            tv.setTypeface(Typeface.DEFAULT_BOLD);
           //  }
            return tv;
        }

        @Override
        public int getCount() {
            return items.length;
        }
    }

    
    private void sendToSalesForce(ArrayList<File> filesList)
    {
    	
    	int count = filesList.size();
    	 encodeImgList = new ArrayList<String>();
    	for(int index=0; index < count; index++)
    	{
    		
    		
    	File boxFile =	filesList.get(index);
    	
    	if (boxFile.canRead()) {
			

			
			boxFileName = boxFile.getName();
			boxFilePath = boxFile.getPath();
			
			 mimeType = getMimeType(boxFile.getPath());

        	FileInputStream fileInputStream=null;
        	 
           
            byte[] bFile = new byte[(int) boxFile.length()];
     
            
                //convert file into array of bytes
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
             fields.put("ParentID","0019000000F8a9b");
             fields.put("Body",encodedImage);
             fields.put("Name", boxFileName);
             //fields.put("ContentType", "image/jpeg");
             fields.put("ContentType", mimeType);
            
             
             
                
             try {
            
                 //RestRequest req = RestRequest.getRequestForUpsert(apiVersion, objectType, "AccountID", "0019000000F8a9b", fields);
                 RestRequest req = RestRequest.getRequestForCreate(apiVersion, objectType, fields);
               // Attachment a = new Attachment (ParentId = caseId, Body = pic, ContentType ="image/jpg",Name = "SendViaMyPhone");

               com.metacube.boxforce.MainActivity.sendRequest(req);
             
             } catch (Exception e) {}

			
			
			
    		
    	}
    	}
    	
    	
    	
    }
    
    private void attachToForceObject(ArrayList<String> encodedImageTextList, ArrayList<String> parentIDList, ArrayList<String> fileNameList, ArrayList<String> mimeTypeList )
    {
    	
    	for(int imgIndex=0 ;imgIndex<encodedImageTextList.size();imgIndex++)
    	{
    		for(int j=0 ; j<parentIDList.size(); j++)
        	{
    			 String objectType = "Attachment";
                 Map<String, Object> fields = new HashMap<String, Object>();             
                 fields.put("ParentID",parentIDList.get(j));
                 fields.put("Body",encodedImageTextList.get(imgIndex));
                 fields.put("Name", fileNameList.get(imgIndex));
                 //fields.put("ContentType", "image/jpeg");
                 fields.put("ContentType", mimeTypeList.get(imgIndex));
                
                 
                 
                    
                 try {
                
                     //RestRequest req = RestRequest.getRequestForUpsert(apiVersion, objectType, "AccountID", "0019000000F8a9b", fields);
                     RestRequest req = RestRequest.getRequestForCreate(apiVersion, objectType, fields);
                   // Attachment a = new Attachment (ParentId = caseId, Body = pic, ContentType ="image/jpg",Name = "SendViaMyPhone");

                   com.metacube.boxforce.MainActivity.sendRequest(req);
                 
                 } catch (Exception e) {}

    			
    			
        		
        	} 
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
	
	private void displayError(String error)	{
	
	}

    
    
    private void sendRequest(RestRequest request){

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
			if(v==saveToSF)
			{
				
			}
			
		}
    
 
  
    }