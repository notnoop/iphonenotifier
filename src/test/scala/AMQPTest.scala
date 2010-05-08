package com.notnoop.smartpush.notifier

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
  }


  describe("MQChannel") {
    it("receives sent messages") {
      val listener = new Listener
      val queue = new NotificationListener(
          MQChannel(exchangeName, routing, routing, hostname, port),
          listener, routing)
      queue.start()

      sendMessage("123")
      sendMessage("321")

      listener.semaphore.acquire(2)
      queue.close()

      listener.received should contain("123")
      listener.received should contain("321")
      listener.received should have size(2)
    }

    it("receives messages sent before start") {
      sendMessage("987")
      sendMessage("789")
      sendMessage("765")

      val listener = new Listener
      val queue = new NotificationListener(
          MQChannel(exchangeName, routing, routing, hostname, port),
          listener, routing)
      queue.start()

      listener.semaphore.acquire(3)
      queue.close()

      listener.received should contain("765")
      listener.received should contain("987")
      listener.received should contain("789")
      listener.received should have size(3)
    }
  }
}

