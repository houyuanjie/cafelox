# Cafelox

## 介绍

Cafelox 是一个在 JVM 上实现的 Lox 编程语言方言。

Lox 是 Robert Nystrom 在《Crafting Interpreters》一书中介绍的一门动态类型脚本语言，Cafelox 基本与 Lox 相同，在 Lox 的基础上又吸收了一些来自 Java、Ruby 等语言的特性。

- [Crafting Interpreters](https://www.craftinginterpreters.com/)
- [munificent/craftinginterpreters](https://github.com/munificent/craftinginterpreters)

## 模块

- `runtime` — 运行时库
- `compiler` — 编译器
- `cli` — 开发者工具

## 开发

- `sbt test` — 执行单元测试
- `sbt co` — 测量单元测试覆盖率
- `sbt fix` — 对所有模块执行 Scalafix 和 Scalafmt

## 规范

- 使用 Lean Scala 风格，参考：[Lean Scala | Language and Code - Martin Odersky](https://odersky.github.io/blog/2024-04-11-post.html)
  - 最小权力原则：能简单解决就不使用更强的手段
  - 不可变优先，但不教条：优先不可变数据与纯函数，允许良好封装且可描述的副作用
  - 聚焦 Scala 3 语法，抛弃 Scala 2 遗留写法
  - 核心语言优先于嵌入式 DSL
  - 直接风格：避免将 monad 效果系统作为默认
- 在注释、文档信息中使用直白的简体中文
- 代码中的报错信息使用英文
- 在帮助类信息中使用英文，例如：Makefile help
- 中文和 English 之间不加空格
- 不使用中文句号
- 提交消息使用简短的简体中文，范例：

  ```
  构建：添加'sbt co'、'sbt fix'命令

  1. 添加Scoverage插件；
  2. 移除'make lintFmt'
  ```
