package org.scalatest

import izumi.distage.testkit.services.dstest.DistageTestRunner.TestStatus.{Done, Finished, Ignored}
import izumi.distage.testkit.services.dstest.DistageTestRunner.{SuiteData, TestMeta, TestReporter, TestStatus}
import izumi.fundamentals.platform.language.Quirks

import scala.collection.mutable

class SafeTestReporter(underlying: TestReporter) extends TestReporter {
  private val delayedReports = mutable.LinkedHashMap.empty[TestMeta, TestStatus]
  private var signalled: Option[TestMeta] = None
  private var openSuite: Option[SuiteData] = None

  override def onFailure(f: Throwable): Unit = synchronized {
    endAll()
    underlying.onFailure(f)
  }

  override def endAll(): Unit = synchronized {
    finish()
    finishOpen()
    underlying.endAll()
  }

  override def beginSuite(id: SuiteData): Unit = synchronized {
    Quirks.discard(id)
  }

  override def endSuite(id: SuiteData): Unit = synchronized {
    Quirks.discard(id)
  }

  override def testStatus(test: TestMeta, testStatus: TestStatus): Unit = synchronized {
    testStatus match {
      case TestStatus.Scheduled =>
        reportStatus(test, testStatus)

      case TestStatus.Running =>
        signalled match {
          case Some(_) =>
          case None =>
            reportStatus(test, testStatus)
            signalled = Some(test)
        }

      case _: Ignored =>
        signalled match {
          case None =>
            reportStatus(test, TestStatus.Running)
            reportStatus(test, testStatus)
          case Some(_) =>
            delayedReports.put(test, testStatus)
        }

      case _: Finished =>
        signalled match {
          case Some(value) if value == test =>
            finishTest(test, testStatus)
            signalled = None
            finish()
          case _ =>
            delayedReports.put(test, testStatus)
        }
    }
    ()
  }

  private def reportStatus(test: TestMeta, testStatus: TestStatus): Unit = synchronized {
    val fakeSuite = SuiteData(test.id.suiteName, test.id.suiteId, test.id.suiteClassName)

    nextSuite(fakeSuite)
    underlying.testStatus(test, testStatus)
  }

  private val fakeSuites = false
  private def nextSuite(fakeSuite: SuiteData): Unit = {
    if (fakeSuites) {
      if (!openSuite.contains(fakeSuite)) {
        finishOpen()
        underlying.beginSuite(fakeSuite)
        openSuite = Some(fakeSuite)
      }
    }
  }

  private def finishOpen(): Unit = {
    if (fakeSuites) {
      openSuite.foreach(underlying.endSuite)
    }
  }

  private def finish(): Unit = {
    val finishedTests = delayedReports.collect { case (t, s: Done) => (t, s) }

    finishedTests
      .foreach {
        case (t, s) =>
          reportStatus(t, TestStatus.Running)
          finishTest(t, s)
      }
  }

  private def finishTest(test: TestMeta, testStatus: TestStatus): Option[TestStatus] = {
    reportStatus(test, testStatus)
    delayedReports.remove(test)
  }
}
