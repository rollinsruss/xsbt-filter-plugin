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
import java.io._

object SbtFilterPlugin extends Plugin {
  val FilterResources = config("filter-resources")

  val filterResources = TaskKey[Unit]("filter-resources", "filters files and replaces values using maven-style format ${}")
  //TODO provide more strict coupling to filter definitions and environment definitions, see README
  val currentFilterEnvSetting = SettingKey[String]("current-filter-env")

  //val filterExcludeFiles = SettingKey[PathFinder => PathFinder]("filter-exclude-files")
  val filterIncludeExtensions = SettingKey[Seq[String] => Seq[String]]("filter-include-extensions")

  private def loadProperties(propFile:Reader): JProperties = {
    val loeadedProps = new JProperties
    loeadedProps.load(propFile)
    propFile.close()
    loeadedProps
  }

  private def filterResourcesTask: Initialize[Task[Unit]] =
  (currentFilterEnvSetting,filterIncludeExtensions, baseDirectory,streams) map {
      (curFilterEnvSetting, filterIncExts, baseDirectory, streamss) =>

    def filterPath = baseDirectory / "src"/ "main" / "resources" / "filters"
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

//    if (filterPath.exists) {
//      (mainResourcesOutputPath * "*").getFiles.foreach(x => filterResource(filterSources.get, x))
//    } else {
//      log.warn(filterPath.toString + " is missing")
//    }


//    streamss.log.info(SettingKey[String]("current-filter-env").toString)
//    streamss.log.info(SettingKey[java.io.File]("base-directory").toString)

    //streamss.log.info(filterPath.getClass.toString)

    //    def filterResource(filterFilePaths: Iterable[Path], dest: File): Unit = {


    //
    //      def substitute(prop: JProperties, f: File) = {
    //        val replacements: scala.collection.mutable.Map[String, String] = scala.collection.mutable.Map() ++ sbtBaseProps
    //
    //        //props file precedence is greater than sbt defaults
    //        prop.propertyNames.foreach(key => {
    //          replacements += (key.toString -> prop.getProperty(key.toString()))
    //        })
    //
    //        if (f.isFile) {
    //          val buf = new StringWriter
    //          val in = Source.fromFile(f)
    //          in.getLines.foreach(l => {
    //            var line = l
    //            //is there a more efficient way to do this?
    //            replacements foreach {
    //              case (key, value) =>
    //                line = line.replaceAll("\\$\\{\\s*" + key.toString + "\\s*\\}", value)
    //            }
    //            buf.write(line)
    //          })
    //
    //          val out = new PrintWriter(f)
    //          out.print(buf.toString)
    //          out.close()
    //
    //          //for reference purposes it might be helpful to store a master list of
    //          //the properties that were used for filtering
    //          //not sure where that would be ideally dumped
    //        }
    //      }
    //
    //      substitute(envProps, dest)
    //}


    //TODO add another property value the specify the list of extensions to include in filter replacement( .properties, .xml, .conf)


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
