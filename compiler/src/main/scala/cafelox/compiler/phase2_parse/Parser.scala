package cafelox.compiler.phase2_parse

import cafelox.compiler.common.Node
import cafelox.compiler.phase1_scan.Position
import cafelox.compiler.phase1_scan.Token
import cafelox.compiler.phase1_scan.TokenType

import scala.annotation.tailrec
import scala.util.boundary
import scala.util.boundary.break
import scala.util.control.NonFatal

/** Lox解析器
  *
  * @param tokens
  *   词元序列
  */
final class Parser(val tokens: IndexedSeq[Token]):
  import Parser.*

  /** 当前解析位置 */
  private var current: Int = 0

  /** 已收集的解析错误 */
  private val errors = Seq.newBuilder[ParserError]

  /** 已收集的顶级语句 */
  private val statements = Seq.newBuilder[Node.Stmt]

  // ==============================
  //           顶层
  // ==============================

  /** 解析 */
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
      val programNode       = Node.Program(
        Node.Statements(
          statements.result(),
          Location(firstToken.pos, eof.pos)
        ),
        Location(firstToken.pos, eof.pos)
      )
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

  /** 解析声明（类、函数、变量），下层解析语句 */
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
      superClass = Some(
        Node.Expr.Variable(
          superClassName,
          Location(superClassName.pos, peek().pos)
        )
      )

    val startOfBody = consume(TokenType.LeftBrace, "Expect '{' before class body.")
    val methods     = Seq.newBuilder[Node.Stmt.Function]
    while !check(TokenType.RightBrace) && !isAtEnd do methods += functionDeclaration("method")
    consume(TokenType.RightBrace, "Expect '}' after class body."): Unit

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
    // function从'fun'开始，method从方法名开始
    val startOfFun = if kind == "function" then previous() else peek()

    val name = consume(TokenType.Identifier, s"Expect $kind name.")

    consume(TokenType.LeftParen, s"Expect '(' after $kind name."): Unit
    val parameters      = Seq.newBuilder[Token]
    var parametersCount = 0
    boundary:
      while !check(TokenType.RightParen) && !isAtEnd do
        if parametersCount >= 255 then error("Can't have more than 255 parameters.")
        parameters += consume(TokenType.Identifier, "Expect parameter name.")
        parametersCount += 1
        // 每个参数之后跟一个逗号
        if !matchType(TokenType.Comma) then break()

    // 不能尾随逗号
    if previous().tokenType == TokenType.Comma then error("Don't trail a comma.")

    // 消耗')'
    consume(TokenType.RightParen, "Expect ')' after parameters."): Unit

    consume(TokenType.LeftBrace, s"Expect '{' before $kind body."): Unit
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

    consume(TokenType.Semicolon, "Expect ';' after variable declaration."): Unit

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

    consume(TokenType.LeftParen, "Expect '(' after 'for'."): Unit

    val startOfInitializer = peek()
    val initializer        =
      if matchType(TokenType.Semicolon) then None
      else if matchType(TokenType.Var) then Some(varDeclaration())
      else Some(expressionStatement())

    val startOfCondition = peek()
    val condition        =
      if check(TokenType.Semicolon) then None
      else Some(expression())
    consume(TokenType.Semicolon, "Expect ';' after loop condition."): Unit

    val startOfIncrement = peek()
    val increment        =
      if check(TokenType.RightParen) then None
      else Some(expression())

    consume(TokenType.RightParen, "Expect ')' after for clauses."): Unit

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

    consume(TokenType.LeftParen, "Expect '(' after 'if'."): Unit
    val condition = expression()
    consume(TokenType.RightParen, "Expect ')' after if condition."): Unit

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
    consume(TokenType.Semicolon, "Expect ';' after value."): Unit

    Node.Stmt.Print(
      value,
      Location(startOfPrint.pos, peek().pos)
    )

  /** 解析puts打印换行语句结构 */
  private def putsStatement(): Node.Stmt.Puts =
    val startOfPuts = previous() // 'puts'

    val value = expression()
    consume(TokenType.Semicolon, "Expect ';' after value."): Unit

    Node.Stmt.Puts(
      value,
      Location(startOfPuts.pos, peek().pos)
    )

  /** 解析return返回语句结构 */
  private def returnStatement(): Node.Stmt.Return =
    val startOfReturn = previous() // 'return'

    var value: Option[Node.Expr] = None
    if !check(TokenType.Semicolon) then value = Some(expression())
    consume(TokenType.Semicolon, "Expect ';' after return value."): Unit

    Node.Stmt.Return(
      startOfReturn,
      value,
      Location(startOfReturn.pos, peek().pos)
    )

  /** 解析while循环语句结构 */
  private def whileStatement(): Node.Stmt.While =
    val startOfWhile = previous() // 'while'

    consume(TokenType.LeftParen, "Expect '(' after 'while'."): Unit
    val condition = expression()
    consume(TokenType.RightParen, "Expect ')' after condition."): Unit

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
    while !check(TokenType.RightBrace) && !isAtEnd do declaration().foreach(stmts += _)

    consume(TokenType.RightBrace, "Expect '}' after block."): Unit

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
    consume(TokenType.Semicolon, "Expect ';' after expression."): Unit

    Node.Stmt.Expression(
      expr,
      Location(startOfExprStmt.pos, peek().pos)
    )

  // ==============================
  //           表达式
  // ==============================

  // Pratt解析算法

  /** 解析表达式（使用Pratt算法） */
  private def expression(rightBindingPower: Int = lowestBindingPower): Node.Expr =
    // 一个表达式只能由一个前缀的或主要的子表达式开始
    var expr = parsePrefixExpression()

    // 尝试向后解析中缀部分，追加到构造的表达式中
    boundary:
      while true do
        val part = peek()

        // 若向后看不到一个中缀符号，则结束追加
        if !isInfixOperator(part.tokenType) then break()
        val bp = infixOperatorBindingPowers(part.tokenType)
        // 若后一个中缀符号的绑定力不足，则结束追加
        if bp.left < rightBindingPower then break()

        // 追加右边部分，构造更大的表达式
        expr = parseInfixExpression(expr)

    expr

  /** 解析前缀表达式（一元运算符或首要表达式） */
  private def parsePrefixExpression(): Node.Expr =
    val startOfExpr = peek() // 可能是前缀符号，需要判断

    if isPrefixOperator(startOfExpr.tokenType) then
      val op    = advance()
      val right = expression(prefixOperatorBindingPowers(op.tokenType))

      Node.Expr.Unary(
        op,
        right,
        Location(startOfExpr.pos, peek().pos)
      )
    else parsePrimaryExpression()

  /** 解析中缀表达式 */
  private def parseInfixExpression(left: Node.Expr): Node.Expr =
    val op = advance() // 中缀符号

    val bp    = infixOperatorBindingPowers(op.tokenType)
    val right = expression(bp.right)

    op.tokenType match
      // 语法结构：赋值表达式
      case TokenType.Equal =>
        left match
          // ‘foo = 123’
          case Node.Expr.Variable(name, _, _) =>
            Node.Expr.Assign(
              name,
              right,
              Location(left.loc.startIncluded, peek().pos)
            )

          // 'foo.bar = 123'
          case Node.Expr.Get(obj, name, _) =>
            Node.Expr.Set(
              obj,
              name,
              right,
              Location(left.loc.startIncluded, peek().pos)
            )

          // 其它形式
          case _ =>
            error("Invalid assignment target.")

      // 逻辑运算
      case TokenType.And | TokenType.Or =>
        Node.Expr.Logical(
          left,
          op,
          right,
          Location(left.loc.startIncluded, peek().pos)
        )

      // 算术运算
      case TokenType.EqualEqual | TokenType.BangEqual | TokenType.Less | TokenType.Greater | TokenType.LessEqual |
          TokenType.GreaterEqual | TokenType.Plus | TokenType.Minus | TokenType.Star | TokenType.Slash =>
        Node.Expr.Binary(
          left,
          op,
          right,
          Location(left.loc.startIncluded, peek().pos)
        )

      case _ =>
        error("Invalid infix expression.")

  /** 解析首要表达式 */
  private def parsePrimaryExpression(): Node.Expr =
    val startOfPrimary = peek() // 首要表达式

    val baseExpr = startOfPrimary.tokenType match
      // 数值字面量
      case TokenType.NumberLiteral(value) =>
        advance(): Unit

        Node.Expr.NumberLiteral(
          value,
          Location(startOfPrimary.pos, peek().pos)
        )

      // 字符串字面量
      case TokenType.StringLiteral(value) =>
        advance(): Unit

        Node.Expr.StringLiteral(
          value,
          Location(startOfPrimary.pos, peek().pos)
        )

      // 'true'
      case TokenType.True =>
        advance(): Unit

        Node.Expr.TrueLiteral(
          Location(startOfPrimary.pos, peek().pos)
        )

      // 'false'
      case TokenType.False =>
        advance(): Unit

        Node.Expr.FalseLiteral(
          Location(startOfPrimary.pos, peek().pos)
        )

      // 'nil'
      case TokenType.Nil =>
        advance(): Unit

        Node.Expr.NilLiteral(
          Location(startOfPrimary.pos, peek().pos)
        )

      // 标识符
      case TokenType.Identifier =>
        val name = advance()

        Node.Expr.Variable(
          name,
          Location(name.pos, peek().pos)
        )

      // 'this'
      case TokenType.This =>
        val `this` = advance()

        Node.Expr.This(
          `this`,
          Location(`this`.pos, peek().pos)
        )

      // 'super'
      case TokenType.Super =>
        val `super` = advance()
        consume(TokenType.Dot, "Expect '.' after 'super'."): Unit
        val methodName = consume(TokenType.Identifier, "Expect superclass method name.")

        Node.Expr.Super(
          `super`,
          methodName,
          Location(`super`.pos, peek().pos)
        )

      // '('
      case TokenType.LeftParen =>
        advance(): Unit // '('
        val expr = expression()
        consume(TokenType.RightParen, "Expect ')' after expression."): Unit

        Node.Expr.Grouping(
          expr,
          Location(startOfPrimary.pos, peek().pos)
        )

      // 其它情况
      case _ => error("Invalid primary expression.")

    // 链式后缀解析（支持 .prop、(args)、链式组合）
    if check(TokenType.Dot) then parseGetExpression(baseExpr)
    else if check(TokenType.LeftParen) then parseCallExpression(baseExpr)
    else baseExpr

  /** 解析函数调用 */
  @tailrec
  private def parseCallExpression(callee: Node.Expr): Node.Expr =
    val leftParen = advance() // '('

    // 收集参数列表
    val args      = Seq.newBuilder[Node.Expr]
    var argsCount = 0
    boundary:
      while !check(TokenType.RightParen) && !isAtEnd do
        if argsCount >= 255 then error("Can't have more than 255 arguments.")
        args += expression()
        argsCount += 1
        // 每个参数之后跟一个逗号
        if !matchType(TokenType.Comma) then break()

    // 不能尾随逗号
    if previous().tokenType == TokenType.Comma then error("Don't trail a comma.")

    // 消耗')'
    val rightParen = consume(TokenType.RightParen, "Expect ')' after arguments.")

    val call = Node.Expr.Call(
      callee,
      leftParen,
      Node.Expressions(
        args.result(),
        Location(leftParen.pos, rightParen.pos)
      ),
      Location(callee.loc.startIncluded, peek().pos)
    )

    if check(TokenType.LeftParen) then parseCallExpression(call) // 'foo(1,2,3)(4,5,6)'
    else if check(TokenType.Dot) then parseGetExpression(call)   // 'foo(1,2,3).bar'
    else call

  /** 解析属性访问 */
  @tailrec
  private def parseGetExpression(obj: Node.Expr): Node.Expr =
    advance(): Unit // '.'

    val name = consume(TokenType.Identifier, "Expect property name after '.'.")

    val get = Node.Expr.Get(
      obj,
      name,
      Location(obj.loc.startIncluded, peek().pos)
    )

    if check(TokenType.LeftParen) then parseCallExpression(get) // 'foo.bar(1,2,3)'
    else if check(TokenType.Dot) then parseGetExpression(get)   // 'foo.bar.baz'
    else get

  /** 判断是否前缀符号 */
  private def isPrefixOperator(tt: TokenType): Boolean =
    prefixOperatorBindingPowers.contains(tt)

  /** 判断是否中缀符号 */
  private def isInfixOperator(tt: TokenType): Boolean =
    infixOperatorBindingPowers.contains(tt)

  // ==============================
  //           原语
  // ==============================

  /** 查看当前Token，不消费 */
  private def peek(): Token =
    tokens(current)

  /** 查看前一个Token，不消费 */
  private def previous(): Token =
    // 调用previous()之前必须先成功消费过Token：advance()/consume()/matchType()
    tokens(current - 1)

  /** 判断当前是否已经到达末尾 */
  private def isAtEnd: Boolean =
    peek().tokenType == TokenType.EOF

  /** 构造并抛出一个解析错误 */
  private def error(msg: String): Nothing =
    throw ParserError(msg, peek())

  /** 将当前位置前移一词元，并返回前移前的词元 */
  private def advance(): Token =
    if !isAtEnd then current += 1
    previous()

  /** 若未到EOF且当前词元类型为`tokenType`则返回`true` */
  private def check(tokenType: TokenType): Boolean =
    !isAtEnd && peek().tokenType == tokenType

  /** 消费一个`tokenType`类型的Token，返回消费掉的Token，当前Token不匹配时产生错误消息`msg` */
  private def consume(tokenType: TokenType, msg: String): Token =
    if check(tokenType) then advance()
    else error(msg)

  /** 消费一个`tokenTypes`中的Token，当前Token不匹配时返回false */
  private def matchType(tokenTypes: TokenType*) =
    if tokenTypes.exists(check) then
      advance(): Unit
      true
    else false

  /** 发生解析错误后，跳过词元直到可安全恢复的位置 */
  private def synchronize(): Unit =
    // 跳过出错Token
    advance(): Unit
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
  /** 错误恢复时可停顿的语法结构 */
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

  /** 最低绑定力 */
  private val lowestBindingPower: Int = 0

  /** 前缀（一元）符号的绑定力 */
  private val prefixOperatorBindingPowers: Map[TokenType, Int] = Map(
    // 逻辑运算

    /* 逻辑取反操作符 */
    TokenType.Bang -> 25,

    // 算术运算

    /* 取相反数操作符 */
    TokenType.Minus -> 25
  )

  /** 左右绑定力 */
  final private case class InfixOperatorBindingPower(left: Int, right: Int)

  /** 中缀（二元）符号的左右绑定力 */
  private val infixOperatorBindingPowers: Map[TokenType, InfixOperatorBindingPower] = Map(
    // 语法结构

    /* 赋值表达式 */
    TokenType.Equal -> InfixOperatorBindingPower(1, 1),

    // 逻辑运算

    /* 短路或 */
    TokenType.Or -> InfixOperatorBindingPower(2, 3),
    /* 短路与 */
    TokenType.And -> InfixOperatorBindingPower(3, 4),

    // 二元算术

    /* 判断相等 */
    TokenType.EqualEqual -> InfixOperatorBindingPower(5, 6),
    /* 判断不相等 */
    TokenType.BangEqual -> InfixOperatorBindingPower(5, 6),
    /* 判断小于 */
    TokenType.Less -> InfixOperatorBindingPower(7, 8),
    /* 判断大于 */
    TokenType.Greater -> InfixOperatorBindingPower(7, 8),
    /* 判断小于等于 */
    TokenType.LessEqual -> InfixOperatorBindingPower(7, 8),
    /* 判断大于等于 */
    TokenType.GreaterEqual -> InfixOperatorBindingPower(7, 8),

    /* 加法操作符 */
    TokenType.Plus -> InfixOperatorBindingPower(10, 11),
    /* 减法操作符 */
    TokenType.Minus -> InfixOperatorBindingPower(10, 11),
    /* 乘法操作符 */
    TokenType.Star -> InfixOperatorBindingPower(20, 21),
    /* 除法操作符 */
    TokenType.Slash -> InfixOperatorBindingPower(20, 21)
  )
