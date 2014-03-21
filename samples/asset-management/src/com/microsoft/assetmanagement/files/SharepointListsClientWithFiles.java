/*******************************************************************************
 * Copyright (c) Microsoft Open Technologies, Inc.
 * All Rights Reserved
 * See License.txt in the project root for license information. 
 ******************************************************************************/
package com.microsoft.assetmanagement.files;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.microsoft.office365.Credentials;
import com.microsoft.office365.Logger;
import com.microsoft.office365.OfficeEntity;
import com.microsoft.office365.files.FileClient;
import com.microsoft.office365.lists.SharepointListsClient;

/**
 * This class will be replaced when the new Files API is released to production.
 */

public class SharepointListsClientWithFiles extends SharepointListsClient {

	/**
	 * Instantiates a new sharepoint lists client with files.
	 * 
	 * @param serverUrl
	 *            the server url
	 * @param siteRelativeUrl
	 *            the site relative url
	 * @param credentials
	 *            the credentials
	 */
	public SharepointListsClientWithFiles(String serverUrl, String siteRelativeUrl, Credentials credentials) {
		super(serverUrl, siteRelativeUrl, credentials);
	}

	/**
	 * Instantiates a new sharepoint lists client with files.
	 * 
	 * @param serverUrl
	 *            the server url
	 * @param siteRelativeUrl
	 *            the site relative url
	 * @param credentials
	 *            the credentials
	 * @param logger
	 *            the logger
	 */
	public SharepointListsClientWithFiles(String serverUrl, String siteRelativeUrl, Credentials credentials,
			Logger logger) {
		super(serverUrl, siteRelativeUrl, credentials, logger);
	}

	/**
	 * The Class SPFile.
	 */
	public class SPFile extends OfficeEntity {

	}

	/**
	 * Gets the file.
	 * 
	 * @param listName
	 *            the list name
	 * @param itemId
	 *            the item id
	 * @param fileClient
	 *            the file Client
	 * @return the file
	 */
	public ListenableFuture<DocumentLibraryItem> getFileFromDocumentLibrary(final String listName, final String itemId,
			final FileClient fileClient) {

		final SettableFuture<DocumentLibraryItem> result = SettableFuture.create();
		ListenableFuture<SPFile> picture = getSPFileFromPictureLibrary(listName, itemId);

		Futures.addCallback(picture, new FutureCallback<SPFile>() {
			@Override
			public void onFailure(Throwable t) {
				result.setException(t);
			}

			@Override
			public void onSuccess(SPFile spFile) {
				// TODO:Review if we can use chaining.
				ListenableFuture<byte[]> file = fileClient.getFile(spFile.getData("Name").toString(), listName);
				Futures.addCallback(file, new FutureCallback<byte[]>() {
					@Override
					public void onFailure(Throwable t) {
						result.setException(t);
					};

					@Override
					public void onSuccess(byte[] payload) {
						result.set(new DocumentLibraryItem(payload, itemId));
					}
				});
			}
		});

		return result;
	}

	/**
	 * The Class DocumentLibraryItem.
	 */
	public class DocumentLibraryItem {

		/** The m content. */
		private byte[] mContent;

		/** The m item id. */
		private String mItemId;

		/**
		 * Instantiates a new document library item.
		 * 
		 * @param content
		 *            the content
		 * @param itemId
		 *            the item id
		 */
		public DocumentLibraryItem(byte[] content, String itemId) {
			setContent(content);
			setItemId(itemId);
		}

		/**
		 * Gets the content.
		 * 
		 * @return the content
		 */
		public byte[] getContent() {
			return mContent;
		}

		/**
		 * Sets the content.
		 * 
		 * @param content
		 *            the new content
		 */
		public void setContent(byte[] content) {
			this.mContent = content;
		}

		/**
		 * Gets the item id.
		 * 
		 * @return the item id
		 */
		public String getItemId() {
			return mItemId;
		}

		/**
		 * Sets the item id.
		 * 
		 * @param itemId
		 *            the new item id
		 */
		public void setItemId(String itemId) {
			this.mItemId = itemId;
		}
	}

