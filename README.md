[![Stories in Ready](http://badge.waffle.io/NightWhistler/PageTurner.png)](http://waffle.io/NightWhistler/PageTurner)  
PageTurner, the synchronizing ebook reader for Android
========================================================

PageTurner is a free, open-source ebook reader that allows you to keep your reading progress synchronized across multiple devices. This means you can read a few pages on your phone, than grab your tablet continuing where you left off.

ee http://www.pageturner-reader.org/ for more info and some screenshots.

PageTurner is licensed under the GPL-V3 license.


Progress Synchronization
------------------------

One of the key features of PageTurner is that it automatically syncs your reading progress across all your devices. 

This is achieved through a JSON-based back-end service, which stores progress points in a database.
There is a ready-to-use synchronization service running at api.pageturner-reader.org, but it requires an access key to use.
Access keys can be obtained by donating to the project, or by contributing code, translations, etc. If you wish to run your
own synchronization service, the code is available in the PageTurnerWeb project on Github.

There are also 2 versions of PageTurner available through the Google Play Market which have access keys built in:

 * [PageTurner Reader](https://play.google.com/store/apps/details?id=net.nightwhistler.pageturner.ads "PageTurner available for free") is available for free from the Google Play Store. This version contains ads. 
 * [PageTurner Pro](https://play.google.com/store/apps/details?id=net.nightwhistler.pageturner.pro "PageTurner Pro paid removes ads"), a paid version, also available from the Google Play Store, removes ads.


Contributing
------------

Since PageTurner is dual-licensed, we can only accept contributions under the Apache License or a similar Permissive license.

Unless specifically stated to be otherwise, all contributions will be assumed to be licensed under the Apache 2.0 license.

Building PageTurner
-------------------

# Install Java
*   On Ubuntu

        sudo apt-get install openjdk-8-jdk

*   On Windows install the JDK from http://www.oracle.com/technetwork/java/javase/downloads/index.html

PageTurner uses Java 8 lambda's through usage of the RetroLambda library.

# Install the Android SDK 

1.   Download at http://developer.android.com/sdk/index.html
2.   Unzip
3.   Update 

        sdk/tools/android update sdk --no-ui
4. On Ubuntu install ia32-libs

        apt-get install ia32-libs
5. Add sdk/tools/ and sdk/platform-tools to your PATH

# Install USB drivers for your device

*   Make sure adb devices shows your device, for example

        $ adb devices
        List of devices attached 
        015d18ad5c14000c        device

# Example PATH setup in .bashrc

    export ANDROID_HOME=$HOME/projects/adt-bundle-linux/sdk/
    if [ $(uname -m) == 'x86_64' ]; then
        export JAVA_HOME=/usr/lib/jvm/java-7-openjdk-amd64/jre
    else
        export JAVA_HOME=/usr/lib/jvm/java-7-openjdk-i386/jre
    fi

    PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools


# Gradle

PageTurner is now built using Gradle instead of Maven. If you want to use a local Gradle version, make sure it's at least version 2.1. The preferred way is to run the Gradle wrapper. This will automatically download the correct version of gradle to your system.

Run the Gradle wrapper by running

    gradlew

# Build PageTurner
Once everything is in place you can build PageTurner and install it on your device with 

    gradlew build
    gradlew installDebug

Eclipse
-------

*Note:* Building PageTurner in Eclipse is discouraged. We recommend using IntelliJ IDEA Community Edition instead, it imports Maven projects out of the box.

To use PageTurner in Eclipse, there are 2 options:

#The easy way
=======

The recommended way to build PageTurner in Eclipse is using the m2e-android plugin.

There is a screencast here that shows how to set up your environment: http://www.youtube.com/watch?v=jhSvwpwPFoY

#The hard way

You can follow these steps to only use Maven for dependencies:

1.   Download and unpack the sources        
2.   Run    

        mvn -DexcludeTransitive=true dependency:copy-dependencies
        
     inside the source folder
3.   Create a libs folder
4.   Copy all JAR files in target/dependency to the libs folder
5.   There are also .apklib files in the target/dependency folder. 
     Rename these to .zip, and unpack them. These are library projects,
     and you can add them as a normal library project now.
6.   In Eclipse, select "New Android Project" -> "From existing source" and
     point it to the folder you unpacked PageTurner in.
