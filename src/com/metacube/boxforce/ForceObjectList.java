package com.metacube.boxforce;

import java.util.ArrayList;

import com.metacube.boxforce.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class ForceObjectList extends Activity implements OnItemClickListener {

	private ListView listView1;
	ArrayList<String> filesList;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tree);

	
		filesList = new ArrayList<String>();

		

		FilesAdapter adapter = new FilesAdapter(this, R.layout.list_item,
				filesList);

		listView1 = (ListView) findViewById(R.id.list);

		View header = (View) getLayoutInflater().inflate(
				R.layout.listview_header_row, null);
		listView1.addHeaderView(header);
		listView1.setOnItemClickListener(this);
		
		
		listView1.setAdapter(adapter);
		
		setSupportedObject(filesList);
	}

	/*
	 * @Override public boolean onCreateOptionsMenu(Menu menu) {
	 * getMenuInflater().inflate(R.menu.activity_main, menu); return true; }
	 */

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		
		String objectName = ((TextView) view).getText().toString();
	
		Intent intent = new Intent(ForceObjectList.this,
				ForceRecordTypeList.class);
		intent.putExtra("Object Name", objectName);
		startActivity(intent);

	}

	public static void setSupportedObject(ArrayList<String> filesList) {
		
		boolean result = false;
		filesList.add("Account");
		filesList.add("Asset");
		filesList.add("Campaign");
		filesList.add("Case");
		filesList.add("Contact");
		filesList.add("Contract");
		filesList.add("Custom objects");
		filesList.add("EmailMessage");
		filesList.add("EmailTemplate");
		filesList.add("Event");
		filesList.add("Lead");
		filesList.add("Opportunity");
		filesList.add("Product2");
		filesList.add("Solution");
		filesList.add("Task");

	
	}
}
