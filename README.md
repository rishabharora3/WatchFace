## Application Recording:


https://user-images.githubusercontent.com/14349274/170134793-91075070-8779-4b82-ae03-4427c2c2b477.mp4




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

A demo video for the project has been attached in the zip.

Thank You!

References:

1. https://developer.android.com/training/wearables/health-services
2. https://github.com/android/health-samples
3. https://github.com/android/wear-os-samples/tree/main/RuntimePermissionsWear
4. https://developer.android.com/training/wearables/data/wear-permissions

Author: Rishabh Arora


