import sbt.SettingKey

sbtPlugin := true

name := "xsbt-filter"

organization := "org.xsbtfilter"

version := "0.3"

publishMavenStyle := true

logLevel := Level.Debug

//uncomment for local testing
//seq( org.xsbtfilter.SbtFilterPlugin.settings : _*)

//filterEnv := "development" //or whatever filter you're using


//procFilterResources := filterResources

//procFilterResources <<= filterResources.dependsOn(copyResources)