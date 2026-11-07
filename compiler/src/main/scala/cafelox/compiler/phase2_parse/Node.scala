package cafelox.compiler.phase2_parse

import cafelox.compiler.phase1_scan.Token

/** 抽象节点
  *
  * @tparam A
  *   不同编译阶段附加在该节点的信息的类型
  */
sealed trait Node[A]:
  /** 阶段附加信息 */
  val attachment: A

/** 抽象语法树（AST）
  *
  * @see
  *   [Appendix II - Crafting Interpreters](https://www.craftinginterpreters.com/appendix-ii.html)
  */
object Node:
  /** 顶级节点 */
  final case class Program[A](statements: Statements[A, Stmt[A]], attachment: A) extends Node[A]

  /** 语句容器 */
  final case class Statements[A, +T <: Stmt[A]](seq: Seq[T], attachment: A) extends Node[A]

  /** 表达式容器 */
  final case class Expressions[A, +T <: Expr[A]](seq: Seq[T], attachment: A) extends Node[A]

  /** 语句 */
  enum Stmt[A] extends Node[A]:
    /** 块 */
    case Block[A](statements: Statements[A, Stmt[A]], attachment: A) extends Stmt[A]

    /** 类 */
    case Class[A](name: Token, superClass: Option[Expr.Variable[A]], methods: Statements[A, Function[A]], attachment: A)
        extends Stmt[A]

    /** 表达式语句 */
    case Expression[A](expr: Expr[A], attachment: A) extends Stmt[A]

    /** 函数 */
    case Function[A](name: Token, params: Seq[Token], body: Block[A], attachment: A) extends Stmt[A]

    /** 分支语句 */
    case If[A](condition: Expr[A], thenBranch: Stmt[A], elseBranch: Option[Stmt[A]], attachment: A) extends Stmt[A]

    /** print语句 */
    case Print[A](expression: Expr[A], attachment: A) extends Stmt[A]

    /** puts语句 */
    case Puts[A](expression: Expr[A], attachment: A) extends Stmt[A]

    /** 返回语句 */
    case Return[A](keyword: Token, value: Option[Expr[A]], attachment: A) extends Stmt[A]

    /** 变量声明 */
    case Variable[A](name: Token, initializer: Option[Expr[A]], attachment: A) extends Stmt[A]

    /** 循环语句 */
    case While[A](condition: Expr[A], body: Stmt[A], attachment: A) extends Stmt[A]

  /** 表达式 */
  enum Expr[A] extends Node[A]:
    /** 赋值 */
    case Assign[A](name: Token, value: Expr[A], attachment: A) extends Expr[A]

    /** 二元运算符 */
    case Binary[A](left: Expr[A], operator: Token, right: Expr[A], attachment: A) extends Expr[A]

    /** 调用 */
    case Call[A](callee: Expr[A], paren: Token, arguments: Expressions[A, Expr[A]], attachment: A) extends Expr[A]

    /** 访问 */
    case Get[A](obj: Expr[A], name: Token, attachment: A) extends Expr[A]

    /** 分组 */
    case Grouping[A](expression: Expr[A], attachment: A) extends Expr[A]

    /** 布尔真值字面量 */
    case TrueLiteral[A](attachment: A) extends Expr[A]

    /** 布尔假值字面量 */
    case FalseLiteral[A](attachment: A) extends Expr[A]

    /** 空值字面量 */
    case NilLiteral[A](attachment: A) extends Expr[A]

    /** 数字字面量 */
    case NumberLiteral[A](value: Double, attachment: A) extends Expr[A]

    /** 字符串字面量 */
    case StringLiteral[A](value: String, attachment: A) extends Expr[A]

    /** 逻辑运算符 */
    case Logical[A](left: Expr[A], operator: Token, right: Expr[A], attachment: A) extends Expr[A]

    /** 设值 */
    case Set[A](obj: Expr[A], name: Token, value: Expr[A], attachment: A) extends Expr[A]

    /** super关键字 */
    case Super[A](keyword: Token, method: Token, attachment: A) extends Expr[A]

    /** this关键字 */
    case This[A](keyword: Token, attachment: A) extends Expr[A]

    /** 一元运算符 */
    case Unary[A](operator: Token, right: Expr[A], attachment: A) extends Expr[A]

    /** 变量 */
    case Variable[A](name: Token, attachment: A) extends Expr[A]
