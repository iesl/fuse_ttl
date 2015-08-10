import sbt._
import Keys._
import sbtassembly.Plugin._
import AssemblyKeys._

object Build extends sbt.Build {
  import Dependencies._

  lazy val overrideSettings = {
    lazy val publishSetting = publishTo <<= (version) {
      version: String =>
        def repo(name: String) = name at "https://dev-iesl.cs.umass.edu/nexus/content/repositories/" + name
      val isSnapshot = version.trim.endsWith("SNAPSHOT")
      val repoName = if(isSnapshot) "snapshots" else "releases"
      Some(repo(repoName))
    }
    
    lazy val credentialsSetting = credentials += {
      Seq("build.publish.user", "build.publish.password").map(k => Option(System.getProperty(k))) match {
        case Seq(Some(user), Some(pass)) =>
          Credentials("Sonatype Nexus Repository Manager", "iesl.cs.umass.edu", user, pass)
        case _ =>
          Credentials(Path.userHome / ".ivy2" / ".credentials")
      }
    }
  }

  val NoNLP = config("no-nlp-resources") extend(Runtime)
  val WithNLP = config("with-nlp-resources") extend(Runtime)

  lazy val factorie = Project("factorie", file(".")).
    configs(NoNLP, WithNLP).
    settings(
      organization := "cc.factorie",
      version := "1.1-SNAPSHOT",
      scalaVersion := "2.10.4",
      scalacOptions := Seq("-deprecation", "-unchecked", "-encoding", "utf8"),
      resolvers ++= Dependencies.resolutionRepos,
      libraryDependencies ++= Seq(
        Compile.mongodb,
        Compile.colt,
        Compile.compiler,
        Compile.junit,
        Compile.acompress,
        Compile.snappy,
        Compile.bliki,
        Test.scalatest
      )
    ).
    settings(inConfig(NoNLP)(Classpaths.configSettings ++ Defaults.defaultSettings ++ baseAssemblySettings ++ Seq(
      test in assembly := {},
      target in assembly <<= target,
      assemblyDirectory in assembly := cacheDirectory.value / "assembly-no-nlp-resources",
      jarName in assembly := "%s-%s-%s" format (name.value, version.value, "jar-with-dependencies.jar")
    )): _*).
    settings(inConfig(WithNLP)(Classpaths.configSettings ++ Defaults.defaultSettings ++ baseAssemblySettings ++ Seq(
      test in assembly := {},
      target in assembly <<= target,
      assemblyDirectory in assembly := cacheDirectory.value / "assembly-with-nlp-resources",
      jarName in assembly := "%s-%s-%s" format (name.value, version.value, "nlp-jar-with-dependencies.jar"),
      libraryDependencies ++= Seq(Resources.nlpresources)
    )): _*)
}

object Dependencies {
  val resolutionRepos = Seq(
    "IESL repo" at "https://dev-iesl.cs.umass.edu/nexus/content/groups/public/",
    "Scala tools" at "https://oss.sonatype.org/content/groups/scala-tools",
    "Third party" at "https://dev-iesl.cs.umass.edu/nexus/content/repositories/thirdparty/",
    //"IESL releases" at "https://dev-iesl.cs.umass.edu/nexus/content/repositories/releases/",
    "IESL snapshots" at "https://dev-iesl.cs.umass.edu/nexus/content/repositories/snapshots/"
  )
  
  object Compile {
    val mongodb  = "org.mongodb" % "mongo-java-driver" % "2.11.1"
    val colt = "org.jblas" % "jblas" % "1.2.3"
    val compiler = "org.scala-lang" % "scala-compiler" % "2.10.4"
    val junit = "junit" % "junit" % "4.10"
    val acompress = "org.apache.commons" % "commons-compress" % "1.8"
    val snappy = "org.xerial.snappy" % "snappy-java" % "1.1.1.3"
    val bliki = "info.bliki.wiki" % "bliki-core" % "3.0.19"
  }

  object Test {
    val scalatest = "org.scalatest" % "scalatest_2.10" % "2.2.2" % "test"
  }

  object Resources {
    // This may be brittle, but intransitive() avoids creating a circular dependency.
    val nlpresources = "cc.factorie.app.nlp" % "all-models" % "1.0.0" % "with-nlp-resources" intransitive()
  }
}
