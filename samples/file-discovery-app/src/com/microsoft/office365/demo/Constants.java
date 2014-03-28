package com.microsoft.office365.demo;

public class Constants {
	// -------------------------------AAD PARAMETERS----------------------------------
	public static final String AUTHORITY_URL = "https://login.windows.net/common/oauth2/token";
	public static final String CLIENT_ID = "da146996-bb8c-45f4-a054-bdecba247cb6"; 
	public static final String REDIRECT_URL = "http://msopentechtest.com";
	
	// In this initial Preview release, you must use a temporary Resource ID for Service Discovery ("Microsoft.SharePoint").
    // TODO: If this Resource ID ceases to work, check for an updated value at http://go.microsoft.com/fwlink/?LinkID=392944

	public static final String DISCOVERY_RESOURCE_ID =  "Microsoft.SharePoint";
	public static final String MYFILES_CAPABILITY = "MyFiles";
	//------------------------------------------------------------------------------------------	
    public static final String ENCRYPTION_KEY = "EncryptionKey";
}
