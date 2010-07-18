package com.notnoop.notifier

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.concurrent.Conductor

import com.rabbitmq.client._

trait MQSender {
  val hostname: String
  val exchangeName: String
  val routing: String

  val channel = {
    val params = new ConnectionParameters
    val conFactory = new ConnectionFactory(params)
    val connection = conFactory.newConnection(hostname)
    val channel = connection.createChannel()
    channel
  }

  def sendMessage(str: String) = {
    channel.basicPublish(exchangeName, routing,
      MessageProperties.TEXT_PLAIN, str.getBytes("UTF-8"))
  }

  def sendMessages(list: Iterable[String]) =
    for (msg <- list) sendMessage(msg)

  def close() = channel.close()
}

class MQChannelSpec extends Spec
  with ShouldMatchers with MQSender {
  val hostname = "dev.rabbitmq.com"
  val port = 5672
  val exchangeName = "notnoop-amqp"
  val routing = "notnoop-queue"

  class Listener extends MQHandler {
    val semaphore = new java.util.concurrent.Semaphore(0)
    val received = scala.collection.mutable.HashSet[String]()
    def handleRequest(msg: Array[Byte]) = {
      received += new String(msg)
      semaphore.release()
      true
    }
    def waitFor(i: Int) = semaphore.acquire(i)
  }

  def newNotificationQueue(listener: Listener) =
    new NotificationListener(
        MQChannel(exchangeName, routing, routing, hostname, port),
        listener, routing)

  describe("MQChannel") {
    it("receives sent messages") {
      val listener = new Listener
      val queue = newNotificationQueue(listener)
      queue.start()

      val msgs = Set("123", "321")
      sendMessages(msgs)

      listener.waitFor(msgs.size)
      queue.close()

      listener.received should be(msgs)
    }

    it("receives messages sent before start") {
      val msgs = Set("987", "789", "765")
      sendMessages(msgs)

      val listener = new Listener
      val queue = newNotificationQueue(listener)
      queue.start()

      listener.waitFor(msgs.size)
      queue.close()

      listener.received should be(msgs)
    }

    it("survives across restarts") {
      val listener = new Listener
      val firstQueue = newNotificationQueue(listener)
      firstQueue.start()

      val firstBatch = Set("123", "321")
      val secondBatch = Set("456", "654")

      sendMessages(firstBatch)

      listener.waitFor(firstBatch.size)
      firstQueue.close()

      listener.received should be(firstBatch)

      sendMessages(secondBatch)

      val secondQueue = newNotificationQueue(listener)
      secondQueue.start()

      listener.waitFor(secondBatch.size)
      secondQueue.close()

      listener.received should be(firstBatch ++ secondBatch)
    }
  }
}

