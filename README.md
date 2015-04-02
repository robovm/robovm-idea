RoboVM Intellij IDEA Plugin
===========================
An IDEA plugin not based on the Gradle, but fully integrated with the compiler & debugger toolchain.

### Development
* Install the latest Intellij IDEA Community Edition 14 under /Applications/Intellij IDEA 14 CE.app/
* Install this plugin, it allows us to use Maven for plugin development https://plugins.jetbrains.com/plugin/7127?pr=
* Clone this repo https://github.com/JetBrains/intellij-community.git
* Checkout the branch that corresponds to the respective IDEA version you installed, e.g. 139 for Idea 14.0.x, see http://www.jetbrains.org/pages/viewpage.action?pageId=983225
* Open Intellij IDEA CE 14, setup the IDEA sdk pointing it at your IDEA installation. Also add the JARs from `plugins/maven/lib` and `plugins/gradle/lib`
* Open the project by selecting it's POM. Make sure to use the development Maven profile! It will be recognized as a plugin project
* Open File -> Project Structure, Click on the Project menu entry, and select the IDEA sdk under Project SDK
* In the same dialog, click on Modules, select the org.robovm.idea module, then click the Plugin Deployment tab, and make sure the Path to META-INF/plugin.xml ends with src/resources
* Click OK
* Optionally import the robovm-compiler, robovm-debug-XXX projects as modules. IDEA will resolve the dependencies to those modules so you can debug and code hotswap the compiler and debugger!
* Create a new run configuration, using the Plugin run config type. Set the module. Happy coding.

### Packaging
Simply do `mvn clean package -Pdeployment` in the root dir. You'll end up with a zip in the `target/` folder to which you can point Intellij IDEA.
