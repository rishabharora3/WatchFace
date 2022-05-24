## Application Recording:

https://user-images.githubusercontent.com/14349274/170134901-3143a69f-54e3-4875-ad43-0f3b41dd7a01.mp4


# Heart Rate Monitor

An application demonstrates the rapid heart rate updates using the `MeasureClient`
API.

### Running the application

You will need a Wear device or emulator with Health Services installed. Open the project in Android
Studio and launch the app on your device or emulator.

On startup, the app shows a screen where the user needs to click to allow the permission for BODY
SENSORS. Once enabled, the heart rate is tracked every 10 seconds and stored in a Room database.
Last heart rate is being tracked by querying the database.

Project has been tested using the Virtual Senors -> Additional sensors -> Heart Rate (bpm) in the
emulator options.

A demo video for the project has been attached.

Thank You!

References:

1. https://developer.android.com/training/wearables/health-services
2. https://github.com/android/health-samples
3. https://github.com/android/wear-os-samples/tree/main/RuntimePermissionsWear
4. https://developer.android.com/training/wearables/data/wear-permissions

Author: Rishabh Arora


Screenshots:

![Screenshot_20220329_203805](https://user-images.githubusercontent.com/14349274/170135348-ae85f09a-3553-41ea-9981-cbfc0a9dedea.png)
![Screenshot_20220329_203835](https://user-images.githubusercontent.com/14349274/170135355-f766695d-8e52-4753-aa5b-504d90bf4019.png)
![Screenshot_20220329_210519](https://user-images.githubusercontent.com/14349274/170135362-50f448d9-e8cc-4a69-8e8f-5dbba079e34f.png)
