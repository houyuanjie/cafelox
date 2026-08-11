package millbuild

/** 公共配置 */
object Commons:
  val scalaCompilerOptions = Seq(
    // format: off
    "-encoding", "UTF-8",
    "-release", "25",
    "-explain",
    "-explain-types",
    "-feature",
    "-deprecation",
    "-unchecked",
    "-Wsafe-init",
    "-Wunused:all",
    "-Wvalue-discard",
    "-Yexplicit-nulls",
    // format: on
  )
