# DemonGO - Demonstrating Malicious Camera-Stream Analysis during AR Applications (2017-2018)

This showcase application constists of two parts: 
- an Augmented Reaility Android game using ARCore and a sophisticated image processing pipeline using OpenCV and
- an offline Flask server with OCR enabled by Tesseract.

The application demonstrates the risks of camera-stream analysis in immersive AR games.

## Game Motivation 

With the emergence of easy-to-use Augmented Reality frameworks and increasing capabilities of modern smartphones, the potential for automated analysis of the user’s camera feed arises. However, malicious actors could employ such analyses for illicit surveillance or unethical profiling use cases, all the while staying hidden from the end user.

We propose a showcase application that demonstrates two of these use cases. In the context of an AR game, we

1. __scan the user’s perimeter for logos of brands and organizations__ for potential marketing or surveillance and
2. lead the player into providing __close-up shots of potentially sensitive textual information__. Therefore we first identify potential documents in the scene and then cover them with AR enemies that the user needs to approach to defeat them. This way the user exposes a closer image to the camera which enables our system to detect text using offline OCR.

In order to provide a proof of concept that the combination of AR and computer vision technologies may be used in a malicious way, we created the application Demon GO. As the name implies, the game concept is heavily inspired by the globally successful AR game Pokémon GO and the book series Bartimaeus.
The book revolves around a world in which demons and other magical creatures exist, only visible to other demons or humans who use special devices.

The goal of the game is to control as much territory of the augmented world as possible. 
Players need to place public stashes at locations in the real world which mark their reign and have a specific range of influence (Figure 1). 
To defend those stashes against attackers the player needs to collect demons which are flying around in the augmented reality world, waiting to be captured by the players.
Demon GO is using the live location platform Mapbox to display
an interactive map of the augmented world with markers at the geolocations
of stashes with their range of influence.

<img src="https://user-images.githubusercontent.com/10089188/107430584-a0c29200-6b25-11eb-97df-b70606f6c21a.png" alt="demon go map" width="500"/>

### Catching Demons

Players need to find, follow, and fight demons that are present in the augmented reality world and can be seen through their “magic lens”, which corresponds to the augmented reality view in the Demon GO app.
To find a demon, players first have to search for flying spheres that represent demons in their hidden form as depicted in Figure 2a. 
The sphere flies around quickly and changes its direction frequently, which gets players to scan their surrounding area and thus allowing the system to both get an idea of the room and scan for potential points of interest. To force the demon to appear properly, players need to tap (shoot) the sphere multiple times on their phone screen (the *scanning phase*).

The *capturing phase* begins immediately after the demon emerged from its sphere (Figure 2b). 
It is now navigating towards points of interests that were derived from the camera stream in the scanning phase. 
To capture the demon, players first need to get close enough to it, which enables them to weaken it by “casting spells” in form of drawing specific patterns on the screen. By getting closer to the demon the player ideally provides the camera with a better view and angle onto the previously detected points of interest, which results in higher quality frames of potentially sensitive data for better data analysis. 

At the same time, the virtual demon hides the point of
interest underneath it to not arouse any suspicion in the players,
given that they are fully focused on drawing the pattern. 
To further ensure that the point of interest is covered, a smoke particle system is attached to the demon.
<img src="https://user-images.githubusercontent.com/10089188/107430543-92747600-6b25-11eb-8d99-f8887b5b529c.png" alt="demon interaction" width="500"/>

### Snapshots
The data exploitation subsystem and the AR subsystem only communicate
for a single interaction: the AR subsystem provides
frames in what we call snapshots and the data exploitation subsystem
eventually hands back points of interest for the demon to visit
based on these snapshots. This also marks the only way in which
the data exploitation subsystem affects the gameplay.
We distill every frame provided by ARCore to a snapshot. Snapshots
contain the pixel data, typically at a resolution of 1080 by
1920 pixels, the position in virtual space of all tracking points currently
in the ARCore session, and the current view and projection
matrices of the virtual camera. See Figure 3 for a Visualization.
When the data exploitation subsystem has established what the
points of interest in the picture of a snapshot are, if any, it will
instruct the snapshot to map their two-dimensional location on the
picture back to a three-dimensional point in the virtual game space.

<img src="https://user-images.githubusercontent.com/10089188/107435741-d8810800-6b2c-11eb-9a8b-0a99c5ea17dd.png" alt="snapshot data" width="500"/>

## Data Analysis Implementation
The major part of the data analysis is done in the scanning phase by
the pipeline which can be seen in Figure 4. The pipeline is started in
a separate thread as soon as the AR subsystem hands over a snapshot.
The snapshots are pushed to a queue by the AR subsystem
and then continuously popped by the pipeline thread as resources
become free. In addition to the features described in subsection 4.4
a score is added to each snapshot to determine its quality. Furthermore,
an offset will be tracked if the original frame will be cropped
and only a small part is used for further processing.
Every snapshot will then be analyzed with the intent to minimize
the traffic between the client and the server by sending only the
best frames. The server-side analysis will then yield points of interest
based on those frames.

The image analysis pipeline on the mobile device works as follows:
1. Using a Laplace filter blurry images get filtered out
2. Prioritize frames when the angle changed by more than 5 degrees or the phone has been moved by 10 cms
3. Detect patterns using an We utilize an ORB (Oriented FAST and Rotated BRIEF) key point descriptor described by Rublee et al. that is rotation invariant
and robust to noise, and therefore well applicable to realworld scenes [Rublee et al. 2011]. Matches get sent to our server and in the current implementation the user receives a push notification from Google Firebase (e.g. when a Club Mate logo is detected the user gets notified that Mio Mio Mate is on discount right now).

<img src="https://user-images.githubusercontent.com/10089188/107431692-1713c400-6b27-11eb-9c95-5b9e49d95cb5.png" alt="pipeline" width="1000"/>
<img src="https://user-images.githubusercontent.com/10089188/107430301-3e699180-6b25-11eb-8237-a7187a571c9c.png" alt="demon go map" width="1000"/>
<img src="https://user-images.githubusercontent.com/10089188/107430467-7a9cf200-6b25-11eb-99f4-6082f5d7497d.png" alt="demon go map" width="500"/>
<img src="https://user-images.githubusercontent.com/10089188/107430421-6bb63f80-6b25-11eb-85fd-03b93ce233ab.png" alt="demon go map" width="500"/>

