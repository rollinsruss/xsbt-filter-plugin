package org.xsbtfilter

import sbt._
import Keys._

import sbt.Load.BuildStructure
import sbt.CommandSupport._
import sbt.complete._
import sbt.complete.Parsers._
import collection.Seq
import io.Source
import Project.Initialize
import java.util.{Enumeration, Properties => JProperties}
import scala.collection.immutable._
import xsbti.api.Path
import java.io._

object SbtFilterPlugin extends Plugin {
  val FilterResources = config("filter-resources")

  val filterResources = TaskKey[Unit]("filter-resources", "filters files and replaces values using maven-style format ${}")
  //TODO provide more strict coupling to filter definitions and environment definitions, see README
  val currentFilterEnvSetting = SettingKey[String]("current-filter-env")

  //val filterExcludeFiles = SettingKey[PathFinder => PathFinder]("filter-exclude-files")
  val filterIncludeExtensions = SettingKey[Seq[String] => Seq[String]]("filter-include-extensions")

  private def loadProperties(propFile: Reader): JProperties = {
    val loeadedProps = new JProperties
    loeadedProps.load(propFile)
    propFile.close()
    loeadedProps
  }

  private def filterResourcesTask: Initialize[Task[Unit]] =
    (currentFilterEnvSetting, filterIncludeExtensions, baseDirectory, streams, target) map {
      (curFilterEnvSetting, filterIncExts, baseDirectory, streamss, target) =>

        def filterPath = baseDirectory / "src" / "main" / "resources" / "filters"
        def filterSources = filterPath * "*.properties"
        streamss.log.info(filterPath.toString)



        val envPropertyFilesMap = HashMap[String, String]() ++ filterSources.getPaths.map(path => {
          val pathFile: java.io.File = new java.io.File(path)
          pathFile.getName.split("\\.")(0) -> pathFile.absolutePath
        })
        streamss.log.info(envPropertyFilesMap.toString)
        val envProps = loadProperties(new BufferedReader(new FileReader(envPropertyFilesMap(curFilterEnvSetting))))
        streamss.log.info("Using the following properties for filtering " + envProps.toString)

        //TODO add sbt-related property values
        def sbtBaseProps: Map[String, String] = {
          Map(
            "project.name" -> "TODO", // projectName.value,
            "project.name" -> "TODO", //projectName.value,
            "project.organization" -> "TODO", //projectOrganization.value,
            "project.version" -> "TODO", //projectVersion.value.toString,
            "build.scala.versions" -> "TODO", //buildScalaVersions.value,
            "filter.env" -> currentFilterEnv)
        }

        //finalize replacement values, where file def overrides base props
        val replacements: scala.collection.mutable.Map[String, String] = scala.collection.mutable.Map() ++ sbtBaseProps
        implicit def javaEnumeration2Iterator[A](e: Enumeration[A]) = new Iterator[A] {
          def next = e.nextElement

          def hasNext = e.hasMoreElements
        }
        //better way to do this?
        envProps.propertyNames.foreach(key => {
          replacements += (key.toString -> envProps.getProperty(key.toString))
        })
        streamss.log.info("Master replacement list for filtering " + replacements.toString)

        //actual processing happens here, should put it in more appropriate location for readability
        if (filterPath.exists) {
          //TODO need the proper output path, what setting to reference?
          val targetPath: java.io.File = target / "scala-2.8.1.final" / "classes"
          streamss.log.info("Filtering target path " + targetPath.getAbsolutePath)

          (targetPath * "*").get.foreach(filterResource)
        } else {
          streamss.log.error(filterPath.toString + " is missing!!")
        }

        //TODO extract this to take a map and single file
        def filterResource(targetFile: java.io.File): Unit = {
          streamss.log.info("Filtering file " + targetFile.getAbsolutePath)
          //ensure file extension is in inclusion list
          if (targetFile.isFile) {
            val buf = new StringWriter
            val in = Source.fromFile(targetFile)
            in.getLines.foreach(l => {
              var line = l
              //is there a more efficient way to do this?
              replacements foreach {
                case (key, value) =>
                  line = line.replaceAll("\\$\\{\\s*" + key.toString + "\\s*\\}", value)
              }
              buf.write(line)
            })

            val out = new PrintWriter(targetFile)
            out.print(buf.toString)
            out.close()
            streamss.log.info("Done filtering")
          }
        }
    }


  //describedAs "Filter the main resource files for variable substitutions."
  /**
   * default file extensions to include
   */
  private def filterIncludeExtensions(base: Seq[String]) = {
    Seq(".properties", ".xml")
  }

  /**
   * default environment is dev
   */
  private def currentFilterEnv = {
    "development"
  }

  override lazy val settings = Seq(
    filterResources <<= filterResourcesTask,
    currentFilterEnvSetting := currentFilterEnv,
    filterIncludeExtensions := filterIncludeExtensions _
  )


}
