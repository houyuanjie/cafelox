package cafelox.compiler.phase3_analyze

/** 符号表，用于创建和管理所有作用域 */
final class SymbolTable:
  /** 保留所有创建过的作用域 */
  private var allScopes: List[Scope] = Nil

  /** 下一个作用域的编号，调试时方便区分作用域的创建顺序 */
  private var nextScopeId: Int = 0

  /** 获取所有作用域 */
  def getAllScopes: Seq[Scope] = allScopes

  /** 创建一个作用域 */
  def createScope(scopeType: Scope.Type): Scope =
    val scope = Scope(nextScopeId, scopeType)
    nextScopeId += 1
    allScopes = scope :: allScopes
    scope

  /** 全局作用域 */
  private var globalScope: Scope = createScope(Scope.Type.Global)

  /** 获取全局作用域 */
  def getGlobalScope: Scope = globalScope

  /** 清空符号表，删除表中创建过的所有作用域 */
  def clear(): Unit =
    allScopes = Nil
    nextScopeId = 0
    globalScope = createScope(Scope.Type.Global)
