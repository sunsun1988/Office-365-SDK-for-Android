## Prerequisites ##
- [Maven](http://maven.apache.org/download.cgi) v3.1.1 or higher.

## Installation ##
Execute ```mvn clean install``` OR ```mvn install```

## Working in Eclipse ##
Import demo as "Existing Maven Projects"

## Testing ##
Execute ```mvn -Pe2eTests``` to run e2e tests using Java SE

Mind that you'll need to install artifacts into your local repository first before running the tests. See [Installation] section.

Integration tests take ~5-7 minutes. Integration tests are skipped by default.