import sbt.*

/** Scala package index:
  *
  *   - https://index.scala-lang.org/
  *   - https://pkg.scalanews.net/
  */
object Dependencies:
  object Versions:
    lazy val munit = "1.3.6"

  lazy val munit = "org.scalameta" %% "munit" % Versions.munit
