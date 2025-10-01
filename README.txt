in this app we implement a saingle activity profile where we do the following:
- capture profile photo
- collect basic info fields
- persists data locally and restore on launch
- support auto rotation
- shows non function 3 dot menu

note: for certain android patterns like the following 
    camera intent
    FileProvider, activityresult apis, sharedpreferences

- refernced course lectures / demos and then took inspiration to reimplement the logic myself

for AndroidManifest.xml the following is what patterns i used: 
    - concept of FileProvider and permissions and adjustResize from the course camera slides
I rewrote the manifest to account for these entries in the project

file_paths.xml --> acceptable path choices, the idea for it was drawn from the lecture notes
on FileProvider

activity_profile.xml --> the layout appriach here is same pattern as in the 
    scrollview + linearlayout + widgets lecture, however the designing of this was done myself

MainActivity.kt --> I kept the camera and result pattern from the lecture, but wrote my
    own helpers and chose my own key names, structures, and error handling. sharedpreferences 
    was used by key and i implemented the validation and conversions myself. 

    resources i needed to reference from is following:
        - camera capture flow
            - intent (MediaStore.ACTION_IMAGE_CAPTURE)
            - putExtra
            - registerForActvityResult (..)

        - local persistene using SharedPreferences:
            - save on the save button
            - resote on the launch
        - rotation safe ui:
            _ android lifecycle + scrollview


build and run -->
- running and tested on android studio narwhal 
- first launch request camera perms
- can select change to take pic, and then save or cancel
- scrollview allows access to all fields so it wont crash

Citations --.
- Camera intent + FileProvider usage & Activity Result APIs
- SharedPreferences
- Scrollview based layout 
-- used above resources as conceptual references, but implemented the code myself