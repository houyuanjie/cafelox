package cafelox.compiler.phase3_analyze

import cafelox.compiler.phase2_parse.Node
import cafelox.compiler.phase2_parse.Parser

import scala.collection.mutable

/** Lox语义分析器 */
final class Analyzer(val program: Node.Program[Parser.ParsingPhase]):

  import Analyzer.*

  /** 符号表 */
  private val symbolTable = SymbolTable()

  /** 作用域栈 (仅包含局部作用域，不包含全局作用域) */
  private val scopeStack: mutable.Stack[Scope] = mutable.Stack.empty

  /** 收集语义错误 */
  private val errors = Seq.newBuilder[AnalyzerError]

  /** 当前所在的函数类型 */
  private var currentFunction: FunctionType = FunctionType.None

  /** 当前所在的类类型 */
  private var currentClass: ClassType = ClassType.None

  /** 语义分析 */
  def analyze(): Either[Seq[AnalyzerError], Result] =
    ???

object Analyzer:
  /** 符号解析结果：变量，赋值，this，super */
  enum Resolved:
    case None
    case Global(symbol: Symbol)
    case Local(depth: Int, symbol: Symbol)

  /** AST附加信息 */
  final case class AnalyzingPhase(resolved: Resolved, parsingPhase: Parser.ParsingPhase)

  /** 语义分析结果 */
  final case class Result(analyzed: Node.Program[AnalyzingPhase], symbols: SymbolTable)

  // 内部状态

  /** 区分函数类型 */
  private enum FunctionType:
    case None, Function, Method, Initializer

  /** 区分类类型 */
  private enum ClassType:
    case None, Class, Subclass
