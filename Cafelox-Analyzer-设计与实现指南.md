# Cafelox Analyzer 设计与实现指南

> 本文档总结了关于 `cafelox.compiler.phase3_analyze.Analyzer` 的完整设计讨论，涵盖整体架构、错误处理、AST 遍历流程以及可直接使用的代码骨架。

---

## 1. 背景与目标

Cafelox 是运行在 JVM 上的 Lox 方言实现，严格遵循《Crafting Interpreters》的语义，同时吸收了 Java、Ruby 等语言的部分特性。

语义分析阶段（Resolver）位于解析之后、解释/代码生成之前，主要职责包括：

- 建立作用域与符号表
- 将变量引用、赋值、`this`、`super` 解析到具体符号与深度
- 检查静态语义错误
- 返回带有 `resolved` 信息的新 AST

Analyzer 的签名为：

```scala
def analyze(): Either[Seq[AnalyzerError], Node.Program]
```

成功时返回更新后的 `Node.Program`（所有相关节点的 `resolved` 字段已填充，`symbolTable` 完整）；失败时返回收集到的全部错误。

---

## 2. 整体设计原则

- **Lean Scala 风格**：最小权力原则，优先不可变数据与纯函数，允许良好封装的副作用。
- **不可变 AST**：节点为 `case class`，通过 `copy` 重建带 `resolved` 的新树，而非原地修改。
- **错误收集而非立即失败**：与 Parser 保持一致，尽量一次报告所有语义错误。
- **中文注释 + 英文报错消息**。
- **中英文之间不加空格**。

核心内部状态：

| 字段 | 说明 |
|------|------|
| `scopeStack` | 仅包含局部作用域（不含全局） |
| `errors` | 错误收集器 |
| `currentFunction` | 当前函数类型（None / Function / Method / Initializer） |
| `currentClass` | 当前类类型（None / Class / Subclass） |

全局作用域始终通过 `program.symbolTable.globalScope` 访问。

---

## 3. 错误处理流程

### 3.1 错误数据结构

```scala
final case class AnalyzerError(
  msg: String,           // 英文错误消息
  loc: Location,         // 出错位置
  cause: Throwable | Null = null
) extends RuntimeException(msg, cause)
```

### 3.2 收集策略

- 使用 `Seq.newBuilder[AnalyzerError]` 统一收集。
- 所有检查点通过 `error(msg, loc)` 添加错误，**不中断遍历**。
- 顶层 `analyze()` 在整棵树处理完毕后，根据 `errors` 是否为空决定返回 `Left` 或 `Right`。
- 对真正的意外异常使用 `NonFatal` 捕获并包装。

### 3.3 主要静态检查项

| 场景 | 典型错误消息 |
|------|--------------|
| 同一作用域重复声明 | Already a variable with this name in this scope. |
| 变量在自己的初始化中使用 | Can't read local variable in its own initializer. |
| 顶层 return | Can't return from top-level code. |
| 初始化器中 return 带值 | Can't return a value from an initializer. |
| this 出现在类外 | Can't use 'this' outside of a class. |
| super 出现在类外或无父类的类中 | Can't use 'super' outside of a class. / Can't use 'super' in a class with no superclass. |
| 类继承自己 | A class can't inherit from itself. |

### 3.4 与 Parser 的对比

| 维度 | Parser | Analyzer |
|------|--------|----------|
| 错误收集 | 是 | 是 |
| 是否中断 | 否（synchronize 后继续） | 否（继续遍历整棵树） |
| 恢复机制 | 显式 synchronize | 无需跳词 |
| 最终返回 | `Either[Seq[ParserError], Program]` | `Either[Seq[AnalyzerError], Program]` |

---

## 4. 遍历流程

Analyzer 采用**深度优先、自顶向下**的递归遍历，同时重建 AST。

### 4.1 顶层入口

