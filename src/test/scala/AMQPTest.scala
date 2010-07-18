/*
 * Copyright 2010, Mahmood Ali.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above
 *     copyright notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *   * Neither the name of Mahmood Ali. nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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

