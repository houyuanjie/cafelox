package cafelox.compiler.phase1_scan

/** 分词错误 */
final case class ScannerError(msg: String, pos: Position, cause: Throwable | Null = null)
    extends RuntimeException(msg, cause)
