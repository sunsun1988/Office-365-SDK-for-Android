Copyright © Microsoft Open Technologies, Inc.

All Rights Reserved

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

THIS CODE IS PROVIDED *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS
OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION
ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE, FITNESS FOR A
PARTICULAR PURPOSE, MERCHANTABILITY OR NON-INFRINGEMENT.

See the Apache License, Version 2.0 for the specific language governing permissions and limitations under the License.

-------------------


Working in Eclipse:
-------------------
- Download the code from this branch onto your machine.
- Download ADT from [here](http://developer.android.com/sdk/index.html#win-bundle).
- Open eclipse from the downloaded ADT folder.
- In Eclipse, right click under package and click on import -> Android -> Existing Android code into workspace -> browse -> import the demo-app code into eclipse.
- Import the Azure Active directory Android Library (ADAL) from [here](https://github.com/MSOpenTech/azure-activedirectory-library-for-android/tree/master). 
- Create a “libs” folder under the demo-app folder and add the following JAR files to it.
	- square-otto-1.3.2.JAR from [here](http://search.maven.org/#artifactdetails%7Ccom.squareup%7Cotto%7C1.3.2%7Cjar). 
	- mail contact calendar SDK JAR from [here](https://github.com/OfficeDev/Office-365-SDK-for-Android/blob/master/samples/mail-calendar-contacts-app/demo-app/libs/mail-calendar-contact-core-0.11.1.jar). 
- Create a "libs" folder under the ADAL folder and add the following JAR files to it.
	- 	Android-support-v4.jar from the ADT folder on your machine.
	- 	gson-2.2.2.jar from [here](https://code.google.com/p/google-gson/downloads/list). 
- Go to the properties for the demo-app code -> Android -> Library -> Add -> choose the ADAL library. 
- At this point, you should not have any compile errors and are ready to run the app.

