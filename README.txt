Cafelox


【介绍】
Cafelox是一个在JVM上实现的Lox编程语言方言

Lox是Robert Nystrom在《Crafting Interpreters》一书中介绍的一门动态类型脚本语言，
Cafelox基本与Lox相同，在Lox的基础上又吸收了一些来自Java、Ruby等语言的特性

* https://www.craftinginterpreters.com/
* https://github.com/munificent/craftinginterpreters


【模块】
* runtime     # 运行时库
* compiler    # 编译器
* cli         # 开发者工具


【开发】
* sbt test        # 执行单元测试
* make lintFmt    # 对所有模块执行Scalafix和Scalafmt


【规范】
* 使用Lean Scala风格
* 在注释、文档信息中使用直白的简体中文
* 中文和English之间不加空格
* 不使用中文句号
* 提交消息使用简短的简体中文，范例：
  | 重构：构建系统迁移，Mill -> SBT
  | 
  | 1. 移除Scoverage；
  | 2. 使用SBT项目结构
