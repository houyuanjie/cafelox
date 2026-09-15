import sbt.*

object Dependencies:
  object Versions:
    lazy val munit = "1.3.6"

  lazy val munit = "org.scalameta" %% "munit" % Versions.munit
