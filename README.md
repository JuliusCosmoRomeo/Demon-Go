# DemonGO - Demonstrating Malicious Camera-Stream Analysis during AR Applications (2017-2018)

This showcase application demonstrates the risks of camera-stream analysis to find sensitive textual information like credit card details or brand logos while the player is involved in immersive AR games.

The application constists of 3 parts: 
- an Augmented Reaility Android game using ARCore and Mapbox
- a sophisticated image processing pipeline using OpenCV on Android and
- an offline Flask server with OCR enabled by Tesseract.

1. [Game Motivation](#game-motivation)
2. [Mobile OpenCV Image Analysis Pipeline](#mobile-opencv-image-analysis-pipeline)
3. [Offline OCR Image Processing](#offline-ocr-image-processing)

The following game description is taken from our [unpublished paper](https://drive.google.com/file/d/1wR71WPDh_DmZJ_nUnx_y0vrCpbbSrKmX/view?usp=sharing) where the full text with all images can be found. 

![image](https://user-images.githubusercontent.com/10089188/107501695-0a7b8400-6b98-11eb-8386-dc94b0318741.png)

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

## Mobile OpenCV Image Analysis Pipeline

<img src="https://user-images.githubusercontent.com/10089188/107431692-1713c400-6b27-11eb-9c95-5b9e49d95cb5.png" alt="pipeline" width="1000"/>

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
![image](https://user-images.githubusercontent.com/10089188/107504203-419f6480-6b9b-11eb-82a1-933182f366cf.png)
![image](https://user-images.githubusercontent.com/10089188/107504214-4532eb80-6b9b-11eb-867c-10a10d6d690e.png)

2. Prioritize frames when the angle changed by more than 5 degrees or the phone has been moved by 10 cms

![image](https://user-images.githubusercontent.com/10089188/107504224-482ddc00-6b9b-11eb-9c11-f2459c392a85.png)

2. a) Detect patterns using an ORB (Oriented FAST and Rotated BRIEF) key point descriptor that is rotation invariant
and robust to noise, and therefore well applicable to realworld scenes. Matches get sent to our server and in the current implementation the user receives a push notification from Google Firebase (e.g. when a *Club Mate* logo is detected the user gets notified that *Mio Mio Mate* is on discount right now).

![image](https://user-images.githubusercontent.com/10089188/107504234-4b28cc80-6b9b-11eb-8d7e-e28a19568463.png)
![image](https://user-images.githubusercontent.com/10089188/107504240-4e23bd00-6b9b-11eb-8d63-589390f37b7b.png)
2. b) Contour Detection 

![image](https://user-images.githubusercontent.com/10089188/107504249-524fda80-6b9b-11eb-83a4-e18d1e459f5b.png)
![image](https://user-images.githubusercontent.com/10089188/107504257-554acb00-6b9b-11eb-984d-c53dd849835b.png)
3. Noise Estimation
![image](https://user-images.githubusercontent.com/10089188/107504272-57ad2500-6b9b-11eb-8479-0309f4cdf9f4.png)
4. Colorfulness Estimation
![image](https://user-images.githubusercontent.com/10089188/107504282-5aa81580-6b9b-11eb-9083-e650b3384698.png)

## Offline OCR Image Processing

The images with high probability to contain text are then sent to the server where the heavier processing continues.
5. Scene text detection finds text snippets within an image
![image](https://user-images.githubusercontent.com/10089188/107504293-5da30600-6b9b-11eb-89f1-8fd5aba5a655.png)
6. If text is found the 2D coordinates of it are determined and sent back to the client where the 3D coordinate is extrapolated and a point of interest is marked in the augmented world. That's where the demon goes next to occlude the text snippet with potentially sensitive content.
![image](https://user-images.githubusercontent.com/10089188/107504301-609df680-6b9b-11eb-9617-e2803c2341cb.png)
7. OCR on the server then analyzes the selected snippets which contain text.
![image](https://user-images.githubusercontent.com/10089188/107504312-6398e700-6b9b-11eb-8a4c-9c03a430fc79.png)
![image](https://user-images.githubusercontent.com/10089188/107504320-65fb4100-6b9b-11eb-994a-7bc2f2498eda.png)


