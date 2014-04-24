package com.microsoft.office365.test.integration;

import java.util.concurrent.Future;

import com.microsoft.office365.exchange.ContactClient;
import com.microsoft.office365.exchange.MailClient;
import com.microsoft.office365.test.integration.framework.TestCase;
import com.microsoft.office365.test.integration.framework.TestExecutionCallback;

public class ApplicationContext {

	private static TestPlatformContext mTestPlatformContext;

	public static void setTestPlatformContext(
			TestPlatformContext testPlatformContext) {
		mTestPlatformContext = testPlatformContext;
	}

	public static void sleep() throws Exception {
		mTestPlatformContext.sleep(3);
	}

	public static void sleep(int seconds) throws Exception {
		mTestPlatformContext.sleep(seconds);
	}

	public static Future<Void> showMessage(String message) {
		return mTestPlatformContext.showMessage(message);
	}

	public static void executeTest(TestCase testCase,
			TestExecutionCallback callback) {
		mTestPlatformContext.executeTest(testCase, callback);
	}

	public static String getServerUrl() {
		return mTestPlatformContext.getServerUrl();
	}

	public static MailClient getListsClient() {
		return mTestPlatformContext.getMailClient();
	}

	public static ContactClient getContactClient() {
		return mTestPlatformContext.getContactClient();
	}

	public static String getTestListName() {
		return mTestPlatformContext.getTestListName();
	}
}
