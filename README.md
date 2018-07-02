# HomeAndOffice-Android
<img src="https://i.imgur.com/Rag4PM7.png" width="40%" height="40%">

## Intro

This is the official HomeAndOffice Android App. It's my first major endeavor in both Android and Kotlin 
so apologies if the code is a little rough in places!
	
This isn't an official readme, just more or less a bunch of notes on what I have done, how I've done 
it, and what I would like to do in the future. If/when I get the app to a finished state I'll write up some
formal documentation.

The main purpose of this app is simply to play HomeAndOffice tracks on your phone. That's about it!

The app gets all the album info from a server, and fetches the audio files as the user requests them 
(by control buttons or selecting the album from the list)
	
The app is laid out pretty simply at the moment. The user is presented with the media player and a default
album is chosen upon launching the app. The only menu options are "Album Info" and "Album Select". At this 
time you can view these screens, but they are not in their finished, fully functional states.
	
## Server Side
	
Right now, this app connects to a server I have set up in my house which is using no-ip's dynamic DNS service.
There is not really anything happening server-side for this app to function. The audio files and album art 
are stored in directories on the server as is a JSON file which this application parses into a some data classes.
	
Again, if/when the application approaches a more complete state I'll probably move everything to a more 
reliable server.
	
## The Media Player

I built the media player mostly according to Android's developer guide on Media Players.
Most everything that the media player uses is contained within the MediaPlaybackService class which
extends MediaBrowserServiceCompat and implements MediaPlayer.OnPreparedListener.
	
The media player runs in a service on its own thread so it can continue to play in the background
while the user does other stuff or has their screen off.
	
After being initialized from MainActivity in the form of a MediaBrowserCompat object, the service 
creates a MediaSessionCompat, sets an initial PlaybackState, creates an AudioFocusChangeListener,
requests audio focus, and finally asynchronously creates a PlayerModel (which I'll get into next).
	
The service is started/stopped via transport controls (currently just the on-screen buttons).
Check out the code for what each control does in detail.
	
The service is also responsible for creating and maintaining a notification which displays media info
and will eventually have working controls. At the moment, a pause/play button is visible but doesn't
do anything.
	
As of right now, the media player responds to any audio focus change including removal of headphones,
stopping when another audio source gains focus, and ducking out for notification sounds and whatnot.
	
The developer guide included the ability to send a navigable directory structure to any connecting
client. I've written some code to build a list of media items for this sort of functionality, but I
haven't tested it on anything, and probably won't (at least until every other piece of this project
is completely finished).
	
## The Player Model
	
I've opted to use a singleton to keep track of everything that the media player needs.
This singleton includes:
* URL for the JSON file
* All albums, songs, album art
* Current track and album
* Track number
* Boolean for playback state (playing/not playing)
* Functions to go forward and backward in the album
* A function to convert the album to a MediaItem (for MediaBrowser)
* A function that returns a bitmap of the album art
	
The reason I decided to make the player model a singleton is that I was running into difficulties 
keeping the UI updated and synced with the service. I thought it would be easier to maintain my
own model which would remain consistent the whole time the app is running so I could refer to it 
for UI updates and notification changes, knowing that the PlayerModel always has a reliable handle
on what is happening with the media.
	
Additionally it is convenient to have all the relevant information contained within this model so
the album info and album select fragments have easy access to all the info they need, and it only 
needs to be created once.

## UI

The UI for the player is done in an old-timey Windows visual style, with gray blocky buttons. All
the UI elements were created from scratch by me in a web-based SVG editor 
(https://github.com/SVG-Edit/svgedit) and then the SVG was converted to Android Vector-Drawable
using a handy converter (http://a-student.github.io/SvgToVectorDrawableConverter.Web/)
Since I just used this drag and drop method of converting from SVG to drawable, a couple of the 
icons I created for my Nav menu items didn't quite come out right. This is one of the improvements
I need to work on.

The UI is pretty bare-bones by design. Play/pause button, skip/back buttons, and the navigation
drawer make up the selectable items. The album art is displayed in a window above the controls and
the media info is displayed in a little box next to the controls. When a friend was checking out 
my app I noticed he kept trying to touch the album art, which does beg to be clicked being the
most prominent graphic on the screen. I'll probably add some functionality to clicking the album
art in the future. That about covers the home screen.

The nav drawer includes two selectable options. Album Select and Album Info. These both need some 
work. 

The album select screen constructs a RecyclerView containing every album that is included 
in the PlayerModel. The layout that the ViewHolder uses is pretty basic (and ugly) right now.
The list is built correctly, but at the moment it is not possible to select an album from the list.
I haven't looked to deep into this issue yet, so I don't know if it would be easier to figure out
how to make RecyclerView items clickable and have something happen when you click, or just scrap
the RecyclerView and go with a ListView or something else.
	
In its finished state, the Album Select screen will allow you to view all the albums available for
listening and choose which one to listen to. Hopefully it will also look nicer.

The album info screen currently does all it needs to do, I would just like it to look nicer.
It simply shows the album art and a description of the album.

## Issues
* Currently no way to handle bad/nonexistent server response.
* Album art has to be downloaded from server every time it is used (i.e. every time album select or album info screen is shown)
* Background text animation disappears when navigating away from the player screen.
* Background text animation is only present on the player screen.
* Scrolling media info text stops scrolling when navigating away from then back to the player screen.
* Notification control buttons don't display properly (too big, icon doesn't correspond to playback state)
* Notification buttons don't do anything
* Playback continues when notification is cleared.
