# Coroshit
Mobile application that will tell you if you have been around someone infected with a virus in the last 15 days.
# How it works
This app will sample your location every 5 seconds using a background service, and save it locally on your device. Whenever there is a confirmed case, the location history of the newly confirmed case will be pushed to the firebase realtime database anonymously, and other devices are able to download it, and compare it with the locally saved one to identify whether they were around the newly confirmed case in the last 15 days or not.

# Data privacy
This app does not require registration, and only saves you location history locally. For confirmed cases the data is published without identifying its owner. 

# What can you help with ?
- This application is still under development, and we need to create an IOS version for it.
- We need to test what we have now. so clone it, build it, install it on your phone. You will need at least two devices to test the application, device A (confirmed case), B (normal). 
  - Step 1, you can start the location collector service from the mainactivity (make sure your mobile location is on).
  - Step 2, hit the change status button on device (A) after giving some time to the location collector to save some locations and timestamps.
  - Step 3, click the change status button. it will upload the locations saved on your device.
  - Step 4, perform step 1 on device (B).
  - Step 5, click the sync button on device (B)
  You should see the background color of the QRcode on device B turning to yellow, if the two devices were near each other at step 1.
- We need to write test cases for the current functionality. 

if you think you are able to help send an email to hanora@coroshit.com
