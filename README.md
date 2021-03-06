Weader
======

Weader is a simple Atom/RSS feed reader for Android.  It is written by
Christer Weinigel <christer@weinigel.se>.  Weader was written just for
me as a primitive replacement for Google Reader.

Weader is prounounced as Elmer Fudd would say "reader".

To get started, open the option menu at the main screen and then press
"Refresh".  This will check all feeds for new articles.

To add a new feed, visit the Atom/RSS feed web page with the Android
Browser, you should get a question about which application you would
like to open the page in, select "Weader" and after a short wait the
feed should show up in the list.

Or visit an OPML file in a browser, Weader should add all feeds listed
in the OPML file.

Most screens have an option menu where you can do things and some of
the list items will react to a long press.

Note that everything is stored on the phone, so favorites and the
read/unread status is not synced with any cloud service.  If you
uninstall the application or clear its data all will be lost.

The source to Weader can be found at
[https://github.com/wingel/weader].

News
====

Version 1.4

Ripped out FeedGoal totally, everything is now written from scratch by
Christer Weinigel.  The big changes are a new Atom/RSS feed parser
(which is quite dumb but seems to be functional) and the database
model is completely different.  The new database model should be much
faster so Weader should start up faster.  There is also primitive
support for OPML import.

Building
========

I have only tried to build Weader using Eclipse and Android SDK 22.

Open Preferences and go to Java / Build Path / Classpath Variables.
Add a variable "ANDROID_SDK_HOME" with the path the Android SDK,
i.e. "/opt/android-sdk".  This variable is used by some of the
projects to find libraries inside the Android SDK.

Known Bugs and Missing Features
===============================

Lots.  For the moment I just wanted to get the application up on
Google Play so that friends can test it and come with feedback.

Missing features I'd like to implement some day:

 * Offline support - download images so that feeds can be read
   offline.  This is probably quite tricky.

 * Add a synthetic feed with all favorites

 * Show categories at the end of posts

 * Background download of feeds when the application is not active.

There are a lot more things I'd like to do, but this is just a hobby
project so don't count on it happening soon.

FeedGoal
========

Weader used to be based on another feed reader for Android called
FeedGoal, but all code originally from FeedGoal is now gone.  If you
are interested in FeedGoal anyway, the source to FeedGoal can be found
at [http://code.google.com/p/feedgoal/].

License
=======

Weader is free software: you can redistribute and/or modify it under
the terms of the GNU General Public License as published by the Free
Software Foundation, either version 3 of the License, or (at your
option) any later version.  See [GPL-3.0.txt].

Some parts of Weader are distributed under a less restrictive license,
the Apache License Version 2.0.  See [APACHE-2.0.txt].

The following icons are taken from the Android SDK and are licensed
under the Apache License Version 2.0:

    btn_star_big_off.png
    btn_star_big_on.png
    ic_menu_add.png
    ic_menu_delete.png
    ic_menu_info_details.png
    ic_menu_preferences.png
    ic_menu_refresh.png
    ic_menu_star.png

Other artwork in the source distribution is by Christer Weinigel and
licensed under the Apache License Version 2.0.

The Weader logotype is based on the Feed Icon by Matt Brett found at
[http://www.feedicons.com/].  The W in the logo in the source
distribution is a variant of the W in Ballantines-Medium font.

The logo for the somewhat official release of Weader by Christer
Weinigel is based on the logo for the compay Weinigel Ingenjörsbyrå AB
and can not be used without permisson from that companay.
