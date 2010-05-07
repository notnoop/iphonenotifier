import sbt._

class NotifierProject(info: ProjectInfo) extends DefaultProject(info) {

  val notnoopRepo = "Notnoop Repo" at "http://notnoop.github.com/m2-repo"
  val lagRepo = "Lag Repo" at "http://www.lag.net/repo/"

  val javaapns = "com.notnoop.apns" % "apns" % "0.1.4"

  val configgy = "net.lag" % "configgy" % "1.5"
  val logback = "ch.qos.logback" % "logback-classic" % "0.9.17"
}

