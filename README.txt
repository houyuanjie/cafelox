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
* ./mill compiler.compile    # 编译compiler模块
* ./mill cli.test            # 对cli模块进行单元测试
* ./mill runtime.testCo      # 对runtime模块进行单元测试和Scoverage
* ./mill __.lintFmt          # 对所有模块执行Scalafix和Scalafmt


【规范】
* 使用Lean Scala风格
* 在注释、文档信息中使用直白的简体中文
* 中文和English之间不加空格
* 不使用中文句号
* 提交消息使用简短的简体中文，范例：
  | Build：重构代码风格，遵守Mill惯用法
  | 
  | 1. 使用os.up代替..；
  | 2. 重构testCo返回类型为PathRef
