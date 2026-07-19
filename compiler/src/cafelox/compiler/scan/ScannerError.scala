package cafelox.compiler.scan

/** 分词错误 */
final case class ScannerError(msg: String, pos: Position, cause: Throwable | Null = null)
    extends RuntimeException(msg, cause)
