
tests
=====

Tests are located in jersey-spring-test module.
The module contains a test webapp and test code.
The tests can be run in Jersey test container or an external container.

- Running tests in Jersey test container
    mvn clean test

- Running tests in an external container
  build the test app
  deploy to an external container
  configure container connection info in jersey-spring-test/pom.xml, if needed
  run tests in integration test mode:
    mvn -Pit verify

- Running tests in embedded Jetty instance
  build the test app
  deploy to Jetty:
    mvn -Pjetty jetty:run
  run tests in integration test mode in another console session:
    mvn -Pit verify

test class naming conventions
- *ITTest.java: run in unit and IT test mode
- *Test.java: run as unit tests
- *IT.java: run as IT tests

