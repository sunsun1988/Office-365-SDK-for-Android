/*******************************************************************************
 * Copyright (c) Microsoft Open Technologies, Inc.
 * All Rights Reserved
 * See License.txt in the project root for license information. 
 ******************************************************************************/
package com.microsoft.filediscovery.datasource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import com.microsoft.filediscovery.AssetApplication;
import com.microsoft.filediscovery.Constants;
import com.microsoft.filediscovery.viewmodel.FileSaveItem;
import com.microsoft.filediscovery.viewmodel.FileViewItem;
import com.microsoft.filediscovery.viewmodel.ServiceViewItem;
import com.microsoft.office365.DiscoveryInformation;
import com.microsoft.office365.OfficeClient;
import com.microsoft.office365.files.FileClient;
import com.microsoft.office365.files.FileSystemItem;

/**
 * The Class ListItemsDataSource.
 */
public class ListItemsDataSource {

	/** The m application. */
	private AssetApplication mApplication;

	/**
	 * Instantiates a new list items data source.
	 *
	 * @param application the application
	 */
	public ListItemsDataSource(AssetApplication application) {
		mApplication = application;
	}

	public ArrayList<ServiceViewItem> getServices() {
		final ArrayList<ServiceViewItem> serviceItems = new ArrayList<ServiceViewItem>();
		OfficeClient officeClient = mApplication.getOfficeClient(Constants.DISCOVERY_RESOURCE_ID);

		List<DiscoveryInformation> services = null;
		try {
			services = officeClient.getDiscoveryInfo().get();
		} catch (Exception e) {
			e.printStackTrace();
		} 

		for (DiscoveryInformation service : services) {
			ServiceViewItem item = new ServiceViewItem();

			item.Selectable = service.getCapability().equals(com.microsoft.filediscovery.Constants.MYFILES_CAPABILITY);
			item.Name = service.getServiceName();
			item.EndpointUri = service.getServiceEndpointUri().split("_api")[0];
			item.ResourceId = service.getServiceResourceId();
			serviceItems.add(item);
		}

		return serviceItems;
	}

	public ArrayList<FileViewItem> getFiles(String resourceId, String endpoint) {
		FileClient fileClient = mApplication.getCurrentFileClient(resourceId, endpoint);

		ArrayList<FileViewItem> files = new ArrayList<FileViewItem>();
		try {
			List<FileSystemItem> items = fileClient.getFileSystemItems().get();

			for(FileSystemItem item : items){
				FileViewItem file = new FileViewItem();
				file.Id = item.getData("Id").toString();
				file.Name = item.getName();
				file.CreatedOn = item.getData("TimeCreated").toString();
				files.add(file);

			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}

		return files;
	}
	
	public void saveFile(FileSaveItem file) {
		FileClient fileClient = mApplication.getCurrentFileClient(file.ResourceId, file.Endpoint);

		try {
			fileClient.createFile(file.Name + ".png",null,false,file.Content).get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}
}