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

Most screens have an option menu where you can do things and some of
the list items will react to a long press.

Note that everything is stored on the phone, so favorites and the
read/unread status is not synced with any cloud service.  If you
uninstall the application or clear its data all will be lost.

The source to Weader can be found at
[https://github.com/wingel/weader].

FeedGoal
========

Weader is based on another feed reader for Android called FeedGoal.
Almost all the GUI code is new code written by Christer, most of the
guts of the application comes from FeedGoal, it does all of the
fetching and parsing of feeds and it handles the SQLite database on
the phone.  The source to FeedGoal can be found at
[http://code.google.com/p/feedgoal/].

License
=======

Weader and FeedGoal are free software: you can redistribute and/or
modify them under the terms of the GNU General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

See the file LICENSE for more information.

Known Bugs and Missing Features
===============================

Lots.  For the moment I just wanted to get the application up on
Google Play so that friends can test it and come with feedback.

The main two missing features are:

* Automatic refresh - right now one has do a refresh of the feeds
  manually.

* Expiry - articles do not expire, so sooner or later weader will fill
  up your data partition.

There are a lot more things I'd like to do, but this is just a hobby
project so don't count on it happening soon.
