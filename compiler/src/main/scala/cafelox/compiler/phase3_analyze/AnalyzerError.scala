package cafelox.compiler.phase3_analyze

import cafelox.compiler.phase2_parse.Location

/** 语义分析错误
  *
  * @param msg
  *   错误消息
  * @param loc
  *   发生错误时处理的位置
  * @param cause
  *   由`cause`引发的错误
  */
final case class AnalyzerError(msg: String, loc: Location, cause: Throwable | Null = null)
    extends RuntimeException(msg, cause)
