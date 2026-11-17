package cafelox.compiler.phase3_analyze

import scala.collection.mutable

/** 语义作用域
  *
  * @param scopeId
  *   创建该作用域时赋予的一个编号，每个作用域应当不同，用来在调试时区分不同的作用域
  * @param scopeType
  *   作用域的类型
  * @param parent
  *   上层的作用域，在语义分析阶段，作用域通过`parent`指针实现向上层查找
  */
final class Scope(
    val scopeId: Int,
    val scopeType: Scope.Type,
    val parent: Option[Scope] = None
):
  import Scope.*

  /** 当前作用域内的符号 */
  private val symbolStates: mutable.Map[String, SymbolStates] = mutable.Map.empty

  /** 获取所有符号状态 */
  def allSymbols: Seq[SymbolStates] =
    symbolStates.values.toSeq

  /** 获取一个符号状态 */
  def get(name: String): Option[SymbolStates] =
    symbolStates.get(name)

  /** 检查符号 */
  def contains(name: String): Boolean =
    symbolStates.contains(name)

  /** 声明一个符号 */
  def declare(symbol: Symbol): Unit =
    symbolStates(symbol.name) = SymbolStates(symbol)

  /** 定义一个符号 */
  def define(symbol: Symbol): Unit =
    symbolStates(symbol.name) = SymbolStates(symbol, isDefined = true)

object Scope:
  /** 语义作用域的类型 */
  enum Type:
    /** 全局作用域 */
    case Global

    /** 函数作用域 */
    case Function

    /** 类作用域 */
    case Class

    /** 方法作用域 */
    case Method

    /** 块作用域 */
    case Block

  /** 跟踪当前作用域内的符号状态
    *
    * @param symbol
    *   符号
    * @param isDefined
    *   标记符号是否已经声明，用来阻止递归定义`var a = a;`
    */
  final case class SymbolStates(symbol: Symbol, isDefined: Boolean = false)
