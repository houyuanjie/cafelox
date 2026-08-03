package cafelox.compiler.parse

import cafelox.compiler.scan.Token

/** 解析错误 */
final case class ParserError(msg: String, token: Token, cause: Throwable | Null = null)
    extends RuntimeException(msg, cause)
