package cafelox.compiler.phase1_scan

/** 分词错误
  *
  * @param msg
  *   错误消息
  * @param pos
  *   发生错误的位置
  * @param cause
  *   由`cause`诱发的错误
  */
final case class ScannerError(msg: String, pos: Position, cause: Throwable | Null = null)
    extends RuntimeException(msg, cause)
