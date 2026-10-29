package cafelox.compiler.phase1_scan

final case class Token(tokenType: TokenType, lexeme: String, pos: Position)
