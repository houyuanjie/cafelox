package cafelox.compiler.phase3_analyze

import cafelox.compiler.common.Node
import cafelox.compiler.phase2_parse.Location

import scala.collection.mutable
import scala.util.boundary

/** Lox语义分析器
  *
  * @param program
  *   AST
  */
final class Analyzer(val program: Node.Program):
  import Analyzer.*

  /** 符号表 */
  private val symbolTable: SymbolTable = SymbolTable()

  /** 作用域栈 (仅包含当前有效的局部作用域，不包含全局作用域) */
  private val scopeStack: mutable.Stack[Scope] = mutable.Stack.empty

  /** 收集语义错误 */
  private val errors = Seq.newBuilder[AnalyzerError]

  /** 当前所在的函数类型 */
  private var currentFunction: FunctionType = FunctionType.None

  /** 当前所在的类类型 */
  private var currentClass: ClassType = ClassType.None

  /** 执行语义分析，返回更新后的AST */
  def analyze(): Either[Seq[AnalyzerError], Node.Program] =
    // 清空符号表
    symbolTable.clear()
    // 清空作用域栈
    scopeStack.clear()
    // 清空上次的错误
    errors.clear()

    // 状态归位
    currentFunction = FunctionType.None
    currentClass = ClassType.None

    ???

  // ==============================
  //           作用域
  // ==============================

  /** 全局作用域 */
  private def globalScope: Scope = symbolTable.getGlobalScope

  /** 当前作用域 */
  private def currentScope: Scope =
    if scopeStack.isEmpty then globalScope
    else scopeStack.top

  /** 开启一个新的作用域
    *
    * 开启后，仅能通过调用[[currentScope]]来访问
    *
    * 最后必须调用[[endScope]]来关闭这个作用域，关闭后无法再次访问
    */
  private def beginScope(scopeType: Scope.Type): Unit =
    scopeStack += symbolTable.createScope(scopeType)

  /** 关闭当前作用域 */
  private def endScope(): Unit =
    if scopeStack.nonEmpty then scopeStack.pop(): Unit

  // ==============================
  //           符号
  // ==============================

  /** 在当前作用域中声明一个符号 */
  private def declare(name: String, symbolType: Symbol.Type, declaration: Location): Unit =
    currentScope.declare(name, symbolType, declaration)

  /** 在当前作用域中定义一个符号 */
  private def define(name: String): Unit =
    currentScope.define(name)

  /** 在[[scopeStack]]上的局部作用域中查找 */
  private def lookupLocal(name: String): Resolved =
    var depth = 0

    boundary:
      for scope <- scopeStack do
        scope.get(name) match
          case Some(ss) => boundary.break(Resolved.Local(scope, depth, ss.symbol))
          case None     => depth += 1
      Resolved.None

  /** 在[[globalScope]]全局作用域中查找 */
  private def lookupGlobal(name: String): Resolved =
    globalScope.get(name) match
      case Some(ss) => Resolved.Global(globalScope, ss.symbol)
      case None     => Resolved.None

  /** 查找符号 */
  private def lookup(name: String): Resolved =
    var resolved = lookupLocal(name)
    if resolved == Resolved.None then resolved = lookupGlobal(name)
    resolved

object Analyzer:
  /** 符号解析结果 */
  enum Resolved:
    /** 未解析的符号 */
    case None

    /** 全局符号 */
    case Global(scope: Scope, symbol: Symbol)

    /** 局部符号 */
    case Local(scope: Scope, depth: Int, symbol: Symbol)

  // 内部状态

  /** 区分函数类型 */
  private enum FunctionType:
    case None, Function, Method, Initializer

  /** 区分类类型 */
  private enum ClassType:
    case None, Class, Subclass
