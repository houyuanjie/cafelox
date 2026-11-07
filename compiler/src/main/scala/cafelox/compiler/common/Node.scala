package cafelox.compiler.common

import cafelox.compiler.phase1_scan.Token
import cafelox.compiler.phase2_parse.Location
import cafelox.compiler.phase3_analyze.Analyzer.Resolved
import cafelox.compiler.phase3_analyze.SymbolTable

/** 抽象节点 */
sealed trait Node:
  val loc: Location

/** 抽象语法树（AST）
  *
  * @see
  *   [Appendix II - Crafting Interpreters](https://www.craftinginterpreters.com/appendix-ii.html)
  */
object Node:
  /** 顶级节点 */
  final case class Program(statements: Statements, loc: Location, symbolTable: SymbolTable = SymbolTable()) extends Node

  /** 语句容器 */
  final case class Statements(seq: Seq[Stmt], loc: Location) extends Node

  /** 表达式容器 */
  final case class Expressions(seq: Seq[Expr], loc: Location) extends Node

  /** 语句 */
  enum Stmt extends Node:
    /** 块 */
    case Block(statements: Statements, loc: Location) extends Stmt

    /** 类 */
    case Class(name: Token, superClass: Option[Expr.Variable], methods: Statements, loc: Location) extends Stmt

    /** 表达式语句 */
    case Expression(expr: Expr, loc: Location) extends Stmt

    /** 函数 */
    case Function(name: Token, params: Seq[Token], body: Block, loc: Location) extends Stmt

    /** 分支语句 */
    case If(condition: Expr, thenBranch: Stmt, elseBranch: Option[Stmt], loc: Location) extends Stmt

    /** print语句 */
    case Print(expression: Expr, loc: Location) extends Stmt

    /** puts语句 */
    case Puts(expression: Expr, loc: Location) extends Stmt

    /** 返回语句 */
    case Return(keyword: Token, value: Option[Expr], loc: Location) extends Stmt

    /** 变量声明 */
    case Variable(name: Token, initializer: Option[Expr], loc: Location) extends Stmt

    /** 循环语句 */
    case While(condition: Expr, body: Stmt, loc: Location) extends Stmt

  /** 表达式 */
  enum Expr extends Node:
    /** 赋值 */
    case Assign(name: Token, value: Expr, loc: Location, resolved: Resolved = Resolved.None) extends Expr

    /** 二元运算符 */
    case Binary(left: Expr, operator: Token, right: Expr, loc: Location) extends Expr

    /** 调用 */
    case Call(callee: Expr, paren: Token, arguments: Expressions, loc: Location) extends Expr

    /** 访问 */
    case Get(obj: Expr, name: Token, loc: Location) extends Expr

    /** 分组 */
    case Grouping(expression: Expr, loc: Location) extends Expr

    /** 布尔真值字面量 */
    case TrueLiteral(loc: Location) extends Expr

    /** 布尔假值字面量 */
    case FalseLiteral(loc: Location) extends Expr

    /** 空值字面量 */
    case NilLiteral(loc: Location) extends Expr

    /** 数字字面量 */
    case NumberLiteral(value: Double, loc: Location) extends Expr

    /** 字符串字面量 */
    case StringLiteral(value: String, loc: Location) extends Expr

    /** 逻辑运算符 */
    case Logical(left: Expr, operator: Token, right: Expr, loc: Location) extends Expr

    /** 设值 */
    case Set(obj: Expr, name: Token, value: Expr, loc: Location) extends Expr

    /** super关键字 */
    case Super(keyword: Token, method: Token, loc: Location, resolved: Resolved = Resolved.None) extends Expr

    /** this关键字 */
    case This(keyword: Token, loc: Location, resolved: Resolved = Resolved.None) extends Expr

    /** 一元运算符 */
    case Unary(operator: Token, right: Expr, loc: Location) extends Expr

    /** 变量 */
    case Variable(name: Token, loc: Location, resolved: Resolved = Resolved.None) extends Expr
