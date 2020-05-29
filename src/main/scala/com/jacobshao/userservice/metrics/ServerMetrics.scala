package com.jacobshao.userservice.metrics

import java.util.concurrent.TimeUnit

import monix.eval.Task
import nl.grons.metrics4.scala.DefaultInstrumented
import org.http4s.metrics.{MetricsOps, TerminationType}
import org.http4s.{Method, Status}

object ServerMetrics extends MetricsOps[Task] with DefaultInstrumented {

  private[this] val activeRequestsMeter = metrics.counter("active-requests")
  private[this] val headersTimer = metrics.timer("headers-time")
  private[this] val totalTimer = metrics.timer("total-time")

  override def increaseActiveRequests(classifier: Option[String]): Task[Unit] = Task.now(activeRequestsMeter.inc())

  override def decreaseActiveRequests(classifier: Option[String]): Task[Unit] = Task.now(activeRequestsMeter.dec())

  override def recordHeadersTime(method: Method, elapsed: Long, classifier: Option[String]): Task[Unit] =
    Task.now(headersTimer.update(elapsed, TimeUnit.NANOSECONDS))

  override def recordTotalTime(method: Method, status: Status, elapsed: Long, classifier: Option[String]): Task[Unit] =
    Task.now(totalTimer.update(elapsed, TimeUnit.NANOSECONDS))

  override def recordAbnormalTermination(elapsed: Long, terminationType: TerminationType, classifier: Option[String]): Task[Unit] =
    Task.unit
}
