package cafelox.compiler.phase3_analyze

import cafelox.compiler.phase1_scan.Token

/** 语义符号
  *
  * @param name
  *   名称
  * @param symbolType
  *   类型
  * @param declaration
  *   声明点
  */
final case class Symbol(name: String, symbolType: Symbol.Type, declaration: Token)

object Symbol:
  /** 符号类型 */
  enum Type:
    /** 变量 */
    case Variable

    /** 参数 */
    case Parameter

    /** 函数 */
    case Function

    /** 类 */
    case Class

    /** 方法 */
    case Method

    /** super */
    case Super

    /** this */
    case This