```scala
def analyze(): Either[Seq[AnalyzerError], Node.Program] = {
  // 重置状态
  // 对 program.statements.seq 中的每个语句调用 resolveStmt
  // 根据 errors 决定返回 Left 或 Right(新 Program)
}
```

### 4.2 语句处理（resolveStmt）

按节点类型分发：

- **Block**：`beginScope(Block)` → 递归处理内部语句 → `endScope`
- **Variable**：`declare` → 解析 initializer → `define`
- **Function**：提前 `declare` + `define` 函数名（支持递归）→ 进入函数体作用域
- **Class**：处理继承、注入 `this`/`super` 作用域、解析方法
- **If / While / Print / Puts / Expression / Return**：递归处理子节点，必要时检查上下文

### 4.3 表达式处理（resolveExpr）

- **Variable**：检查是否在自己的初始化中使用 → 调用 `resolveLocal` 填充 `resolved`
- **Assign**：先解析右侧，再解析左侧目标
- **This / Super**：检查 `currentClass`，再解析
- 其它节点（Binary、Call、Get、Set、Logical、Unary、Grouping、字面量）：递归子节点并 `copy`

### 4.4 作用域管理

```scala
private def currentScope: Scope =
  if scopeStack.nonEmpty then scopeStack.top
  else program.symbolTable.globalScope

private def beginScope(scopeType: Scope.Type): Scope
private def endScope(): Unit
private def declare(name: Token, symbolType: Symbol.Type): Unit
private def define(name: Token): Unit
private def resolveLocal(name: Token): Resolved
```

`resolveLocal` 从 `scopeStack` 顶部向下查找，计算 depth；找不到则查全局作用域，返回 `Resolved.Local` 或 `Resolved.Global`。

### 4.5 关键语义保证

- 声明先于使用
- 函数名在进入函数体前已 define（支持递归）
- 类名提前 define
- `this` / `super` 通过手动注入符号实现
- 无运行时控制流剪枝（if 两个分支、while 条件与体都会被完整遍历）

---

## 5. 代码骨架

完整可运行的骨架见下方（可直接复制到 `compiler/src/main/scala/cafelox/compiler/phase3_analyze/Analyzer.scala`）：

