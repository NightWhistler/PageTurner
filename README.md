PageTurner, the synchronizing ebook reader for Android
========================================================

PageTurner is a free, open-source ebook reader that allows you to keep your reading progress synchronized across multiple devices. This means you can read a few pages on your phone, than grab your tablet continuing where you left off.

[PageTurner Reader](https://play.google.com/store/apps/details?id=net.nightwhistler.pageturner.ads&feature=search_result#?t=W251bGwsMSwxLDEsIm5ldC5uaWdodHdoaXN0bGVyLnBhZ2V0dXJuZXIuYWRzIl0. "PageTurner available for free") is available for free from the Google Play Store. [PageTurner Pro](https://play.google.com/store/apps/details?id=net.nightwhistler.pageturner.pro&feature=more_from_developer#?t=W251bGwsMSwxLDEwMiwibmV0Lm5pZ2h0d2hpc3RsZXIucGFnZXR1cm5lci5wcm8iXQ. "PageTurner Pro paid removes ads"), a paid version, also available from the Google Play Store, removes ads.

See http://www.pageturner-reader.org/ for more info and some screenshots.

PageTurner is licensed under the GPL-V3 license.

Building PageTurner
-------------------

# Install Java
*   On Ubuntu

        sudo apt-get install openjdk-7-jdk
*   On Windows install the JDK from http://www.oracle.com/technetwork/java/javase/downloads/index.html

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

# Install Maven 3

1. Download Maven 3 tarball http://maven.apache.org/download.html
2. Puth maven/bin on your path

# Example PATH setup in .bashrc

    export ANDROID_HOME=$HOME/projects/adt-bundle-linux/sdk/
    if [ $(uname -m) == 'x86_64' ]; then
        export JAVA_HOME=/usr/lib/jvm/java-7-openjdk-amd64/jre
    else
        export JAVA_HOME=/usr/lib/jvm/java-7-openjdk-i386/jre
    fi

    PATH=$HOME/projects/apache-maven-3.0.4/bin:$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools

# Build PageTurner
Once everything is in place you can build PageTurner and install it on your device with 

    mvn clean install
    mvn android:deploy

# Maven HTTPS SSL

This section is not necessary for building PageTurner. PageTurner uses a repository which is accessed through HTTPS using a self-signed certificate. 
A trust-store is included in trust.jks, password 'pageturner'

To use it set the MAVEN_OPTS environment option or change your .mavenrc file to:

    MAVEN_OPTS="-Djavax.net.ssl.trustStore=trust.jks -Djavax.net.ssl.trustStorePassword=pageturner"
    export MAVEN_OPTS

See http://maven.apache.org/guides/mini/guide-repository-ssl.html for more details.



Eclipse
-------

You can either use the Maven Eclipse and Maven Eclipse Android plugin

OR

You can follow these steps to only use Maven for dependencies:

1.   Download and unpack the sources        
2.   Run    

        mvn -Djavax.net.ssl.trustStore=trust.jks 
            -Djavax.net.ssl.trustStorePassword=pageturner
            -DexcludeTransitive=true
            dependency:copy-dependencies
        
     inside the source folder
3.   Create a libs folder
4.   Copy all JAR files in target/dependency to the libs folder
5.   There are also .apklib files in the target/dependency folder. 
     Rename these to .zip, and unpack them. These are library projects,
     and you can add them as a normal library project now.
6.   In Eclipse, select "New Android Project" -> "From existing source" and
     point it to the folder you unpacked PageTurner in.