# mule-multiproject

This project is an attempt to make a working multi-domain mule application suite that can be tested using the
mule-maven-plugin to run integration tests. Also, you can add 'backdoor' code to the integration-tests mule app
to get a behind the scenes look at things if you need to.

There are two apps and one integration test directory that can be run separate (i.e. it is not a module of the parent).
The basic 'trick' is to use the maven dependecy plugin to place a domain and application ZIPs into a build target
directory where the mule runtime is then extracted.

If this is using Windows, your cmd shell (or IDE) needs to run as admin and you need to have a 32-bit JDK available
because of the java service wrapper licensing issues for community edition. You can run this like:

    % mvn clean package
    % cd integration-tests
    % mvn clean verify

This should start a Mule ESB instance and execute the tests in integration.
