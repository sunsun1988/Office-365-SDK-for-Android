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

## Running tests ##

**Step #1**. Build Mail-Contact-Calendar SDK: 
Execute 
```mvn install``` 
in <em>"/office365-mail-calendar-contact-sdk"</em> directory.
	
**Step #2**. Build tested application:
Execute
```mvn install android:deploy```
in tested application directory (<em>/tested-project</em>).

*NOTE*: steps 1, and 2 are required only: 
- If those project has been updated since the last execution.
- If it is the first time you're running these tests on current machine. 
Otherwise you can SKIP these steps.	

**Step #3**. Build, deploy and run testing application: 
Execute
```mvn package android:deploy android:instrument```
in testing application directory (<em>/testing-project-it</em>).

You can specify ```-Dtest=testA`` parameter while running the tests to execute a single test where 'testA' is the name of selected test method.
