package org.xsbtfilter

import sbt._
import Keys._

import collection.Seq
import io.Source
import Project.Initialize
import java.util.{Enumeration, Properties => JProperties}
import scala.collection.immutable._
import java.io._
import scala.collection.JavaConverters._


object SbtFilterPlugin extends Plugin {
  implicit def javaEnumeration2Iterator[A](e: Enumeration[A]) = new Iterator[A] {
    def next = e.nextElement
    def hasNext = e.hasMoreElements
  }

  val FilterResources = config("filter-resources") extend (Compile)
  val filterResources = TaskKey[Unit]("filter-resources", "filters resource files and replaces values using the maven-style format of ${}")

  //TODO provide more strict coupling to filter definitions and environment definitions, see README
  val filterEnv = SettingKey[String]("current-filter-env")

  //val filterExcludeFiles = SettingKey[PathFinder => PathFinder]("filter-exclude-files")
  val filterIncludeExtensions = SettingKey[List[String] => List[String]]("filter-include-extensions")

  private def loadProperties(propFile: Reader): JProperties = {
    val loadedProps = new JProperties
    loadedProps.load(propFile)
    propFile.close()
    loadedProps
  }

  private def filterResourcesTask: Initialize[Task[Unit]] =
    (classDirectory, filterEnv, filterIncludeExtensions, baseDirectory, streams, name, organization, version, scalaVersion) map {
      (classDir, curFilterEnvSetting, filterIncExts, baseDirectory, streams, name, organization, version, scalaVersion) =>
        log.info("Filtering for environment: " + curFilterEnvSetting)

        //TODO hard-coding this for now, should implement a setting for defining paths for processing
        def filterPath = baseDirectory / "src" / "main" / "filters"
        def filterSources = filterPath * "*.properties"
        def log = streams.log

        log.debug(filterPath.toString)

        val envPropertyFilesMap = filterSources.get.map(file => (file.base, file)).toMap

        log.debug("Master property replacement list: " + envPropertyFilesMap.toString)
        val envProps = loadProperties(new BufferedReader(new FileReader(envPropertyFilesMap(curFilterEnvSetting))))
        log.debug("Using the following properties for filtering " + envProps.toString)

        //TODO add sbt-related property values
        def sbtBaseProps: Map[String, String] = {
          Map(
            "project.name" -> name, // projectName.value,
            "project.organization" -> organization, //projectOrganization.value,
            "project.version" -> version, //projectVersion.value.toString,
            "build.scala.version" -> scalaVersion, //buildScalaVersions.value,
            "filter.env" -> curFilterEnvSetting)
        }

        //finalize replacement values, where file def overrides base props
        val replacements: scala.collection.mutable.Map[String, String] = scala.collection.mutable.Map() ++ sbtBaseProps

        //better way to do this?
        envProps.propertyNames.foreach(key => {
          replacements += (key.toString -> envProps.getProperty(key.toString))
        })
        log.debug("Master replacement list for filtering " + replacements.toString)

        //actual processing happens here, should put it in more appropriate location for readability
        if (filterPath.exists) {
          //TODO need the proper output path, what setting to reference?
          val targetPath: java.io.File = classDir

          log.debug("Filtering target path " + targetPath.getAbsolutePath)

          //TODO implement some greatly improved matcher on inclusion list
          (targetPath * ("*.properties" | "*.xml")).get.foreach(filterResource)
        } else {
          log.error(filterPath.toString + " is missing!!")
        }

        //TODO extract this to take a map and single file
        def filterResource(targetFile: java.io.File): Unit = {
          val fileName: String = targetFile.getName

          if (targetFile.isFile) {
            log.debug("Filtering file " + targetFile.getAbsolutePath)
            //TODO see Mark's suggestion for using IO here:
            //https://github.com/harrah/xsbt/blob/0.10/project/Transform.scala
            //http://groups.google.com/group/simple-build-tool/tree/browse_frm/thread/816a98ecee06ee6d/52e2fd7a6e598a2e?rnum=1&q=xsbt-filter&_done=%2Fgroup%2Fsimple-build-tool%2Fbrowse_frm%2Fthread%2F816a98ecee06ee6d%2F52e2fd7a6e598a2e%3Flnk%3Dgst%26q%3Dxsbt-filter%26#doc_9681b118d1f3a3a6
            val buf = new StringWriter
            val in = Source.fromFile(targetFile)
            in.getLines.foreach(l => {
              var line = l
              //TODO is there a more efficient way to do this?
              replacements foreach {
                case (key, value) =>
                  line = line.replaceAll("\\$\\{\\s*" + key.toString + "\\s*\\}", value)
              }
              //ensure newline at the end, was getting whacked during initial testing
              if (!line.contains("\n")) line = line + "\n" //newline was missing during initial testing
              buf.write(line)
            })

            val out = new PrintWriter(targetFile)
            out.print(buf.toString)
            out.close()
          }
        }
    }


  //describedAs "Filter the main resource files for variable substitutions."
  /**
   * default file extensions to include
   */
  private def filterIncludeExtensions(base: List[String]) = {
    List(".properties", ".xml")
  }

  val defineFilter: Seq[Setting[_]] = Seq(
    filterResources <<= filterResourcesTask triggeredBy (copyResources)

  )
  val filterBase: Seq[Setting[_]] = Seq(
    filterEnv := "development",
    filterIncludeExtensions := filterIncludeExtensions
  )

  override lazy val settings = filterBase ++
      inConfig(Compile)(defineFilter) ++
      inConfig(Test)(defineFilter ++ Seq(filterEnv := "test"))


}
