package cafelox.compiler.parse

import cafelox.compiler.scan.Scanner
import munit.FunSuite

class ParserTest extends FunSuite:

  // ==============================
  //           辅助
  // ==============================

  private def assertParseSuccess(source: String): Unit =
    Scanner(source, None).tokenize() match
      case Left(e)       => fail(s"扫描失败: ${e.msg}", e)
      case Right(tokens) =>
        val parser = new Parser(tokens)
        parser.parse() match
          case Left(errs) => fail(s"解析失败: $errs")
          case Right(_)   => ()

  private def assertParseError(source: String, expectedMsgPart: String): Unit =
    Scanner(source, None).tokenize() match
      case Left(e)       => fail(s"扫描失败: ${e.msg}", e)
      case Right(tokens) =>
        val parser = new Parser(tokens)
        parser.parse() match
          case Left(errs) =>
            assert(
              errs.exists(_.msg.contains(expectedMsgPart)),
              s"期望错误包含 '$expectedMsgPart'，实际得到: $errs"
            )
          case Right(_) =>
            fail(s"期望报错，但解析成功")

  // ==============================
  //           空程序 / 基础语句
  // ==============================

  test("空程序"):
    assertParseSuccess("")

  test("仅表达式语句"):
    assertParseSuccess("print true;")

  test("puts 语句"):
    assertParseSuccess("puts \"hello\";")

  test("多条顶层语句"):
    assertParseSuccess(
      """|var x = 1;
         |print x;
         |puts x;""".stripMargin
    )

  // ==============================
  //           变量声明
  // ==============================

  test("var 无初始化"):
    assertParseSuccess("var x;")

  test("var 有初始化"):
    assertParseSuccess("var x = 1;")

  test("var 复杂初始化"):
    assertParseSuccess("var x = (1 + 2) * 3;")

  test("连续 var 声明"):
    assertParseSuccess(
      """|var a = 1;
         |var b = 2;
         |var c;""".stripMargin
    )

  // ==============================
  //           控制流
  // ==============================

  test("if 语句"):
    assertParseSuccess("if (true) { print 1; }")

  test("if-else 语句"):
    assertParseSuccess("if (true) { print 1; } else { print 2; }")

  test("if-else if-else"):
    assertParseSuccess(
      """|if (a) { print 1; }
         |else if (b) { print 2; }
         |else { print 3; }""".stripMargin
    )

  test("while 循环"):
    assertParseSuccess("while (true) { print 1; }")

  test("while 单语句体"):
    assertParseSuccess("while (i < 10) print i;")

  test("for 完整形式"):
    assertParseSuccess("for (var i = 0; i < 10; i = i + 1) { print i; }")

  test("for 无初始化"):
    assertParseSuccess("for (; i < 10; i = i + 1) { print i; }")

  test("for 无条件"):
    assertParseSuccess("for (var i = 0; ; i = i + 1) { print i; }")

  test("for 无更新"):
    assertParseSuccess("for (var i = 0; i < 10; ) { print i; }")

  test("for 全空 (无限循环)"):
    assertParseSuccess("for (;;) { print 1; }")

  test("for 表达式初始化"):
    assertParseSuccess("for (i = 0; i < 10; i = i + 1) { print i; }")

  test("块语句"):
    assertParseSuccess("{ var x = 1; print x; }")

  test("嵌套块"):
    assertParseSuccess(
      """|{
         |  var a = 1;
         |  {
         |    var b = 2;
         |    print a + b;
         |  }
         |}""".stripMargin
    )

  // ==============================
  //           函数
  // ==============================

  test("空函数"):
    assertParseSuccess("fun foo() { }")

  test("带参数函数"):
    assertParseSuccess("fun add(a, b) { return a + b; }")

  test("单参数函数"):
    assertParseSuccess("fun id(x) { return x; }")

  test("多参数函数"):
    assertParseSuccess("fun f(a, b, c, d) { }")

  test("嵌套函数"):
    assertParseSuccess(
      """|fun outer() {
         |  fun inner() { return 1; }
         |  return inner();
         |}""".stripMargin
    )

  test("函数体内声明与语句"):
    assertParseSuccess(
      """|fun f() {
         |  var x = 1;
         |  if (x > 0) { return x; }
         |  return 0;
         |}""".stripMargin
    )

  // ==============================
  //           类与方法
  // ==============================

  test("空类"):
    assertParseSuccess("class Foo { }")

  test("带方法的类"):
    assertParseSuccess(
      """|class Point {
         |  init(x, y) { }
         |}""".stripMargin
    )

  test("类中多方法"):
    assertParseSuccess(
      """|class Point {
         |  init(x, y) { }
         |  distance() { return 0; }
         |  toString() { return "Point"; }
         |}""".stripMargin
    )

  test("子类"):
    assertParseSuccess(
      """|class Dog < Animal {
         |  bark() { }
         |}""".stripMargin
    )

  test("子类带多个方法"):
    assertParseSuccess(
      """|class Dog < Animal {
         |  init(name) { }
         |  bark() { print "woof"; }
         |  eat() { }
         |}""".stripMargin
    )

  // ==============================
  //           this / super
  // ==============================

  test("this 关键字"):
    assertParseSuccess("print this;")

  test("this 属性访问"):
    assertParseSuccess("print this.x;")

  test("this 方法调用"):
    assertParseSuccess(
      """|class A {
         |  init() { }
         |  get() { return this.init(); }
         |}""".stripMargin
    )

  test("this 链式"):
    assertParseSuccess("print this.prop.method();")

  test("super 方法调用"):
    assertParseSuccess(
      """|class A < B {
         |  init() { super.init(); }
         |}""".stripMargin
    )

  test("super 属性访问"):
    assertParseSuccess(
      """|class A < B {
         |  init() { print super.init; }
         |}""".stripMargin
    )

  test("super 链式调用"):
    assertParseSuccess(
      """|class A < B {
         |  init() { super.init().foo(); }
         |}""".stripMargin
    )

  // ==============================
  //           字面量与基础表达式
  // ==============================

  test("数字字面量"):
    assertParseSuccess("print 123;")

  test("浮点数字面量"):
    assertParseSuccess("print 3.14;")

  test("字符串字面量"):
    assertParseSuccess("print \"hello\";")

  test("布尔值"):
    assertParseSuccess("print true;")
    assertParseSuccess("print false;")

  test("nil"):
    assertParseSuccess("print nil;")

  test("分组表达式"):
    assertParseSuccess("print (1 + 2) * 3;")

  test("深层分组"):
    assertParseSuccess("print (((1)));")

  // ==============================
  //           赋值
  // ==============================

  test("简单赋值"):
    assertParseSuccess("var x = 1; x = 2;")

  test("赋值右结合 a = b = c"):
    assertParseSuccess("var a; var b; var c; a = b = c = 1;")

  test("属性赋值"):
    assertParseSuccess("obj.prop = 1;")

  test("链式属性赋值"):
    assertParseSuccess("obj.a.b = 1;")

  test("调用后属性赋值"):
    assertParseSuccess("foo().bar = 1;")

  test("赋值作为表达式"):
    assertParseSuccess("print a = 1;")

  // ==============================
  //           调用与链式
  // ==============================

  test("无参数调用"):
    assertParseSuccess("print foo();")

  test("带参数调用"):
    assertParseSuccess("print add(1, 2);")

  test("多参数调用"):
    assertParseSuccess("print f(1, 2, 3, 4);")

  test("链式调用完整组合"):
    assertParseSuccess("print foo().bar().baz();")
    assertParseSuccess("print foo().bar().baz;")
    assertParseSuccess("print foo().bar.baz();")
    assertParseSuccess("print foo().bar.baz;")
    assertParseSuccess("print foo.bar().baz();")
    assertParseSuccess("print foo.bar().baz;")
    assertParseSuccess("print foo.bar.baz();")
    assertParseSuccess("print foo.bar.baz;")

  test("连续调用"):
    assertParseSuccess("print foo()();")
    assertParseSuccess("print foo()()();")

  test("分组后调用"):
    assertParseSuccess("print (foo).bar();")
    assertParseSuccess("print (foo())();")

  // ==============================
  //           运算符与优先级
  // ==============================

  test("算术运算"):
    assertParseSuccess("print 1 + 2;")
    assertParseSuccess("print 5 - 3;")
    assertParseSuccess("print 3 * 4;")
    assertParseSuccess("print 10 / 2;")
    assertParseSuccess("print 1 + 2 * 3;")
    assertParseSuccess("print (1 + 2) * 3;")

  test("比较运算"):
    assertParseSuccess("print a == b;")
    assertParseSuccess("print a != b;")
    assertParseSuccess("print a < b;")
    assertParseSuccess("print a <= b;")
    assertParseSuccess("print a > b;")
    assertParseSuccess("print a >= b;")

  test("equality 低于 comparison"):
    // a == b < c  应解析为 a == (b < c)
    assertParseSuccess("print a == b < c;")
    assertParseSuccess("print a != b >= c;")

  test("逻辑运算"):
    assertParseSuccess("print a and b;")
    assertParseSuccess("print a or b;")
    assertParseSuccess("print a and b or c;")
    assertParseSuccess("print a or b and c;")

  test("前缀运算"):
    assertParseSuccess("print !a;")
    assertParseSuccess("print -x;")
    assertParseSuccess("print !!true;")
    assertParseSuccess("print --x;")
    assertParseSuccess("print !-a;")

  test("混合优先级综合"):
    assertParseSuccess("print !a + b * c == d < e and f or g;")
    assertParseSuccess("print a = b = c + d * e;")

  // ==============================
  //           return
  // ==============================

  test("return 带值"):
    assertParseSuccess("fun f() { return 1; }")

  test("空 return"):
    assertParseSuccess("fun f() { return; }")

  test("return 复杂表达式"):
    assertParseSuccess("fun f() { return a + b * c; }")

  // ==============================
  //           错误：语法结构
  // ==============================

  test("缺少分号"):
    assertParseError("print 1", "Expect ';' after value")

  test("类缺少左花括号"):
    assertParseError("class Foo", "Expect '{' before class body")

  test("类缺少右花括号"):
    assertParseError("class Foo {", "Expect '}' after class body")

  test("函数缺少参数左括号"):
    assertParseError("fun foo x { }", "Expect '(' after function name")

  test("函数缺少右括号"):
    assertParseError("fun foo( { }", "Expect parameter name.")

  test("函数缺少函数体左花括号"):
    assertParseError("fun foo()", "Expect '{' before function body")

  test("for 缺少左括号"):
    assertParseError("for i { }", "Expect '(' after 'for'")

  test("for 缺少右括号"):
    assertParseError("for (var i = 0; i < 10; i = i + 1 { }", "Expect ')' after for clauses")

  test("if 缺少左括号"):
    assertParseError("if true { }", "Expect '(' after 'if'")

  test("if 缺少右括号"):
    assertParseError("if (true { }", "Expect ')' after if condition")

  test("while 缺少左括号"):
    assertParseError("while true { }", "Expect '(' after 'while'")

  test("块缺少右花括号"):
    assertParseError("{ print 1;", "Expect '}' after block")

  // ==============================
  //           错误：赋值目标
  // ==============================

  test("非法赋值目标 - 字面量"):
    assertParseError("1 = 2;", "Invalid assignment target")

  test("非法赋值目标 - 分组"):
    assertParseError("(a) = 2;", "Invalid assignment target")

  test("非法赋值目标 - 二元表达式"):
    assertParseError("a + b = 1;", "Invalid assignment target")

  test("非法赋值目标 - 调用"):
    assertParseError("foo() = 1;", "Invalid assignment target")

  // ==============================
  //           错误：尾随逗号
  // ==============================

  test("函数参数尾随逗号"):
    assertParseError("fun f(a,) { }", "Don't trail a comma")

  test("函数参数多个尾随逗号"):
    assertParseError("fun f(a, b,) { }", "Don't trail a comma")

  test("调用参数尾随逗号"):
    assertParseError("print f(1,);", "Don't trail a comma")

  test("调用参数多个尾随逗号"):
    assertParseError("print f(1, 2,);", "Don't trail a comma")

  // ==============================
  //           错误：super / this 相关
  // ==============================

  test("super 缺少点"):
    assertParseError("class A < B { init() { super; } }", "Expect '.' after 'super'")

  test("super 缺少方法名"):
    assertParseError("class A < B { init() { super.; } }", "Expect superclass method name")

  // ==============================
  //           错误：其它常见
  // ==============================

  test("缺少表达式"):
    assertParseError("print ;", "Invalid primary expression")

  test("类名缺失"):
    assertParseError("class { }", "Expect class name")

  test("方法名缺失"):
    assertParseError("class C { () { } }", "Expect method name")

  test("参数名缺失"):
    assertParseError("fun f(,) { }", "Expect parameter name")

  test("for 条件后缺少分号"):
    assertParseError("for (var i = 0 i < 10; i = i + 1) { }", "Expect ';' after variable declaration")

  // ==============================
  //           边界与组合
  // ==============================

  test("深层嵌套表达式"):
    assertParseSuccess("print ((((1 + 2) * 3) - 4) / 5);")

  test("嵌套控制流"):
    assertParseSuccess(
      """|for (var i = 0; i < 10; i = i + 1) {
         |  if (i == 0) {
         |    while (true) {
         |      print i;
         |    }
         |  } else {
         |    print -i;
         |  }
         |}""".stripMargin
    )

  test("复杂类 + 方法体"):
    assertParseSuccess(
      """|class Point {
         |  init(x, y) {
         |    this.x = x;
         |    this.y = y;
         |  }
         |
         |  distance(other) {
         |    var dx = this.x - other.x;
         |    var dy = this.y - other.y;
         |    return dx * dx + dy * dy;
         |  }
         |}""".stripMargin
    )

  test("顶层混合声明与语句"):
    assertParseSuccess(
      """|var g = 10;
         |
         |fun helper(n) {
         |  return n * 2;
         |}
         |
         |class Counter {
         |  init() { this.n = 0; }
         |  inc() { this.n = this.n + 1; }
         |}
         |
         |print helper(g);
         |var c = Counter();
         |c.inc();""".stripMargin
    )
