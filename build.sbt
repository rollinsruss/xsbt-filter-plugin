import sbt.SettingKey

sbtPlugin := true

name := "xsbt-filter"

organization := "org.xsbtfilter"

version := "0.2"

publishMavenStyle := true

logLevel := Level.Debug

seq( org.xsbtfilter.SbtFilterPlugin.settings : _*)

//currentFilterEnvSetting := "development" //or whatever filter you're using


//procFilterResources := filterResources

//procFilterResources <<= filterResources.dependsOn(copyResources)