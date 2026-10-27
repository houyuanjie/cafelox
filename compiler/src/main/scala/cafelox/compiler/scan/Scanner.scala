package cafelox.compiler.scan

import java.nio.file.Files
import java.nio.file.Path
import scala.util.boundary
import scala.util.boundary.break
import scala.util.control.NonFatal

/** Lox分词器 */
final class Scanner(val source: String, val path: Option[Path] = None):
  import Scanner.*

  def this(path: Path) = this(Files.readString(path), Some(path))

  /** 指针零位 */
  private def initialPosition = Position(1, 1, 0, path)

  /** 开始位置指针 */
  private var start: Position = initialPosition

  /** 当前位置指针 */
  private var current: Position = initialPosition

  /** 收集分词结果 */
  private val tokens = IndexedSeq.newBuilder[Token]

  // ==============================
  //           顶层
  // ==============================

  /** 分词 */
  def tokenize(): Either[ScannerError, IndexedSeq[Token]] =
    // 指针归零
    start = initialPosition
    current = initialPosition
    // 清空上次的收集结果
    tokens.clear()

    try
      while !isAtEnd do
        skipTrivia()
        if !isAtEnd then
          markStart()
          scanToken()
      // 扫描结束时在末尾添加一个EOF
      tokens += Token(TokenType.EOF, "", current)
      Right(tokens.result())
    catch
      case e: ScannerError => Left(e)
      case NonFatal(e)     => Left(ScannerError("An error occurred during tokenization.", current, e))

  // ==============================
  //           主体
  // ==============================

  private def scanToken(): Unit =
    advance() match
      case Some(c) =>
        c match
          case '(' => addToken(TokenType.LeftParen)
          case ')' => addToken(TokenType.RightParen)
          case '{' => addToken(TokenType.LeftBrace)
          case '}' => addToken(TokenType.RightBrace)
          case ',' => addToken(TokenType.Comma)
          case '.' => addToken(TokenType.Dot)
          case '-' => addToken(TokenType.Minus)
          case '+' => addToken(TokenType.Plus)
          case ';' => addToken(TokenType.Semicolon)
          case '*' => addToken(TokenType.Star)
          case '!' => addToken(if matchChar('=') then TokenType.BangEqual else TokenType.Bang)
          case '=' => addToken(if matchChar('=') then TokenType.EqualEqual else TokenType.Equal)
          case '<' => addToken(if matchChar('=') then TokenType.LessEqual else TokenType.Less)
          case '>' => addToken(if matchChar('=') then TokenType.GreaterEqual else TokenType.Greater)
          case '/' => addToken(TokenType.Slash)
          case '"' => scanString()
          case c   =>
            if isDigit(c) then scanNumber()
            else if isIdentHead(c) then scanKeywordOrIdentifier()
            else error(s"Unexpected character: '$c'.")
      case None => error("Unexpected none character.")

  private def scanKeywordOrIdentifier(): Unit =
    // 首字符已经跳过，从第二个字符开始
    while peek().exists(isIdentTail) do advance(): Unit

    val lexeme = currentLexeme()
    val tt     = keywords.getOrElse(lexeme, TokenType.Identifier)
    addToken(tt)

  private def scanNumber(): Unit =
    // 整数部分，最高位字符已经跳过，从次高位字符开始
    while peek().exists(isDigit) do advance(): Unit
    // 小数部分，可能存在
    if peek2().exists((d, f) => d == '.' && isDigit(f)) then
      advance(): Unit // 跳过'.'
      while peek().exists(isDigit) do advance(): Unit

    val lexeme = currentLexeme()
    val value  = lexeme.toDouble
    val tt     = TokenType.NumberLiteral(value)
    addToken(tt)

  private def scanString(): Unit =
    // 左侧的'"'已经跳过，从字符串内容开始
    while peek().exists(_ != '"') do advance(): Unit
    if !matchChar('"') then error("Unterminated string.")

    val lexeme = currentLexeme()
    val value  = lexeme.drop(1).dropRight(1)
    val tt     = TokenType.StringLiteral(value)
    addToken(tt)

  // ==============================
  //           原语
  // ==============================

  /** 判断当前是否已经到达末尾 */
  private def isAtEnd: Boolean =
    current.offset >= source.length

  /** 查看当前字符，不消费 */
  private def peek(): Option[Char] =
    if isAtEnd then None
    else Some(source.charAt(current.offset))

  /** 查看当前和下一个字符，不消费 */
  private def peek2(): Option[(Char, Char)] =
    val nextOffset = current.offset + 1
    if nextOffset >= source.length then None
    else Some((source.charAt(current.offset), source.charAt(nextOffset)))

  /** 推动当前指针移动到下一个字符，返回移动前的字符 */
  private def advance(): Option[Char] =
    if isAtEnd then None
    else
      val before = current
      // 根据当前字符更新current指针
      current = source.charAt(current.offset) match
        case '\n' => current.copy(line = current.line + 1, col = 1, offset = current.offset + 1)
        case _    => current.copy(col = current.col + 1, offset = current.offset + 1)
      // 返回移动前的字符
      Some(source.charAt(before.offset))

  /** 如果当前字符等于`c`，则移动当前指针到下一个字符并返回`true`，否则不动并返回`false` */
  private def matchChar(c: Char): Boolean =
    peek() match
      case Some(`c`) =>
        advance(): Unit
        true
      case _ => false

  // ==============================
  //           工具
  // ==============================

  // 位置处理

  /** 标记当前指针的位置是开始位置 */
  private def markStart(): Unit =
    start = current

  /** 返回开始位置到当前位置之间的文本内容，包含开始位置的字符，不包含当前位置的字符 */
  private def currentLexeme(): String =
    source.substring(start.offset, current.offset)

  /** 使用当前位置的内容构造一个`tt`类型的Token */
  private def addToken(tt: TokenType): Unit =
    tokens += Token(tt, currentLexeme(), start)

  /** 在当前位置构造并抛出一个`ScannerError` */
  private def error(msg: String): Nothing =
    throw ScannerError(msg, current)

  // 跳过

  /** 跳过空白字符 */
  private def skipWhitespace(): Unit =
    while peek().exists(isWhitespace) do advance(): Unit

  /** 跳过注释：一行注释`// ...`、多行注释`/* ... */` */
  private def skipComment(): Unit =
    def doAdvanceUntilNewline(): Unit = boundary:
      while true do
        peek() match
          case None | Some('\n') => break()
          case _                 => advance(): Unit

    def doAdvanceUntilCloseMultilineComment(): Unit = boundary:
      while true do
        peek2() match
          case None | Some(('*', '/')) => break()
          case _                       => advance(): Unit

    peek2() match
      case Some(('/', '/')) =>
        advance(): Unit // 跳过'/'
        advance(): Unit // 跳过'/'
        doAdvanceUntilNewline()
        // 当前'\n'，未消费
      case Some(('/', '*')) =>
        advance(): Unit // 跳过 '/'
        advance(): Unit // 跳过'*'
        doAdvanceUntilCloseMultilineComment()
        // 当前"*/"，需要消费掉
        if !(matchChar('*') && matchChar('/')) then error("Unterminated block comment.")
      case _ => ()

  /** 跳过空白和注释 */
  private def skipTrivia(): Unit =
    while !isAtEnd do
      skipWhitespace()
      peek2() match
        case Some(('/', '/' | '*')) => skipComment()
        case _                      => return

object Scanner:
  // 判断工具

  /** 判断`c`是否一个数字 */
  private def isDigit(c: Char): Boolean =
    c.isDigit

  /** 判断`c`是否一个字母 */
  private def isAlpha(c: Char): Boolean =
    c.isLetter

  /** 判断`c`是否一个字母或数字 */
  private def isAlphaNum(c: Char): Boolean =
    c.isLetterOrDigit

  /** 判断`c`是否一个合法的标识符开头：字母或下划线 */
  private def isIdentHead(c: Char): Boolean =
    isAlpha(c) || c == '_'

  /** 判断`c`是否一个合法的标识符后续：字母、数字或下划线 */
  private def isIdentTail(c: Char): Boolean =
    isAlphaNum(c) || c == '_'

  /** 判断`c`是否一个空白字符，例如：空格`' '`、制表`\t`、回车`\r`、换行`\n` */
  private def isWhitespace(c: Char): Boolean =
    c.isWhitespace

  /** Lox关键字表 */
  private val keywords: Map[String, TokenType] = Map(
    "and"    -> TokenType.And,
    "class"  -> TokenType.Class,
    "else"   -> TokenType.Else,
    "false"  -> TokenType.False,
    "for"    -> TokenType.For,
    "fun"    -> TokenType.Fun,
    "if"     -> TokenType.If,
    "nil"    -> TokenType.Nil,
    "or"     -> TokenType.Or,
    "print"  -> TokenType.Print,
    "puts"   -> TokenType.Puts,
    "return" -> TokenType.Return,
    "super"  -> TokenType.Super,
    "this"   -> TokenType.This,
    "true"   -> TokenType.True,
    "var"    -> TokenType.Var,
    "while"  -> TokenType.While
  )
