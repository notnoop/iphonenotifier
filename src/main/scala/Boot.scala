package com.notnoop.smartpush.notifier

import org.slf4j.LoggerFactory
import net.lag.configgy.Configgy

object Boot {
  val logger = LoggerFactory.getLogger(getClass)

  def configure(filename: Option[String]) = {
    filename match {
      case Some(path) => Configgy.configure(path)
      case None => Configgy.configureFromResource("notifier.conf")
    }

    Configgy.config
  }

  def main(args: Array[String]) {
    val config = configure(args.firstOption)

    val keyStore = config.getString("apns.keystore.path").get
    val password = config.getString("apns.keystore.pass").get

    val exchange = config.getString("amqp.exchange", "smartpush")
    val queue = config.getString("amqp.queue", "smartpush.notification")
    val routing = config.getString("amqp.routing", "smartpush.notification")

    val handler = new MQApnsHandler(new ApnsSender(keyStore, password))

    val listener = new NotificationListener(MQChannel(exchange, queue, routing), handler, queue)

    logger.debug("starting")
    listener.run()
  }
}

