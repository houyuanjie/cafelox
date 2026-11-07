package cafelox.compiler.phase1_scan

/** 词元
  *
  * @param tokenType
  *   类型
  * @param lexeme
  *   字面内容
  * @param pos
  *   位置
  */
final case class Token(tokenType: TokenType, lexeme: String, pos: Position)
