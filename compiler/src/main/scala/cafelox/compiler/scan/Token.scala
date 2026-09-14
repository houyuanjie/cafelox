package cafelox.compiler.scan

final case class Token(tokenType: TokenType, lexeme: String, pos: Position)
