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

import java.util.ArrayList;
import java.util.Collections;

import com.salesforce.androidsdk.rest.RestClient;

public class Constants {

    // IMPORTANT: Set this to your OpenBox API Key. This demo will not work
    // until you do so!!!
    // To get an API Key, go to https://www.box.net/developers
    public static final String API_KEY = "q2cccyv2o4yz5764elf6icy7ixdzie0y ";
    		/*"pambred0e4sbqfuzykl95uuu3362x4ei";*/
    public static RestClient client; 
    public static final String PREFS_FILE_NAME = "prefs";
    public static final String PREFS_KEY_AUTH_TOKEN = "AUTH_TOKEN";
    public static final String ITEM_TYPE_LIST_ITEM = "LIST_ITEM";
    
    public static String SORT_BY_LABEL = "SORT_BY_LABEL";
	public static String SORT_BY_NAME = "SORT_BY_NAME";
	public static String SORT_BY_ID = "SORT_BY_ID";
	public static String SORT_BY_SORT_ORDER = "SORT_BY_SORT_ORDER";	
	
	public static  ArrayList<CommonListItems> changeOrdering(String orderType,ArrayList<CommonListItems> items)
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
    
}


