package cafelox.compiler.phase1_scan

/** Lox词元类型，分为单字符符号、双字符符号、字面量、关键字和文件结束标记五类 */
enum TokenType:
  // 单字符的Token
  case Comma
  case Dot
  case LeftBrace
  case LeftParen
  case Minus
  case Plus
  case RightBrace
  case RightParen
  case Semicolon
  case Slash
  case Star

  // 一或二字符的Token
  case Bang
  case BangEqual
  case Equal
  case EqualEqual
  case Greater
  case GreaterEqual
  case Less
  case LessEqual

  // 字面量Token
  case Identifier
  case NumberLiteral(value: Double)
  case StringLiteral(value: String)

  // 关键字Token
  case And
  case Class
  case Else
  case False
  case For
  case Fun
  case If
  case Nil
  case Or
  case Print
  case Puts
  case Return
  case Super
  case This
  case True
  case Var
  case While

  // 结束标记
  case EOF
