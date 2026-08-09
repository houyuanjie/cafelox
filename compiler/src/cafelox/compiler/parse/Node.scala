package cafelox.compiler.parse

import cafelox.compiler.scan.Token

sealed trait Node:
  val loc: Location

/** 抽象语法树（AST）
  *
  * @see
  *   [Appendix II - Crafting Interpreters](https://www.craftinginterpreters.com/appendix-ii.html)
  */
object Node:
  /** 顶级节点 */
  final case class Program(statements: Statements[Stmt], loc: Location) extends Node

  /** 语句容器 */
  final case class Statements[+T <: Stmt](seq: Seq[T], loc: Location) extends Node

  /** 表达式容器 */
  final case class Expressions[+T <: Expr](seq: Seq[T], loc: Location) extends Node

  /** 语句 */
  enum Stmt extends Node:
    /** 块 */
    case Block(statements: Statements[Stmt], loc: Location)

    /** 类 */
    case Class(name: Token, superClass: Option[Expr.Variable], methods: Statements[Function], loc: Location)

    /** 表达式语句 */
    case Expression(expr: Expr, loc: Location)

    /** 函数 */
    case Function(name: Token, params: Seq[Token], body: Statements[Stmt], loc: Location)

    /** 分支语句 */
    case If(condition: Expr, thenBranch: Stmt, elseBranch: Option[Stmt], loc: Location)

    /** print语句 */
    case Print(expression: Expr, loc: Location)

    /** puts语句 */
    case Puts(expression: Expr, loc: Location)

    /** 返回语句 */
    case Return(keyword: Token, value: Option[Expr], loc: Location)

    /** 变量声明 */
    case Variable(name: Token, initializer: Option[Expr], loc: Location)

    /** 循环语句 */
    case While(condition: Expr, body: Stmt, loc: Location)

  /** 表达式 */
  enum Expr extends Node:
    /** 赋值 */
    case Assign(name: Token, value: Expr, loc: Location)

    /** 二元运算符 */
    case Binary(left: Expr, operator: Token, right: Expr, loc: Location)

    /** 调用 */
    case Call(callee: Expr, paren: Token, arguments: Expressions[Expr], loc: Location)

    /** 访问 */
    case Get(obj: Expr, name: Token, loc: Location)

    /** 分组 */
    case Grouping(expression: Expr, loc: Location)

    /** 布尔真值字面量 */
    case TrueLiteral(loc: Location)

    /** 布尔假值字面量 */
    case FalseLiteral(loc: Location)

    /** 空值字面量 */
    case NilLiteral(loc: Location)

    /** 数字字面量 */
    case NumberLiteral(value: Double, loc: Location)

    /** 字符串字面量 */
    case StringLiteral(value: String, loc: Location)

    /** 逻辑运算符 */
    case Logical(left: Expr, operator: Token, right: Expr, loc: Location)

    /** 设值 */
    case Set(obj: Expr, name: Token, value: Expr, loc: Location)

    /** super关键字 */
    case Super(keyword: Token, method: Token, loc: Location)

    /** this关键字 */
    case This(keyword: Token, loc: Location)

    /** 一元运算符 */
    case Unary(operator: Token, right: Expr, loc: Location)

    /** 变量 */
    case Variable(name: Token, loc: Location)
