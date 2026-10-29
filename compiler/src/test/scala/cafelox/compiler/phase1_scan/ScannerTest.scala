package cafelox.compiler.phase1_scan

import munit.FunSuite

class ScannerTest extends FunSuite:

  // ==============================
  //           辅助
  // ==============================

  private def assertError(source: String, expectedMsgPart: String): Unit =
    Scanner(source, None).tokenize() match
      case Left(e) =>
        assert(
          e.msg.contains(expectedMsgPart),
          s"Expected error containing '$expectedMsgPart', got: '${e.msg}'"
        )
      case Right(tokens) =>
        fail(s"Expected an error but tokenize succeeded with ${tokens.size} tokens.", clues(tokens))

  private def assertSuccess(source: String): Seq[Token] =
    Scanner(source, None).tokenize() match
      case Left(e)       => fail(s"Tokenization failed: ${e.msg}", e)
      case Right(tokens) => tokens

  private def assertTokens(source: String, expectedTokens: Seq[Token]): Unit =
    val tokens = assertSuccess(source)
    assertEquals(tokens, expectedTokens)

  private def assertTokenTypes(source: String, expectedTokenTypes: Seq[TokenType]): Unit =
    val tokenTypes = assertSuccess(source).map(_.tokenType)
    assertEquals(tokenTypes, expectedTokenTypes)

  private def assertLexemes(source: String, expectedLexemes: Seq[String]): Unit =
    val lexemes = assertSuccess(source).map(_.lexeme)
    assertEquals(lexemes, expectedLexemes)

  private def pos(line: Int, col: Int, offset: Int): Position =
    Position(line, col, offset)

  // ==============================
  //           单字符Token
  // ==============================

  test("左括号"):
    val source   = "("
    val expected = Seq(TokenType.LeftParen, TokenType.EOF)
    assertTokenTypes(source, expected)

  test("右括号"):
    val source   = ")"
    val expected = Seq(TokenType.RightParen, TokenType.EOF)
    assertTokenTypes(source, expected)

  test("左花括号"):
    val source   = "{"
    val expected = Seq(TokenType.LeftBrace, TokenType.EOF)
    assertTokenTypes(source, expected)

  test("右花括号"):
    val source   = "}"
    val expected = Seq(TokenType.RightBrace, TokenType.EOF)
    assertTokenTypes(source, expected)

  test("逗号"):
    val source   = ","
    val expected = Seq(TokenType.Comma, TokenType.EOF)
    assertTokenTypes(source, expected)

  test("点号"):
    val source   = "."
    val expected = Seq(TokenType.Dot, TokenType.EOF)
    assertTokenTypes(source, expected)

  test("减号"):
    val source   = "-"
    val expected = Seq(TokenType.Minus, TokenType.EOF)
    assertTokenTypes(source, expected)

  test("加号"):
    val source   = "+"
    val expected = Seq(TokenType.Plus, TokenType.EOF)
    assertTokenTypes(source, expected)

  test("分号"):
    val source   = ";"
    val expected = Seq(TokenType.Semicolon, TokenType.EOF)
    assertTokenTypes(source, expected)

  test("星号"):
    val source   = "*"
    val expected = Seq(TokenType.Star, TokenType.EOF)
    assertTokenTypes(source, expected)

  test("斜杠"):
    val source   = "/"
    val expected = Seq(TokenType.Slash, TokenType.EOF)
    assertTokenTypes(source, expected)

  // ==============================
  //           一或二字符Token
  // ==============================

  test("感叹号"):
    val source   = "!"
    val expected = Seq(TokenType.Bang, TokenType.EOF)
    assertTokenTypes(source, expected)

  test("不等于"):
    val source   = "!="
    val expected = Seq(TokenType.BangEqual, TokenType.EOF)
    assertTokenTypes(source, expected)

  test("赋值"):
    val source   = "="
    val expected = Seq(TokenType.Equal, TokenType.EOF)
    assertTokenTypes(source, expected)

  test("等于"):
    val source   = "=="
    val expected = Seq(TokenType.EqualEqual, TokenType.EOF)
    assertTokenTypes(source, expected)

  test("小于"):
    val source   = "<"
    val expected = Seq(TokenType.Less, TokenType.EOF)
    assertTokenTypes(source, expected)

  test("小于等于"):
    val source   = "<="
    val expected = Seq(TokenType.LessEqual, TokenType.EOF)
    assertTokenTypes(source, expected)

  test("大于"):
    val source   = ">"
    val expected = Seq(TokenType.Greater, TokenType.EOF)
    assertTokenTypes(source, expected)

  test("大于等于"):
    val source   = ">="
    val expected = Seq(TokenType.GreaterEqual, TokenType.EOF)
    assertTokenTypes(source, expected)

  // ==============================
  //           字符串字面量
  // ==============================

  test("简单字符串"):
    val source          = "\"hello\""
    val expectedTypes   = Seq(TokenType.StringLiteral("hello"), TokenType.EOF)
    val expectedLexemes = Seq("\"hello\"", "")
    assertTokenTypes(source, expectedTypes)
    assertLexemes(source, expectedLexemes)

  test("空字符串"):
    val source   = "\"\""
    val expected = Seq(TokenType.StringLiteral(""), TokenType.EOF)
    assertTokenTypes(source, expected)

  test("含空格字符串"):
    val source   = "\"hello world\""
    val expected = Seq(TokenType.StringLiteral("hello world"), TokenType.EOF)
    assertTokenTypes(source, expected)

  test("多行字符串"):
    // 使用显式'\n'构造多行字符串
    val source   = "\"line1\nline2\""
    val expected = Seq(TokenType.StringLiteral("line1\nline2"), TokenType.EOF)
    assertTokenTypes(source, expected)

  test("未终止字符串"):
    val source = "\"hello"
    assertError(source, "Unterminated string")

  test("仅引号"):
    val source   = "\"\""
    val expected = Seq(TokenType.StringLiteral(""), TokenType.EOF)
    assertTokenTypes(source, expected)

  // ==============================
  //           数字字面量
  // ==============================

  test("整数"):
    val source          = "123"
    val expectedTypes   = Seq(TokenType.NumberLiteral(123.0), TokenType.EOF)
    val expectedLexemes = Seq("123", "")
    assertTokenTypes(source, expectedTypes)
    assertLexemes(source, expectedLexemes)

  test("浮点数"):
    val source          = "3.14"
    val expectedTypes   = Seq(TokenType.NumberLiteral(3.14), TokenType.EOF)
    val expectedLexemes = Seq("3.14", "")
    assertTokenTypes(source, expectedTypes)
    assertLexemes(source, expectedLexemes)

  test("零"):
    val source   = "0"
    val expected = Seq(TokenType.NumberLiteral(0.0), TokenType.EOF)
    assertTokenTypes(source, expected)

  test("大数"):
    val source   = "999999.999"
    val expected = Seq(TokenType.NumberLiteral(999999.999), TokenType.EOF)
    assertTokenTypes(source, expected)

  test("小数点前无数字"):
    // .5应该解析为Dot + NumberLiteral(5)
    val source   = ".5"
    val expected = Seq(TokenType.Dot, TokenType.NumberLiteral(5.0), TokenType.EOF)
    assertTokenTypes(source, expected)

  test("小数点后无数字"):
    // 5.应该只解析为NumberLiteral(5.0)，因为peek2要求小数点后必须是数字
    val source          = "5."
    val expectedTypes   = Seq(TokenType.NumberLiteral(5.0), TokenType.Dot, TokenType.EOF)
    val expectedLexemes = Seq("5", ".", "")
    assertTokenTypes(source, expectedTypes)
    assertLexemes(source, expectedLexemes)

  test("多个数字"):
    val source   = "1 2 3"
    val expected = Seq(
      TokenType.NumberLiteral(1.0),
      TokenType.NumberLiteral(2.0),
      TokenType.NumberLiteral(3.0),
      TokenType.EOF
    )
    assertTokenTypes(source, expected)

  // ==============================
  //           标识符
  // ==============================

  test("简单标识符"):
    val source          = "foo"
    val expectedTypes   = Seq(TokenType.Identifier, TokenType.EOF)
    val expectedLexemes = Seq("foo", "")
    assertTokenTypes(source, expectedTypes)
    assertLexemes(source, expectedLexemes)

  test("下划线开头"):
    val source          = "_var"
    val expectedTypes   = Seq(TokenType.Identifier, TokenType.EOF)
    val expectedLexemes = Seq("_var", "")
    assertTokenTypes(source, expectedTypes)
    assertLexemes(source, expectedLexemes)

  test("含数字标识符"):
    val source          = "foo123"
    val expectedTypes   = Seq(TokenType.Identifier, TokenType.EOF)
    val expectedLexemes = Seq("foo123", "")
    assertTokenTypes(source, expectedTypes)
    assertLexemes(source, expectedLexemes)

  test("含下划线标识符"):
    val source          = "foo_bar"
    val expectedTypes   = Seq(TokenType.Identifier, TokenType.EOF)
    val expectedLexemes = Seq("foo_bar", "")
    assertTokenTypes(source, expectedTypes)
    assertLexemes(source, expectedLexemes)

  // ==============================
  //           关键字
  // ==============================

  test("关键字and"):
    val source   = "and"
    val expected = Seq(TokenType.And, TokenType.EOF)
    assertTokenTypes(source, expected)

  test("关键字class"):
    val source   = "class"
    val expected = Seq(TokenType.Class, TokenType.EOF)
    assertTokenTypes(source, expected)

  test("关键字else"):
    val source   = "else"
    val expected = Seq(TokenType.Else, TokenType.EOF)
    assertTokenTypes(source, expected)

  test("关键字false"):
    val source   = "false"
    val expected = Seq(TokenType.False, TokenType.EOF)
    assertTokenTypes(source, expected)

  test("关键字for"):
    val source   = "for"
    val expected = Seq(TokenType.For, TokenType.EOF)
    assertTokenTypes(source, expected)

  test("关键字fun"):
    val source   = "fun"
    val expected = Seq(TokenType.Fun, TokenType.EOF)
    assertTokenTypes(source, expected)

  test("关键字if"):
    val source   = "if"
    val expected = Seq(TokenType.If, TokenType.EOF)
    assertTokenTypes(source, expected)

  test("关键字nil"):
    val source   = "nil"
    val expected = Seq(TokenType.Nil, TokenType.EOF)
    assertTokenTypes(source, expected)

  test("关键字or"):
    val source   = "or"
    val expected = Seq(TokenType.Or, TokenType.EOF)
    assertTokenTypes(source, expected)

  test("关键字print"):
    val source   = "print"
    val expected = Seq(TokenType.Print, TokenType.EOF)
    assertTokenTypes(source, expected)

  test("关键字puts"):
    val source   = "puts"
    val expected = Seq(TokenType.Puts, TokenType.EOF)
    assertTokenTypes(source, expected)

  test("关键字return"):
    val source   = "return"
    val expected = Seq(TokenType.Return, TokenType.EOF)
    assertTokenTypes(source, expected)

  test("关键字super"):
    val source   = "super"
    val expected = Seq(TokenType.Super, TokenType.EOF)
    assertTokenTypes(source, expected)

  test("关键字this"):
    val source   = "this"
    val expected = Seq(TokenType.This, TokenType.EOF)
    assertTokenTypes(source, expected)

  test("关键字true"):
    val source   = "true"
    val expected = Seq(TokenType.True, TokenType.EOF)
    assertTokenTypes(source, expected)

  test("关键字var"):
    val source   = "var"
    val expected = Seq(TokenType.Var, TokenType.EOF)
    assertTokenTypes(source, expected)

  test("关键字while"):
    val source   = "while"
    val expected = Seq(TokenType.While, TokenType.EOF)
    assertTokenTypes(source, expected)

  test("关键字作为前缀"):
    // "andfoo"不是关键字，应该是标识符
    val source          = "andfoo"
    val expectedTypes   = Seq(TokenType.Identifier, TokenType.EOF)
    val expectedLexemes = Seq("andfoo", "")
    assertTokenTypes(source, expectedTypes)
    assertLexemes(source, expectedLexemes)

  // ==============================
  //           空白字符
  // ==============================

  test("空格分隔"):
    val source   = "a b"
    val expected = Seq(TokenType.Identifier, TokenType.Identifier, TokenType.EOF)
    assertTokenTypes(source, expected)

  test("制表符分隔"):
    val source   = "a\tb"
    val expected = Seq(TokenType.Identifier, TokenType.Identifier, TokenType.EOF)
    assertTokenTypes(source, expected)

  test("回车分隔"):
    val source   = "a\rb"
    val expected = Seq(TokenType.Identifier, TokenType.Identifier, TokenType.EOF)
    assertTokenTypes(source, expected)

  test("换行分隔"):
    val source = "a\nb"
    val tokens = assertSuccess(source)
    assertEquals(tokens.size, 3)
    assertEquals(tokens(0).pos, pos(1, 1, 0))
    assertEquals(tokens(1).pos, pos(2, 1, 2))

  test("CRLF换行"):
    val source = "a\r\nb"
    val tokens = assertSuccess(source)
    assertEquals(tokens.size, 3)
    assertEquals(tokens(0).pos, pos(1, 1, 0))
    // \r被消费后 col=2, \n再消费后 line=2, col=1
    assertEquals(tokens(1).pos, pos(2, 1, 3))

  test("多个空白"):
    val source   = "  \t  \n  "
    val expected = Seq(TokenType.EOF)
    assertTokenTypes(source, expected)

  // ==============================
  //           注释
  // ==============================

  test("行注释"):
    val source = "// this is a comment\na"
    val tokens = assertSuccess(source)
    assertEquals(tokens.map(_.tokenType), Seq(TokenType.Identifier, TokenType.EOF))
    assertEquals(tokens(0).lexeme, "a")
    assertEquals(tokens(0).pos, pos(2, 1, 21))

  test("行注释末尾无换行"):
    val source   = "// comment"
    val expected = Seq(TokenType.EOF)
    assertTokenTypes(source, expected)

  test("块注释"):
    val source = "/* comment */a"
    val tokens = assertSuccess(source)
    assertEquals(tokens.map(_.tokenType), Seq(TokenType.Identifier, TokenType.EOF))
    assertEquals(tokens(0).lexeme, "a")

  test("块注释多行"):
    val source = "/* line1\nline2 */a"
    val tokens = assertSuccess(source)
    assertEquals(tokens.map(_.tokenType), Seq(TokenType.Identifier, TokenType.EOF))
    assertEquals(tokens(0).lexeme, "a")

  test("块注释嵌套星号"):
    val source   = "/* * * */a"
    val expected = Seq(TokenType.Identifier, TokenType.EOF)
    assertTokenTypes(source, expected)

  test("未终止块注释"):
    val source = "/* comment"
    assertError(source, "Unterminated block comment")

  test("注释后紧跟代码"):
    val source   = "/*comment*/1+2"
    val expected = Seq(
      TokenType.NumberLiteral(1.0),
      TokenType.Plus,
      TokenType.NumberLiteral(2.0),
      TokenType.EOF
    )
    assertTokenTypes(source, expected)

  // ==============================
  //           组合测试
  // ==============================

  test("简单表达式"):
    val source   = "1 + 2"
    val expected = Seq(
      TokenType.NumberLiteral(1.0),
      TokenType.Plus,
      TokenType.NumberLiteral(2.0),
      TokenType.EOF
    )
    assertTokenTypes(source, expected)

  test("变量声明"):
    val source   = "var x = 10;"
    val expected = Seq(
      TokenType.Var,
      TokenType.Identifier,
      TokenType.Equal,
      TokenType.NumberLiteral(10.0),
      TokenType.Semicolon,
      TokenType.EOF
    )
    assertTokenTypes(source, expected)

  test("函数调用"):
    val source   = "print(\"hello\");"
    val expected = Seq(
      TokenType.Print,
      TokenType.LeftParen,
      TokenType.StringLiteral("hello"),
      TokenType.RightParen,
      TokenType.Semicolon,
      TokenType.EOF
    )
    assertTokenTypes(source, expected)

  test("比较表达式"):
    val source   = "a <= b"
    val expected = Seq(
      TokenType.Identifier,
      TokenType.LessEqual,
      TokenType.Identifier,
      TokenType.EOF
    )
    assertTokenTypes(source, expected)

  test("复杂代码片段"):
    val source   = "if (x == 10) {\n  print(\"yes\");\n}"
    val expected = Seq(
      TokenType.If,
      TokenType.LeftParen,
      TokenType.Identifier,
      TokenType.EqualEqual,
      TokenType.NumberLiteral(10.0),
      TokenType.RightParen,
      TokenType.LeftBrace,
      TokenType.Print,
      TokenType.LeftParen,
      TokenType.StringLiteral("yes"),
      TokenType.RightParen,
      TokenType.Semicolon,
      TokenType.RightBrace,
      TokenType.EOF
    )
    assertTokenTypes(source, expected)

  // ==============================
  //           错误处理
  // ==============================

  test("意外字符"):
    val source = "@"
    assertError(source, "Unexpected character")

  test("意外字符在中间"):
    val source = "abc@def"
    assertError(source, "Unexpected character")

  test("意外字符Unicode"):
    val source = "€"
    assertError(source, "Unexpected character")

  // ==============================
  //           位置信息
  // ==============================

  test("位置追踪简单"):
    val source = "a b"
    val tokens = assertSuccess(source)
    assertEquals(tokens(0).pos, pos(1, 1, 0))
    assertEquals(tokens(1).pos, pos(1, 3, 2))

  test("位置追踪换行"):
    val source = "a\nb"
    val tokens = assertSuccess(source)
    assertEquals(tokens(0).pos, pos(1, 1, 0))
    assertEquals(tokens(1).pos, pos(2, 1, 2))

  test("位置追踪多字符"):
    val source = "== != <= >="
    val tokens = assertSuccess(source)
    assertEquals(tokens(0).pos, pos(1, 1, 0))  // ==
    assertEquals(tokens(1).pos, pos(1, 4, 3))  // !=
    assertEquals(tokens(2).pos, pos(1, 7, 6))  // <=
    assertEquals(tokens(3).pos, pos(1, 10, 9)) // >=

  test("位置追踪字符串"):
    val source = "\"hi\" x"
    val tokens = assertSuccess(source)
    assertEquals(tokens(0).pos, pos(1, 1, 0)) // "hi"
    assertEquals(tokens(1).pos, pos(1, 6, 5)) // x

  test("位置追踪注释"):
    val source = "// comment\nfoo"
    val tokens = assertSuccess(source)
    assertEquals(tokens(0).pos, pos(2, 1, 11)) // foo

  test("位置追踪块注释"):
    val source = "/* abc */foo"
    val tokens = assertSuccess(source)
    assertEquals(tokens(0).pos, pos(1, 10, 9)) // foo

  // ==============================
  //           词素检查
  // ==============================

  test("词素双字符"):
    val source   = "=="
    val expected = Seq("==", "")
    assertLexemes(source, expected)

  test("词素关键字"):
    val source   = "while"
    val expected = Seq("while", "")
    assertLexemes(source, expected)

  test("词素数字"):
    val source   = "42.5"
    val expected = Seq("42.5", "")
    assertLexemes(source, expected)

  test("词素字符串"):
    val source   = "\"test\""
    val expected = Seq("\"test\"", "")
    assertLexemes(source, expected)

  // ==============================
  //           边界情况
  // ==============================

  test("空输入"):
    val source   = ""
    val expected = Seq(Token(TokenType.EOF, "", pos(1, 1, 0)))
    assertTokens(source, expected)

  test("仅空白"):
    val source   = "   \t\n  "
    val expected = Seq(TokenType.EOF)
    assertTokenTypes(source, expected)

  test("仅注释"):
    val source   = "// only comment"
    val expected = Seq(TokenType.EOF)
    assertTokenTypes(source, expected)

  test("连续双字符"):
    val source   = "===="
    val expected = Seq(TokenType.EqualEqual, TokenType.EqualEqual, TokenType.EOF)
    assertTokenTypes(source, expected)

  test("感叹号后非等号"):
    val source   = "!a"
    val expected = Seq(TokenType.Bang, TokenType.Identifier, TokenType.EOF)
    assertTokenTypes(source, expected)

  test("等号后非等号"):
    val source   = "=a"
    val expected = Seq(TokenType.Equal, TokenType.Identifier, TokenType.EOF)
    assertTokenTypes(source, expected)

  test("斜杠后非注释"):
    val source   = "/a"
    val expected = Seq(TokenType.Slash, TokenType.Identifier, TokenType.EOF)
    assertTokenTypes(source, expected)

  test("数字后接标识符"):
    // 123是数字，abc是标识符
    val source   = "123abc"
    val expected = Seq(TokenType.NumberLiteral(123.0), TokenType.Identifier, TokenType.EOF)
    assertTokenTypes(source, expected)

  test("块注释后无内容"):
    val source   = "/* done */"
    val expected = Seq(TokenType.EOF)
    assertTokenTypes(source, expected)
