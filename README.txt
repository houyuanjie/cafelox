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
* sbt test    # 执行单元测试
* sbt co      # 测量单元测试覆盖率
* sbt fix     # 对所有模块执行Scalafix和Scalafmt


【规范】
* 使用Lean Scala风格，参考：https://odersky.github.io/blog/2024-04-11-post.html
  * 最小权力原则：能简单解决就不使用更强的手段
  * 不可变优先，但不教条：优先不可变数据与纯函数，允许良好封装且可描述的副作用
  * 聚焦Scala 3语法，抛弃Scala 2遗留写法
  * 核心语言优先于嵌入式DSL
  * 直接风格：避免将monad效果系统作为默认
* 在注释、文档信息中使用直白的简体中文
* 代码中的报错信息使用英文
* 在帮助类信息中使用英文，例如：Makefile help
* 中文和English之间不加空格
* 不使用中文句号
* 提交消息使用简短的简体中文，范例：
  | 构建：添加'sbt co'、'sbt fix'命令
  | 
  | 1. 添加Scoverage插件；
  | 2. 移除'make lintFmt'
