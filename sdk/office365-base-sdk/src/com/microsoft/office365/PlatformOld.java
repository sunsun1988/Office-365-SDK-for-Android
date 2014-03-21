/*******************************************************************************
 * Copyright (c) Microsoft Open Technologies, Inc.
 * All Rights Reserved
 * See License.txt in the project root for license information.
 ******************************************************************************/
package com.microsoft.office365;

import java.util.Locale;

import android.os.Build;

import com.microsoft.office365.http.FroyoHttpConnectionOld;
import com.microsoft.office365.http.HttpConnectionOld;
import com.microsoft.office365.http.JavaHttpConnectionOld;


/**
 * Platform specific classes and operations
 */
public class PlatformOld {
	static boolean mPlatformVerified = false;
	static boolean mIsAndroid = false;
	
	/**
	 * Creates an adequate HttpConnection for the current platform
	 * @param logger Logger to use with the connection
	 * @return An HttpConnection
	 */
	public static HttpConnectionOld createHttpConnection() {
		if (isAndroid() && Build.VERSION.SDK_INT <= Build.VERSION_CODES.FROYO) {
			return new FroyoHttpConnectionOld();
		} else {
			return new JavaHttpConnectionOld();
		}
	}

	/**
	 * Indicates if the current platform is Android
	 */
	public static boolean isAndroid() {
		
		if (!mPlatformVerified) {
			String runtime = System.getProperty("java.runtime.name").toLowerCase(Locale.getDefault());
			
			if (runtime.contains("android")) {
				mIsAndroid = true;
			} else {
				mIsAndroid = false;
			}
			
			mPlatformVerified = true;
		}

		return mIsAndroid;
	}
	
	/**
     * Generates the User-Agent
     */
    public static String getUserAgent() {
        String osName;

        if (isAndroid()) {
            osName = "Android API" + Build.VERSION.SDK_INT;
        } else {
            osName = System.getProperty("os.name").toLowerCase(Locale.getDefault());
        }
        String userAgent = String.format("Office (lang=Java; os=%s; version=1.0)", osName);

        return userAgent;
    }
}
