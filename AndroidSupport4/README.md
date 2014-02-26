AndroidSupport4
===============

This Android Project for Eclipse is an ugly hack to avoid problems
with including androidsupport4.jar in every Android project that uses
it and have to keep all of them in sync to avoid versioning problems.

This project expects a variable ANDROID_SDK_HOME in Eclipse to be set
and point to where the Android SDK is installed on your machine.  To
set this variable, open the Preferences window in Eclipse and then go
to Java / Build Path / Classpath Variables.  Add a new variable
"ANDROID_SDK_HOME" with the path the Android SDK, on my machine that
is "/opt/android-sdk".  This variable is then used to find
androidsupport4.jar in the SDK and then export the jar to all Android
projects that add a dependency on the AndroidSupport4 project.

