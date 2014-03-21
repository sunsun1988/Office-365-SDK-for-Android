Prerequisites:
--------------

- Maven v3.1.1 or higher. Download: http://maven.apache.org/download.cgi

Installation:
-------------

 >> mvn clean install

 OR

 >> mvn install

Site/Javadoc generation:
------------------------

 You can generate simple javadoc for each project with:

 >> mvn javadoc:javadoc

 You can generate aggregated javadoc with:

 >> mvn javadoc:aggregate

 You can generate aggregated javadoc jar with:

 >> mvn javadoc:aggregate-jar

 To generate a site you'll first need to generate a usual site and then generate a staging site with valid links to submodules.

 1) >> mvn site:site
 2) >> mvn site:stage

 Valid site will be in "/target/staging" directory.

 To generate cross linked source view use JXR plugin:

 >> mvn jxr:jxr

 Results will be in "/target/site/xref" module directories. Also this will be automatically executed when generating a site.

Code verification:
------------------

 Following command will generate html report with code validation results

 >> mvn pmd:pmd

 Following command will generate html report with code duplication (copy-paste) results

 >> mvn pmd:cpd.

Working in Eclipse:
-------------------

 Eclipse files can be generated with:

 >> mvn eclipse:clean eclipse:eclipse

 As result each Maven module will get a consistent .project, .classpath and .settings file with which each module can be imported as existing project to Eclipse.

Testing:
--------

 >> mvn -Pe2eTests

 Mind that you'll need to install artifacts into your local repository first before running the tests. See [Installation] section.

 Integration tests take ~5 minutes. By default integration tests are skipped.
 You can skip all tests by using -Dmaven.test.skip paramater:

 >> mvn install -Dmaven.test.skip

Upgrading version:
------------------
NOTE: dont's use maven-release-plugin, it fails to do what it should.

 1. Execute (in parent directory):
  >> mvn versions:set -DnewVersion=X.YY.ZZ

 2. Execute (odata subdirectory) if you would like to update version of ODataJClient libs (e.g. due to introduced changes):
  >> mvn versions:set -DnewVersion=VV.RR.WW

 Substitute 'X.YY.ZZ' and 'VV.RR.WW' with appropriate version.

 3.
 	a) If you made a mistake, do
		>> mvn versions:revert
	b) Or confirm results
		>> mvn versions:commit

Releasing:
------------------		
 1. Preparing release, generating release metadata files for 'perform' step.
 - mvn release:prepare -Dusername=<username> -Dpassword=<password> -DpreparationGoals="clean install"
 
 2. Removing tag created on the server since it contains invalid version (release plugin bug).
 - git tag -d mail-calendar-contact-0.XY.Z
 - git push origin :refs/tags/mail-calendar-contact-0.XY.Z

 3. Setting valid versions in local working copy.
 - mvn versions:set -DnewVersion=0.XY.Z
 - mvn versions:commit

 4. Removing everything not related to mail-calendar-contact that we are releasing and committing it (put not pushing yet).
 - <remove files/lists sdk, tests, samples>
 - git add -A :/
 - git commit -m "<message>"

 5. Creating a valid remote tag based on local commits (step 4) and resetting local history as though there we no commits. 
	This way we protect the log from being messed up with preparation commits that have nothing to do with meaningful code changes.
 - git tag -a -m "<message>" mail-calendar-contact-0.XY.Z
 - git push --tags
 - git reset --hard HEAD~1

6. Performing actual release based on the tag we've prepared.
 - mvn release:perform -Dusername=<username> -Dpassword=<password>


