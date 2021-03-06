package scalatest.adapter.specs

import izumi.distage.testkit.services.scalatest.adapter.DistagePluginTestSupport
import izumi.fundamentals.reflection.Tags.TagK
import org.scalatest.{ScalatestSuite, WordSpecLike}

@deprecated("Use dstest", "2019/Jul/18")
abstract class DistagePluginSpec[F[_]](implicit val tagMonoIO: TagK[F]) extends DistagePluginTestSupport[F] with WordSpecLike {

  override def toString: String = ScalatestSuite.suiteToString(None, this)
}


