package izumi.distage.plugins.load

import io.github.classgraph.ClassGraph
import izumi.distage.plugins.{PluginBase, PluginConfig, PluginDef}
import izumi.functional.Value

import scala.jdk.CollectionConverters._

class PluginLoaderDefaultImpl
(
  pluginConfig: PluginConfig,
) extends PluginLoader {
  def load(): Seq[PluginBase] = {
    val pluginBase = classOf[PluginBase]
    val pluginDef = classOf[PluginDef]
    val config = pluginConfig.copy(packagesEnabled = pluginConfig.packagesEnabled)

    val enabledPackages: Seq[String] = config.packagesEnabled.filterNot(config.packagesDisabled.contains)
    val disabledPackages: Seq[String] = config.packagesDisabled

    PluginLoaderDefaultImpl.load[PluginBase](pluginBase.getName, Seq(pluginDef.getName), enabledPackages, disabledPackages, config.debug)
  }
}

object PluginLoaderDefaultImpl {
  def load[T](base: String, whitelist: Seq[String], enabledPackages: Seq[String], disabledPackages: Seq[String], debug: Boolean): Seq[T] = {
    val scanResult = Value(new ClassGraph())
      .map(_.whitelistPackages(enabledPackages: _*))
      .map(_.whitelistClasses(whitelist :+ base: _*))
      .map(_.blacklistPackages(disabledPackages: _*))
      .map(_.enableMethodInfo())
      .map(if (debug) _.verbose() else identity)
      .map(_.scan())
      .get

    try {
      val implementors = scanResult.getClassesImplementing(base)
      implementors
        .asScala
        .filterNot(_.isAbstract)
        .flatMap {
          classInfo =>
            val clz = classInfo.loadClass()

            if (Option(clz.getSimpleName).exists(_.endsWith("$"))) {
              Seq(clz.getField("MODULE$").get(null).asInstanceOf[T])
            } else {
              clz.getDeclaredConstructors.find(_.getParameterCount == 0).map(_.newInstance().asInstanceOf[T]).toSeq
            }
        }
        .toSeq // 2.13 compat
    } finally {
      scanResult.close()
    }
  }
}
