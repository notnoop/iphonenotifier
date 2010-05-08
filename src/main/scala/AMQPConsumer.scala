package com.notnoop.smartpush.notifier

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

