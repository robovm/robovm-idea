RoboVM Intellij IDEA Plugin
===========================
An IDEA plugin not based on the Gradle, but fully integrated with the compiler & debugger toolchain.

### Development
* Install the latest Intellij IDEA Community Edition 14 under /Applications/Intellij IDEA 14 CE.app/
* Install this plugin, it allows us to use Maven for plugin development https://plugins.jetbrains.com/plugin/7127?pr=
* Clone this repo https://github.com/JetBrains/intellij-community.git
* Checkout the branch that corresponds to the respective IDEA version you installed, e.g. 139 for Idea 14.0.x, see http://www.jetbrains.org/pages/viewpage.action?pageId=983225
* Open Intellij IDEA CE 14, follow these instructions, you already completed step 1. Setup the IDEA sdk!
* Open the project by selecting it's POM. It will be recognized as a plugin project
* Open File -> Project Structure, Click on the Project menu entry, and select the IDEA sdk under Project SDK
* In the same dialog, click on Modules, select the org.robovm.idea module, then click the Plugin Deployment tab, and make sure the Path to META-INF/plugin.xml ends with src/resources
* Click OK
* Create a new run configuration, using the Plugin run config type. Set the module. Happy coding.

If you make changes to the compiler/debugger/rt, you have to package it on the CLI via Maven and install it to your repo. Here's a simple script:

```bash
#/bin/sh
cd robovm
mvn clean install -Dmaven.test.skip=true
cd ../robovm-lm
mvn clean install -Dmaven.test.skip=true
cd ../robovm-templates
mvn clean install -Dmaven.test.skip=true
cd ../robovm-debug
mvn clean install -Dmaven.test.skip=true
cd ../robovm-proprietary
mvn clean install -P development
cd ../robovm-dist
mvn -Pcommercial clean install -DlicenseTxtURL="file:///Users/badlogic/workspaces/robovm/robovm-proprietary/LICENSE.txt"
cd ../robovm-eclipse
mvn clean package
```

After that, refresh your Maven dependencies in IDEA.


### Packaging
Simply do `mvn clean package` in the root dir. You'll end up with a zip in the `target/` folder to which you can point Intellij IDEA.