	/**
	 * Gets the SP file from picture library.
	 * 
	 * @param library
	 *            the library
	 * @param id
	 *            the id
	 * @return the SP file from picture library
	 */
	public ListenableFuture<SPFile> getSPFileFromPictureLibrary(final String library, final String id) {

		final SettableFuture<SPFile> result = SettableFuture.create();
		String getListUrl = getSiteUrl() + "_api/web/lists/GetByTitle('%s')/items('%s')/File";
		getListUrl = String.format(getListUrl, urlEncode(library), id);

		try {
			ListenableFuture<JSONObject> request = executeRequestJson(getListUrl, "GET");
			Futures.addCallback(request, new FutureCallback<JSONObject>() {
				@Override
				public void onFailure(Throwable t) {
					result.setException(t);
				}

				@Override
				public void onSuccess(JSONObject json) {
					SPFile file = new SPFile();
					file.loadFromJson(json);
					result.set(file);
				}
			});

		} catch (Throwable t) {
			result.setException(t);
		}
		return result;
	}

	/**
	 * Upload file.
	 * 
	 * @param documentLibraryName
	 *            the document library name
	 * @param fileName
	 *            the file name
	 * @param fileContent
	 *            the file content
	 * @return the office future
	 */
	public ListenableFuture<SPFile> uploadFile(final String documentLibraryName, final String fileName,
			final byte[] fileContent) {
		final SettableFuture<SPFile> result = SettableFuture.create();

		// The name of the library not always matches the title, here is how we
		// get the real path
		String getRootFolderUrl = getSiteUrl()
				+ String.format("_api/web/lists/GetByTitle('%s')/RootFolder", urlEncode(documentLibraryName));

		ListenableFuture<JSONObject> request = executeRequestJson(getRootFolderUrl, "GET");

		Futures.addCallback(request, new FutureCallback<JSONObject>() {

			@Override
			public void onFailure(Throwable t) {
				result.setException(t);
			}

			@Override
			public void onSuccess(JSONObject json) {
				try {

					String libraryServerRelativeUrl = json.getJSONObject("d").getString("ServerRelativeUrl");
					String getListUrl = getSiteUrl()
							+ "_api/web/GetFolderByServerRelativeUrl('%s')/Files/add(url='%s',overwrite=true)";
					getListUrl = String.format(getListUrl, urlEncode(libraryServerRelativeUrl), urlEncode(fileName));

					Map<String, String> headers = new HashMap<String, String>();
					headers.put("Content-Type", "application/json;odata=verbose");
					ListenableFuture<JSONObject> request = executeRequestJsonWithDigest(getListUrl, "POST", headers,
							fileContent);

					Futures.addCallback(request, new FutureCallback<JSONObject>() {
						@Override
						public void onFailure(Throwable t) {
							result.setException(t);
						}

						@Override
						public void onSuccess(JSONObject json) {
							SPFile file = new SPFile();
							file.loadFromJson(json);
							result.set(file);
						}
					});
				} catch (Throwable t) {
					result.setException(t);
				}
			}
		});

		return result;
	}

	/**
	 * Gets the list item id for file by server relative url.
	 * 
	 * @param serverRelativeUrl
	 *            the server relative url
	 * @return the list item id for file by server relative url
	 */
	public ListenableFuture<String> getListItemIdForFileByServerRelativeUrl(String serverRelativeUrl) {
		final SettableFuture<String> result = SettableFuture.create();

		String getListUrl = getSiteUrl() + "_api/Web/GetFileByServerRelativeUrl('%s')/ListItemAllFields?$select=id";
		getListUrl = String.format(getListUrl, serverRelativeUrl);

		try {
			ListenableFuture<JSONObject> request = executeRequestJson(getListUrl, "GET");

			Futures.addCallback(request, new FutureCallback<JSONObject>() {
				@Override
				public void onFailure(Throwable t) {
					result.setException(t);
				}

				@Override
				public void onSuccess(JSONObject json) {
					try {
						result.set(json.getJSONObject("d").getString("ID"));
					} catch (JSONException e) {
						result.setException(e);
					}
				}
			});

		} catch (Throwable t) {
			result.setException(t);
		}
		return result;
	}
}
