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

import org.slf4j.LoggerFactory

import com.rabbitmq.client.{ConnectionFactory,ConnectionParameters,Channel}
import com.rabbitmq.client.{QueueingConsumer, ShutdownSignalException}

trait MQHandler {
  def handleRequest(msg: Array[Byte]): Boolean
}

object MQChannel {
  def apply(exchangeName: String, queueName: String,
    routingKey: String): Channel
    = this(exchangeName, queueName, routingKey, "localhost", 5672)

  def apply(exchangeName: String, queueName: String, routingKey: String,
    host: String, port: Int) = {
    val durable = true

    val params = new ConnectionParameters()

    val conFactory = new ConnectionFactory(params)

    val conn = conFactory.newConnection(host, port)
    val channel = conn.createChannel()

    channel.exchangeDeclare(exchangeName, "direct", durable)
    channel.queueDeclare(queueName, durable)
    channel.queueBind(queueName, exchangeName, routingKey)

    channel
  }
}

class NotificationListener(channel: Channel, handler :MQHandler,
  queueName: String) extends Thread {
  val logger = LoggerFactory.getLogger(getClass)
  private[this] var consumer: QueueingConsumer = _

  override def run() = {
    consumer = new QueueingConsumer(channel)
    channel.basicConsume(queueName, false, consumer)

    while (consumer.getChannel.isOpen) {
      try {
        val delivery = consumer.nextDelivery()
        logger.debug("Received new request")
        val handled = handler.handleRequest(delivery.getBody)
        if (handled) {
          channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false)
        }
      } catch {
        case e: ShutdownSignalException => // Do nothing
        case e: InterruptedException => // Do nothing
        case e => logger.error("Error while handling new message", e)
      }
    }
  }

  def close() = consumer.getChannel.close(0, "application termination")
}

