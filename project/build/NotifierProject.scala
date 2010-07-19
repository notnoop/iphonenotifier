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
import sbt._

class NotifierProject(info: ProjectInfo) extends DefaultProject(info) {

  val notnoopRepo = "Notnoop Repo" at "http://notnoop.github.com/m2-repo"
  val lagRepo = "Lag Repo" at "http://www.lag.net/repo/"

  val rabbitmq = "com.rabbitmq" % "amqp-client" % "1.7.2"

  val javaapns = "com.notnoop.apns" % "apns" % "0.1.4"
  val json_jackson = "org.codehaus.jackson" % "jackson-mapper-asl" % "1.4.0"

  val configgy = "net.lag" % "configgy" % "1.5"

  val scalatest = "org.scalatest" % "scalatest" % "1.0" % "test"

  // For Configgy Logger
  val slf4j_configgy = "com.notnoop.logging" % "slf4j-configgy" % "0.0.1"
  override def ivyXML =
    <dependencies>
      <exclude module="logback-classic"/>
    </dependencies>

  // For Logback Logger
//  val logback = "ch.qos.logback" % "logback-classic" % "0.9.17"
}

