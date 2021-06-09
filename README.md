An Android app with a convenient UI that pushes blog posts to my Github Pages site. Stores the remote repo link and Github credentials in an sqlite db.

# Using the git command on Android #
This app uses a ```git``` binary cross-compiled for arm. You could probably cross-compile it from source yourself, but an easier way to
get your hands on the binary is to just install [Termux](https://github.com/termux/termux-app), run ```apt install git```, and then copy the ```git```, ```git-receive-pack```, ```git-upload-archive```, and ```git-upload-pack``` binaries from /data/data/com.termux/files/usr/bin to /system/bin. (You'll probably need to be root to do this.) ```git``` is linked dynamically, so you'll also need to run ```ldd /system/bin/git``` and copy all of the required libs from /data/data/com.termux/files/usr/lib to /system/lib, then run ```patchelf --set-rpath /system/lib /system/bin/git```. Otherwise the binary will still look for libs in Termux's lib folder, and your app won't have permission to access that folder (unless you ```chown``` to make your app, ```u0_aXXX```, own all of Termux). Anyways, you need to uninstall Termux completely from your device, otherwise ```git``` will still try to look for a config file in /data/data/com.termux and will be denied permission. 

Without Termux installed, you'll likely also have problems with ```git pull``` and ```git push```. (Error msg "git: 'remote-https' is not a git command."). I'm working on a more robust solution for using the git binary on Android.

If you still want to have Termux installed on your device, you can [create your own full clone of Termux with a different package name](https://github.com/termux/termux-app/issues/1761#issuecomment-752077124).  

You'll probably need to run ```git config user.name``` and ```git config user.email``` to get things working.  

An alternative solution would be to use [JGit](https://github.com/eclipse/jgit) or sheimi's [SGit](https://github.com/sheimi/SGit). I used bits and pieces of both in this project.
