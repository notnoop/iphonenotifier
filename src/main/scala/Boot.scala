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
    val mqHost = config.getString("amqp.hostname", "localhost")
    val mqPort = config.getInt("amqp.port", 5672)

    val handler = new MQApnsHandler(new ApnsSender(keyStore, password))

    val listener = new NotificationListener(
      MQChannel(exchange, queue, routing, mqHost, mqPort),
      handler, queue)

    logger.debug("starting")
    listener.run()
  }
}

