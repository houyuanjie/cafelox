package cafelox.compiler.phase2_parse

import cafelox.compiler.phase1_scan.Token

/** 解析错误
  *
  * @param msg
  *   错误消息
  * @param token
  *   产生错误时正在处理`token`
  * @param cause
  *   由`cause`引起的错误
  */
final case class ParserError(msg: String, token: Token, cause: Throwable | Null = null)
    extends RuntimeException(msg, cause)
