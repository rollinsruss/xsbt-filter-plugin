package org.xsbtfilter

import sbt._
import Keys._

import collection.Seq
import io.Source
import Project.Initialize
import java.util.{Enumeration, Properties => JProperties}
import scala.collection.immutable._
import java.io._

object SbtFilterPlugin extends Plugin {

  val FilterResources = config("filter-resources")extend(Runtime)

  val filterMainResources = TaskKey[Unit]("filter-resources", "filters main resource files and replaces values using the maven-style format of ${}")
  val filterTestResources = TaskKey[Unit]("filter-test-resources", "filters test resource files and replaces values using the maven-style format of ${}")

  //TODO provide more strict coupling to filter definitions and environment definitions, see README
  val currentFilterEnvSetting = SettingKey[String]("current-filter-env")

  //val filterExcludeFiles = SettingKey[PathFinder => PathFinder]("filter-exclude-files")
  val filterIncludeExtensions = SettingKey[List[String] => List[String]]("filter-include-extensions")

  private def loadProperties(propFile: Reader): JProperties = {
    val loadedProps = new JProperties
    loadedProps.load(propFile)
    propFile.close()
    loadedProps
  }

  private def filterResourcesTask: Initialize[Task[Unit]] =
    (classDirectory in Compile,currentFilterEnvSetting, filterIncludeExtensions, baseDirectory, streams) map {
      (classDir, curFilterEnvSetting, filterIncExts, baseDirectory, streams) =>
        log.info("Filtering for environment: "+curFilterEnvSetting )

        //hard-coding this for now, gotta be a better way
        def filterPath = baseDirectory / "src" / "main" / "resources" / "filters"
        def filterSources = filterPath * "*.properties"
        def log = streams.log
        log.debug(filterPath.toString)

        val envPropertyFilesMap = HashMap[String, String]() ++ filterSources.getPaths.map(path => {
          val pathFile: java.io.File = new java.io.File(path)
          pathFile.getName.split("\\.")(0) -> pathFile.absolutePath
        })

        log.debug("Master property replacement list: " + envPropertyFilesMap.toString)
        val envProps = loadProperties(new BufferedReader(new FileReader(envPropertyFilesMap(curFilterEnvSetting))))
        log.debug("Using the following properties for filtering " + envProps.toString)

        //TODO add sbt-related property values
        def sbtBaseProps: Map[String, String] = {
          Map(
            "project.name" -> "TODO", // projectName.value,
            "project.name" -> "TODO", //projectName.value,
            "project.organization" -> "TODO", //projectOrganization.value,
            "project.version" -> "TODO", //projectVersion.value.toString,
            "build.scala.versions" -> "TODO", //buildScalaVersions.value,
            "filter.env" -> curFilterEnvSetting)
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
        log.debug("Master replacement list for filtering " + replacements.toString)

        //actual processing happens here, should put it in more appropriate location for readability
        if (filterPath.exists) {
          //TODO need the proper output path, what setting to reference?
          val targetPath: java.io.File = classDir

          log.debug("Filtering target path " + targetPath.getAbsolutePath)

          (targetPath * "*").get.foreach(filterResource)
        } else {
          log.error(filterPath.toString + " is missing!!")
        }

        //TODO extract this to take a map and single file
        def filterResource(targetFile: java.io.File): Unit = {
          val fileName: String = targetFile.getName
          //TODO implement some greatly improved matcher on inclusion list
          //ensure file extension is in inclusion list
          //val extSplit: Array[String] = targetFile.getName.split(".")
          //val extension: String = extSplit.last
          if (targetFile.isFile && (fileName.contains(".properties") || fileName.contains(".xml"))) {
            //if (targetFile.isFile && filterIncExts.contains(extension)) {
            log.debug("Filtering file " + targetFile.getAbsolutePath)

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



  override lazy val settings = Seq(
    filterMainResources <<= filterResourcesTask triggeredBy(copyResources in Compile),
    currentFilterEnvSetting := "development",
    filterIncludeExtensions := filterIncludeExtensions,
    //filterTestResources <<= filterResourcesTask(classDirectory in Test,"test",null,baseDirectory in Compile,streams) triggeredBy(copyResources in Test) //map {x =>
    filterTestResources <<= filterResourcesTask triggeredBy(copyResources in Test),
    //testOptions in Test := (Seq(currentFilterEnvSetting := "test"))
    //compile in Test <<= (compile in Test) map {x =>
    copyResources in Test <<= (copyResources in Test) map {x=>

      currentFilterEnvSetting := "test"
      x
    }
    //update test settings here

      //seq(currentFilterEnvSetting := "test")
        //x.currentFilterEnvSetting = "test"
    //    currentFilterEnvSetting := "test"


    //  x
   // }

  )

}
