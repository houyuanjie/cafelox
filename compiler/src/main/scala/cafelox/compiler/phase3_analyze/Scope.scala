package cafelox.compiler.phase3_analyze

import cafelox.compiler.phase2_parse.Location

import scala.collection.mutable

/** 语义作用域
  *
  * 要创建一个作用域，调用[[SymbolTable#createScope]]
  *
  * @param scopeId
  *   创建该作用域时赋予的一个编号，每个作用域应当不同，用来在调试时区分不同的作用域
  * @param scopeType
  *   作用域的类型
  */
final class Scope(
    val scopeId: Int,
    val scopeType: Scope.Type
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
  def declare(name: String, symbolType: Symbol.Type, declaration: Location): Unit =
    if contains(name) then throw Error("Already a variable with this name in this scope.", Some(declaration))

    val symbol = Symbol(name, symbolType, declaration)
    symbolStates(name) = SymbolStates(symbol)

  /** 定义一个符号 */
  def define(name: String): Unit =
    if !contains(name) then throw Error("A variable with this name has never been declared in this scope.")

    symbolStates(name) = symbolStates(name).copy(isDefined = true)

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

  /** 作用域错误
    *
    * @param msg
    *   错误消息
    * @param loc
    *   发生错误时处理的位置
    * @param cause
    *   由`cause`引发的错误
    */
  final case class Error(msg: String, loc: Option[Location] = None, cause: Throwable | Null = null)
      extends RuntimeException(msg, cause)
