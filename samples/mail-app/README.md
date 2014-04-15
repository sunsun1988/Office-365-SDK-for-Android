Copyright � Microsoft Open Technologies, Inc.

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

Overview
---------
A very simple demo that shows you:
 - AAD authentication setup and process 
 - Retrieves all messages from Drafts folder and outputs it to list.
 - Select a message and send with button press.
 
Usage
------ 
1. Import to Eclipse via "General > Existing Project into Workspace".
2. Resolve ADAL library dependency via "Properties > Android > Library section"
3. Add android-support-v4 library to the 'libs' folder and add to build path. You can take it from Android SDK: "<SDK-root>/extras/android/support/v4/"
4. Download mail-calendar-contact.jar (min version is 1.0) and put it into "/libs" folder.
5. Run on device/emulator with API < 4.4