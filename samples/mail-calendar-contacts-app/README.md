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

See the Apache License, Version 2.0 for the specific language
governing permissions and limitations under the License.

## Working with Maven ##

### Prerequisites ###
- [Maven](http://maven.apache.org/download.cgi) 3.1.1 or later
- Android SDK (Platform v19 downloaded)
- ADAL artifact in your local Maven repository 
	- Download here: https://github.com/MSOpenTech/azure-activedirectory-library-for-android
	- Install with: >> mvn clean install
	
### Usage ###
To create .apk file	execute:
```mvn clean package``` OR ```mvn clean install```
To deploy application on connected device or emulator execute:
```mvn android:deploy``` in "demo-app" directory.

## Working in Eclipse ##

### With Maven ###

**Prerequisites**
- Latest version of m2e Eclipse plugin (built in by default into 'Juno' and 'Kepler')
- Android SDK (Platform v19 downloaded)
	
**Usage**
Import demo as "Existing Maven Projects" 

### Without Maven ###

**Prerequisites**
- Eclipse Indigo or Kepler
- Android SDK (Platform v19 downloaded)
- Download Otto library, put it in the 'libs' folder and add to build path. Download: http://square.github.io/otto/
- Add android-support-v4 library to the 'libs' folder and add to build path. You can take it from Android SDK: "<SDK-root>/extras/android/support/v4/"
		
**Usage**
- Import into Eclipse as "Android Sources"
- Resolve ADAL dependency
	- Download here: https://github.com/MSOpenTech/azure-activedirectory-library-for-android
	- Import in Eclipse
	- Add as a library dependency to the demo project
