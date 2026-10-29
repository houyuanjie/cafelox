package cafelox.compiler.phase2_parse

import cafelox.compiler.phase1_scan.Token

/** 解析错误 */
final case class ParserError(msg: String, token: Token, cause: Throwable | Null = null)
    extends RuntimeException(msg, cause)
