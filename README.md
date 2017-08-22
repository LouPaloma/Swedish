# Overview

Thank you for the opportunity to provide this coding sample.

The supplied project provides a minimally viable product (MVP) to meet the requirements of the exercise.  In the `results` folder, you can find the output from a trial run using the supplied 3 files as well as the application log output.

I welcome any questions or comments and look forward to hearing back from you.
 
Project requirements can be found in the file `src\test\resources\README.txt`

# Build

    mvn clean verify

Creates `Swedish-0.0.1-SNAPSHOT.jar` in the `target` directory.  This will also produce a unit test coverage report in the `target\site\index.html` directory.  The current branch coverage of just over 66% is low by my usual standards (typically 80% or more), however, given the small ratio of code branches to hard-to-mock Execptions (e.g., thread interruptions, file access, etc.), I felt that this still provided a fair representation of my approach to unit testing. 



# Usage

The command line usage would be of the form: 

    java -jar Swedish-0.0.1-SNAPSHOT.jar sourceFile1...

Usage is best shown via Maven unit test or running same within an IDE such as eclipse.  This project can be imported into Eclipse via "Import... Existing Maven Projects" and selecting the `pom.xml` file.  The set of 3 files for proof can be run against the Google Translation service via the `/Swedish/src/test/java/org/disorg/SwedishControllerRemoteTest.java` unit test.