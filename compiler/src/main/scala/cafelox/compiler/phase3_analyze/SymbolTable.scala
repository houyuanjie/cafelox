package cafelox.compiler.phase3_analyze

/** 符号表 */
final class SymbolTable:
  /** 保留所有创建过的作用域 */
  private var scopes: List[Scope] = Nil

  /** 下一个作用域的编号，调试时方便区分作用域的创建顺序 */
  private var nextScopeId: Int = 0

  /** 获取所有作用域 */
  def allScopes: Seq[Scope] =
    scopes

  /** 创建一个作用域 */
  def createScope(scopeType: Scope.Type, parent: Option[Scope] = None): Scope =
    val scope = Scope(nextScopeId, scopeType, parent)
    nextScopeId += 1
    scopes = scope :: scopes
    scope

  /** 全局作用域 */
  val globalScope: Scope = createScope(Scope.Type.Global)
