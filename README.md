xsbt-filter-plugin
==================
Just starting out, still a work in progress. Intent here is to create a plugin approaching equivalence to Maven's filtering prowess for SBT 0.10, using wyuenho's 1.0.3 release as a baseline (https://bitbucket.org/wyuenho/sbt-filter-plugin/wiki/Home).

Once the jar is built and deployed (sbt publish-local) by adding this line to your project/plugins/build.sbt:

`libraryDependencies += "org.xsbtfilter" %% "xsbt-filter" % "0.1"`

And this to your build.sbt at the project root:

`seq( org.xsbtfilter.SbtFilterPlugin.settings : _*)`

`currentFilterEnvSetting := "development" //or whatever filter you're using`


Then, create an src/main/resources/filters directory and add your filter definitions (eg, development.properties, test.properties, beta.properties, etc.)

##0.1
This release is a minimal release that supports filtering .properties and .xml files. It requires copy-resources being run before filter-resources.

Requires building with sbt-launch-0.10.jar
* TODO: fix the problem with changing currentFilterEnvSetting and requiring a clean before new replacement vals are present
* TODO: finalize sbt settings propagation in filtering
* TODO: filter test resources
* TODO: add dependency hooks
* TODO: List[String] dev environment definitions in proj properties
* TODO: runtime checking ensuring dev env filters present when task(s) executed
* TODO: dependsOn copy-resources
* TODO  enable common props dev where a project could define a Map[String,String] to union with the filter prop definitions (thus enabling common properties)
* TODO: ensure standalone operation
* TODO: inclusion list definition for file extension
* TODO: exclusion list definition for specific files
* TODO: automated sbt testing during plugin build