```scala
package cafelox.compiler.phase3_analyze

import cafelox.compiler.common.Node
import cafelox.compiler.phase1_scan.Token
import cafelox.compiler.phase2_parse.Location

import scala.collection.mutable
import scala.util.control.NonFatal

/** Lox语义分析器 */
final class Analyzer(val program: Node.Program):
  import Analyzer.*

  private val scopeStack: mutable.Stack[Scope] = mutable.Stack.empty
  private val errors = Seq.newBuilder[AnalyzerError]
  private var currentFunction: FunctionType = FunctionType.None
  private var currentClass: ClassType = ClassType.None

  def analyze(): Either[Seq[AnalyzerError], Node.Program] =
    scopeStack.clear()
    errors.clear()
    currentFunction = FunctionType.None
    currentClass = ClassType.None

    try
      val newStatements = program.statements.seq.map(resolveStmt)
      val collected = errors.result()
      if collected.nonEmpty then Left(collected)
      else
        Right(program.copy(statements = program.statements.copy(seq = newStatements)))
    catch
      case NonFatal(e) =>
        errors += AnalyzerError("An error occurred during analysis.", program.loc, e)
        Left(errors.result())

  // ---------- 语句遍历 ----------
  private def resolveStmt(stmt: Node.Stmt): Node.Stmt = stmt match
    case s: Node.Stmt.Block      => resolveBlock(s)
    case s: Node.Stmt.Class      => resolveClass(s)
    case s: Node.Stmt.Expression => resolveExpressionStmt(s)
    case s: Node.Stmt.Function   => resolveFunctionStmt(s)
    case s: Node.Stmt.If         => resolveIf(s)
    case s: Node.Stmt.Print      => resolvePrint(s)
    case s: Node.Stmt.Puts       => resolvePuts(s)
    case s: Node.Stmt.Return     => resolveReturn(s)
    case s: Node.Stmt.Variable   => resolveVariable(s)
    case s: Node.Stmt.While      => resolveWhile(s)

  // 各 resolveXxx 方法实现见完整骨架
  // ...（此处省略具体实现，完整版请参考对话中的代码）

  // ---------- 表达式遍历 ----------
  private def resolveExpr(expr: Node.Expr): Node.Expr = expr match
    case e: Node.Expr.Assign   => resolveAssign(e)
    case e: Node.Expr.Binary   => resolveBinary(e)
    case e: Node.Expr.Call     => resolveCall(e)
    case e: Node.Expr.Get      => resolveGet(e)
    case e: Node.Expr.Grouping => resolveGrouping(e)
    case e: Node.Expr.Logical  => resolveLogical(e)
    case e: Node.Expr.Set      => resolveSet(e)
    case e: Node.Expr.Super    => resolveSuper(e)
    case e: Node.Expr.This     => resolveThis(e)
    case e: Node.Expr.Unary    => resolveUnary(e)
    case e: Node.Expr.Variable => resolveVariableExpr(e)
    case e: (Node.Expr.TrueLiteral | Node.Expr.FalseLiteral |
             Node.Expr.NilLiteral | Node.Expr.NumberLiteral |
             Node.Expr.StringLiteral) => e

  // ---------- 作用域与符号 ----------
  private def currentScope: Scope =
    if scopeStack.nonEmpty then scopeStack.top
    else program.symbolTable.globalScope

  private def beginScope(scopeType: Scope.Type): Scope = ???
  private def endScope(): Unit = ???
  private def declare(name: Token, symbolType: Symbol.Type): Unit = ???
  private def define(name: Token): Unit = ???
  private def resolveLocal(name: Token): Resolved = ???

  private def error(msg: String, loc: Location): Unit =
    errors += AnalyzerError(msg, loc)

object Analyzer:
  enum Resolved:
    case None
    case Global(symbol: Symbol)
    case Local(depth: Int, symbol: Symbol)

  private enum FunctionType:
    case None, Function, Method, Initializer

  private enum ClassType:
    case None, Class, Subclass
```

> **注意**：完整可复制的详细实现（包含所有方法体）已在对话中提供。建议按以下顺序填充：
> 1. 作用域管理原语
> 2. 变量声明与引用
> 3. 函数与块
> 4. 类、`this`、`super`

---

## 6. 推荐实现与测试顺序

1. **作用域原语**：`beginScope` / `endScope` / `declare` / `define` / `resolveLocal`
2. **变量相关**：Variable 声明、Variable 引用、Assign
3. **控制流与块**：Block、If、While、Print、Puts、Expression、Return
4. **函数**：Function 声明与递归支持
5. **类系统**：Class、方法、`this`、`super`、继承检查

测试重点：

- `var a = a;`（自己初始化自己）
- 顶层 `return`
- 嵌套作用域深度计算
- 方法中正确解析 `this` / `super`
- 类继承自身
- 函数递归调用

---

## 7. 与后续阶段的衔接

- 解释器根据 `resolved`：
  - `Local(depth, symbol)` → 环境链上跳 `depth` 层
  - `Global(symbol)` → 全局表查找
- `SymbolTable` 可用于调试、IDE 支持或后续优化
- `puts` 与 `print` 在分析阶段无区别，仅运行时输出行为不同

---

## 8. 参考

- [Crafting Interpreters - Resolving and Binding](https://www.craftinginterpreters.com/resolving-and-binding.html)
- 项目现有代码：`Node.scala`、`compiler/src/main/scala/cafelox/compiler/phase3_analyze/Scope.scala`、`Symbol.scala`、`SymbolTable.scala`、`Parser.scala`

---

*文档生成时间：2026-11-09*  
*基于 Cafelox 项目 Analyzer 设计讨论整理*
