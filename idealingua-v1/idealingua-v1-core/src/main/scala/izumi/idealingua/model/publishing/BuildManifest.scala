package izumi.idealingua.model.publishing

import izumi.fundamentals.reflection.ProjectAttributeMacro
import izumi.idealingua.model.publishing.BuildManifest._

trait BuildManifest {
  def common: Common
}

case class ProjectVersion(version: String, release: Boolean, snapshotQualifier: String) {
  override def toString: String = {
    val snapshot = if (release) "" else s"-$snapshotQualifier"
    s"$version$snapshot"
  }
}

object ProjectVersion {
  def default = ProjectVersion("0.0.1", release = false, "UNSET-BUILD-ID")
}

case class ProjectNamingRule(
                              prefix: Seq[String],
                              /**
                                * Positive value will work as .drop on fully qualified module name
                                * Zero value will leave name untouched
                                * Negative value will work as .takeRight
                                *
                                * Does not apply for layout == PLAIN
                                */
                              dropFQNSegments: Option[Int],
                              postfix: Seq[String],
                            )

object ProjectNamingRule {
  def example = ProjectNamingRule(
    prefix = Seq("company", "example", "library"),
    dropFQNSegments = Some(-1),
    postfix = Seq("api"),
  )
}

object BuildManifest {

  case class Common(
                     name: String,
                     group: String,
                     tags: List[String],
                     description: String,
                     releaseNotes: String,
                     publisher: Publisher,
                     version: ProjectVersion,
                     licenses: List[License],
                     website: MFUrl,
                     copyright: String,
                     izumiVersion: String,
                   )

  object Common {
    final val example = Common(
      name = "idealingua-v1-project",
      group = "com.mycompany.generated",
      tags = List.empty,
      description = "Generated by Izumi IDL Compiler",
      releaseNotes = "",
      publisher = Publisher("MyCompany", "com.my.company"),
      version = ProjectVersion.default,
      licenses = List(License("MIT", MFUrl("https://opensource.org/licenses/MIT"))),
      website = MFUrl("http://project.website"),
      copyright = "Copyright (C) Test Inc.",
      izumiVersion = ProjectAttributeMacro.extractSbtProjectVersion().getOrElse("UNSET-IZUMI-VERSION"),
    )
  }

  final case class ManifestDependency(module: String, version: String)

  final case class License(name: String, url: MFUrl)

  final case class MFUrl(url: String)
}
