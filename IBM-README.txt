This version of Cordova has issues compiling with an Android SDK that is later
than API 17. In this case you will receive an error when the bin/create script
attempts to compile the cordova.jar:

BUILD FAILED
sdk/tools/ant/build.xml:720: The following error occurred while executing this line:
sdk/tools/ant/build.xml:734: Compile failed; see the compiler error output for details.

This is compounded by the function where the bin/create script will dynamically
set the target SDK in project.properties to be the latest Android SDK that it
finds on your workstation. There are 2 workarounds for this:

1) Set the target API in project.properties to android-17, then edit bin/create
   to overwrite the $TARGET variable with the simple integer value for
   "android-17" displayed on the "android list targets" command, and overwrite
   the $API_LEVEL variable with "17" after they are initially set by the script.
2) Go into the Android SDK tree, and in the sdk/platforms directory temporarily
   remove (i.e., move into a subdirectory name "disabled") any dirs that are
   later than android-17.

Then re-run the bin-create command.
