package cafelox.compiler.parse

import cafelox.compiler.scan.Position
import cafelox.compiler.scan.Token
import cafelox.compiler.scan.TokenType

import scala.compiletime.uninitialized
import scala.util.boundary
import scala.util.boundary.break
import scala.util.control.NonFatal

/** Lox解析器 */
final class Parser(val tokens: IndexedSeq[Token]):
  import Parser.*

  /** 指针 */
  private var current: Int = uninitialized

  /** 收集解析错误 */
  private val errors = Seq.newBuilder[ParserError]

  /** 收集顶级语句 */
  private val statements = Seq.newBuilder[Node.Stmt]

  // ==============================
  //           顶层
  // ==============================

  def parse(): Either[Seq[ParserError], Node.Program] =
    // 指针归零
    current = 0
    // 清空上次的错误
    errors.clear()
    // 清空上次的语句
    statements.clear()

    // 失败路径：tokens序列不能为空
    if tokens.isEmpty then
      return Left(Seq(ParserError("Empty token stream.", Token(TokenType.EOF, "", Position(1, 1, 0)))))
    // 失败路径：tokens序列必须以EOF结尾
    if tokens.last.tokenType != TokenType.EOF then
      return Left(Seq(ParserError("Token stream must end with EOF.", tokens.last)))

    try
      // 递归下降解析
      while !isAtEnd do declaration().foreach(statements += _)

      // 失败路径：tokens中间不允许EOF，当解析结束时，tokens序列必须耗尽
      if current != (tokens.length - 1) then
        errors += ParserError("Parser stopped before reaching the end of the token stream.", peek())
      // 最终失败路径：errors不为空
      val errs = errors.result()
      if errs.nonEmpty then return Left(errs)

      // 包装解析结果
      val (firstToken, eof) = (tokens.head, tokens.last)
      val fullTextLoc       = Location(firstToken.pos, eof.pos)
      val statementsNode    = Node.Statements(statements.result(), fullTextLoc)
      val programNode       = Node.Program(statementsNode, fullTextLoc)
      Right(programNode)
    catch
      case NonFatal(e) =>
        // ParserError原样记录（
        //     防御性：
        //         declaration()已捕获语句/声明链上的所有ParserError，
        //         正常情况下不可达
        // ），
        // 其他异常包装为ParserError
        errors += (e match
          case pe: ParserError => pe
          case other           => ParserError("An error occurred during parse.", peek(), other))
        Left(errors.result())

  // ==============================
  //           递归下降
  // ==============================

  // 声明

  /** 解析声明结构：类、函数、变量；下层：语句 */
  private def declaration(): Option[Node.Stmt] =
    try
      if matchType(TokenType.Class) then Some(classDeclaration())
      else if matchType(TokenType.Fun) then Some(functionDeclaration("function"))
      else if matchType(TokenType.Var) then Some(varDeclaration())
      else Some(statement())
    catch
      case e: ParserError =>
        errors += e
        synchronize()
        None

  /** 解析类声明 */
  private def classDeclaration(): Node.Stmt.Class =
    val startOfClass = previous() // 'class'

    val name = consume(TokenType.Identifier, "Expect class name.")

    var superClass: Option[Node.Expr.Variable] = None
    if matchType(TokenType.Less) then
      val superClassName = consume(TokenType.Identifier, "Expect superclass name.")
      superClass = Some(Node.Expr.Variable(superClassName, Location(superClassName.pos, peek().pos)))

    val startOfBody = consume(TokenType.LeftBrace, "Expect '{' before class body.")
    val methods     = Seq.newBuilder[Node.Stmt.Function]
    while !check(TokenType.RightBrace) do methods += functionDeclaration("method")
    consume(TokenType.RightBrace, "Expect '}' after class body.")

    Node.Stmt.Class(
      name,
      superClass,
      Node.Statements(
        methods.result(),
        Location(startOfBody.pos, peek().pos)
      ),
      Location(startOfClass.pos, peek().pos)
    )

  /** 解析函数或方法声明 */
  private def functionDeclaration(kind: "function" | "method"): Node.Stmt.Function =
    val startOfFun = previous() // 'fun'

    val name = consume(TokenType.Identifier, s"Expect $kind name.")

    consume(TokenType.LeftParen, s"Expect '(' after $kind name.")
    val parameters      = Seq.newBuilder[Token]
    var parametersCount = 0
    boundary:
      while !check(TokenType.RightParen) do
        if parametersCount >= 255 then error("Can't have more than 255 parameters.")
        parameters += consume(TokenType.Identifier, "Expect parameter name.")
        parametersCount += 1
        // 每个参数之后跟一个逗号
        if !matchType(TokenType.Comma) then break()
        // 但最后一个参数之后不接受尾随逗号
        if check(TokenType.RightParen) then error("Don't trail a comma.")
    consume(TokenType.RightParen, "Expect ')' after parameters.")

    consume(TokenType.LeftBrace, s"Expect '{' before $kind body.")
    val body = blockStatement()

    Node.Stmt.Function(
      name,
      parameters.result(),
      body,
      Location(startOfFun.pos, peek().pos)
    )

  /** 解析变量声明 */
  private def varDeclaration(): Node.Stmt.Variable =
    val startOfVar = previous() // 'var'

    val name = consume(TokenType.Identifier, "Expect variable name.")

    var initializer: Option[Node.Expr] = None
    if matchType(TokenType.Equal) then initializer = Some(expression())

    consume(TokenType.Semicolon, "Expect ';' after variable declaration.")

    Node.Stmt.Variable(
      name,
      initializer,
      Location(startOfVar.pos, peek().pos)
    )

  // 语句

  /** 解析语句结构：for、if、print、puts、return、while、块、表达式 */
  private def statement(): Node.Stmt =
    if matchType(TokenType.For) then forStatement()
    else if matchType(TokenType.If) then ifStatement()
    else if matchType(TokenType.Print) then printStatement()
    else if matchType(TokenType.Puts) then putsStatement()
    else if matchType(TokenType.Return) then returnStatement()
    else if matchType(TokenType.While) then whileStatement()
    else if matchType(TokenType.LeftBrace) then blockStatement()
    else expressionStatement()

  /** 解析for循环结构 */
  private def forStatement(): Node.Stmt.Block =
    val startOfFor = previous() // 'for'

    consume(TokenType.LeftParen, "Expect '(' after 'for'.")

    val startOfInitializer = peek()
    val initializer        =
      if matchType(TokenType.Semicolon) then None
      else if matchType(TokenType.Var) then Some(varDeclaration())
      else Some(expressionStatement())

    val startOfCondition = peek()
    val condition        =
      if check(TokenType.Semicolon) then None
      else Some(expression())
    consume(TokenType.Semicolon, "Expect ';' after loop condition.")

    val startOfIncrement = peek()
    val increment        =
      if check(TokenType.RightParen) then None
      else Some(expression())

    consume(TokenType.RightParen, "Expect ')' after for clauses.")

    var body = statement()

    // 脱糖

    // 如果存在更新表达式，则与body打包成一个块
    if increment.isDefined then
      body = Node.Stmt.Block(
        Node.Statements(
          Seq(
            body,
            Node.Stmt.Expression(increment.get, increment.get.loc)
          ),
          Location(startOfIncrement.pos, peek().pos)
        ),
        Location(startOfIncrement.pos, peek().pos)
      )

    // 使用while循环重写，若没有条件表达式，则使用true作为循环条件
    body = Node.Stmt.While(
      condition.getOrElse(
        Node.Expr.TrueLiteral(Location(startOfCondition.pos, startOfCondition.pos))
      ),
      body,
      Location(startOfCondition.pos, peek().pos)
    )

    // 如果存在初始化语句，则与while循环打包成一个块
    Node.Stmt.Block(
      Node.Statements(
        initializer.fold(Seq(body))(Seq(_, body)),
        Location(startOfInitializer.pos, peek().pos)
      ),
      Location(startOfFor.pos, peek().pos)
    )

  /** 解析if分支语句结构 */
  private def ifStatement(): Node.Stmt.If =
    val startOfIf = previous() // 'if'

    consume(TokenType.LeftParen, "Expect '(' after 'if'.")
    val condition = expression()
    consume(TokenType.RightParen, "Expect ')' after if condition.")

    val thenBranch = statement()

    var elseBranch: Option[Node.Stmt] = None
    if matchType(TokenType.Else) then elseBranch = Some(statement())

    Node.Stmt.If(
      condition,
      thenBranch,
      elseBranch,
      Location(startOfIf.pos, peek().pos)
    )

  /** 解析print打印语句结构 */
  private def printStatement(): Node.Stmt.Print =
    val startOfPrint = previous() // 'print'

    val value = expression()
    consume(TokenType.Semicolon, "Expect ';' after value.")

    Node.Stmt.Print(
      value,
      Location(startOfPrint.pos, peek().pos)
    )

  /** 解析puts打印换行语句结构 */
  private def putsStatement(): Node.Stmt.Puts =
    val startOfPuts = previous() // 'puts'

    val value = expression()
    consume(TokenType.Semicolon, "Expect ';' after value.")

    Node.Stmt.Puts(
      value,
      Location(startOfPuts.pos, peek().pos)
    )

  /** 解析return返回语句结构 */
  private def returnStatement(): Node.Stmt.Return =
    val startOfReturn = previous() // 'return'

    var value: Option[Node.Expr] = None
    if !check(TokenType.Semicolon) then value = Some(expression())
    consume(TokenType.Semicolon, "Expect ';' after return value.")

    Node.Stmt.Return(
      startOfReturn,
      value,
      Location(startOfReturn.pos, peek().pos)
    )

  /** 解析while循环语句结构 */
  private def whileStatement(): Node.Stmt.While =
    val startOfWhile = previous() // 'while'

    consume(TokenType.LeftParen, "Expect '(' after 'while'.")
    val condition = expression()
    consume(TokenType.RightParen, "Expect ')' after condition.")

    val body = statement()

    Node.Stmt.While(
      condition,
      body,
      Location(startOfWhile.pos, peek().pos)
    )

  /** 解析块结构 */
  private def blockStatement(): Node.Stmt.Block =
    val startOfBlock = previous() // '{'

    val stmts = Seq.newBuilder[Node.Stmt]
    while !check(TokenType.RightBrace) do declaration().foreach(stmts += _)

    consume(TokenType.RightBrace, "Expect '}' after block.")

    Node.Stmt.Block(
      Node.Statements(
        stmts.result(),
        Location(startOfBlock.pos, peek().pos)
      ),
      Location(startOfBlock.pos, peek().pos)
    )

  /** 解析表达式语句结构 */
  private def expressionStatement(): Node.Stmt.Expression =
    val startOfExprStmt = peek()

    val expr = expression()
    consume(TokenType.Semicolon, "Expect ';' after expression.")

    Node.Stmt.Expression(
      expr,
      Location(startOfExprStmt.pos, peek().pos)
    )

  // ==============================
  //           表达式
  // ==============================

  private def expression(): Node.Expr = ???

  // TODO

  // ==============================
  //           原语
  // ==============================

  /** 查看当前Token，不消费 */
  private inline def peek(): Token =
    tokens(current)

  /** 查看前一个Token，不消费 */
  private inline def previous(): Token =
    // 调用previous()之前必须先成功消费过Token：advance()/consume()/matchType()
    tokens(current - 1)

  /** 判断当前是否已经到达末尾 */
  private inline def isAtEnd: Boolean =
    peek().tokenType == TokenType.EOF

  /** 报错 */
  private inline def error(msg: String): Nothing =
    throw ParserError(msg, peek())

  /** 推动当前指针移动到下一个Token，返回移动前的Token */
  private def advance(): Token =
    if !isAtEnd then current += 1
    previous()

  /** 如果当前未到达EOF，则检查当前Token的TokenType是否为`tokenType` */
  private def check(tokenType: TokenType): Boolean =
    !isAtEnd && peek().tokenType == tokenType

  /** 消费一个`tokenType`类型的Token，返回消费掉的Token，当前Token不匹配时产生错误消息`msg` */
  private def consume(tokenType: TokenType, msg: String): Token =
    if check(tokenType) then advance()
    else error(msg)

  /** 消费一个`tokenTypes`中的Token，当前Token不匹配时返回false */
  private def matchType(tokenTypes: TokenType*) =
    if tokenTypes.exists(check) then
      advance()
      true
    else false

  /** 错误恢复 */
  private def synchronize(): Unit =
    // 跳过出错Token
    advance()
    // 尝试向后跳
    boundary:
      while !isAtEnd do
        // 前一个Token是分号';'，这条语句结束，不跳了，尝试恢复
        if previous().tokenType == TokenType.Semicolon then break()
        // 当前Token是新的语法结构，不跳了，尝试恢复
        if checkpoints.contains(peek().tokenType) then break()
        // 继续向后跳
        advance(): Unit

object Parser:
  /** 安全的语法结构 */
  private val checkpoints: Seq[TokenType] = Seq(
    TokenType.Class,
    TokenType.Fun,
    TokenType.Var,
    TokenType.For,
    TokenType.If,
    TokenType.While,
    TokenType.Print,
    TokenType.Puts,
    TokenType.Return
  )
