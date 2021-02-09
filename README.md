# DemonGO - Demonstrating malicious camera-stream analysis during AR applications (2017-2018)

This application constists of two parts: 
- an Android app using ARCore and OpenCV and 
- an offline Flask server with OCR enabled by Tesseract.

## Motivation

With the emergence of easy-to-use Augmented Reality (AR) frameworks and increasing capabilities of modern smartphones, the potential for automated analysis of the user’s camera feed arises. However, malicious actors could employ such analyses for illicit surveillance or unethical profiling use cases, all the while staying hidden from the end user.

We propose a showcase application that demonstrates two of these use cases. In the context of an AR game, we

1. scan the user’s perimeter for logos of brands and organizations for potential marketing or surveillance and
2. lead the player into providing close-up shots of potentially sensitive textual information. Therefore we first identify potential documents in the scene and then cover them with AR enemies that the user needs to approach to defeat them. This way the user exposes a closer image to the camera which enables our system to detect text using offline OCR.

In order to provide a proof of concept that the combination of AR and computer vision technologies may be used in a malicious way, we created the application Demon GO. As the name implies, the game concept is heavily inspired by the globally successful AR game Pokémon GO and the book series Bartimaeus.
The book revolves around a world in which demons and other magical creatures exist, only visible to other demons or humans who use special devices.

<img src="https://user-images.githubusercontent.com/10089188/107430584-a0c29200-6b25-11eb-97df-b70606f6c21a.png" alt="demon go map" width="500"/>

<img src="https://user-images.githubusercontent.com/10089188/107430543-92747600-6b25-11eb-8d99-f8887b5b529c.png" alt="demon interaction" width="500"/>
<img src="https://user-images.githubusercontent.com/10089188/107431692-1713c400-6b27-11eb-9c95-5b9e49d95cb5.png" alt="pipeline" width="1000"/>
<img src="https://user-images.githubusercontent.com/10089188/107430301-3e699180-6b25-11eb-8237-a7187a571c9c.png" alt="demon go map" width="1000"/>
<img src="https://user-images.githubusercontent.com/10089188/107430467-7a9cf200-6b25-11eb-99f4-6082f5d7497d.png" alt="demon go map" width="500"/>
<img src="https://user-images.githubusercontent.com/10089188/107430421-6bb63f80-6b25-11eb-85fd-03b93ce233ab.png" alt="demon go map" width="500"/>

