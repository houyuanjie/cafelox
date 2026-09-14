import sbt.*

object Dependencies:
  object Versions:
    val munit = "1.3.6"

  lazy val munit = "org.scalameta" %% "munit" % Versions.munit
