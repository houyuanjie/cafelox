package cafelox.compiler

import cafelox.compiler.phase1_scan.Scanner
import cafelox.compiler.phase2_parse.Parser

/** Cafelox编译器演示入口
  *
  * 解析一段示例Lox代码，并将生成的抽象语法树（AST）打印到控制台
  */
object Compiler:
  /** 程序入口：扫描、解析示例代码并打印AST */
  def main(args: Array[String]): Unit =
    val loxProg =
      """|print "Hello, Scala!";
         |print "Hello, Cafelox!";""".stripMargin

    val scanner = Scanner(loxProg)
    val words   = scanner.tokenize().toOption.get

    val parser = Parser(words)
    val ast    = parser.parse().toOption.get

    pprint.pprintln(ast)
