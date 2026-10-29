package cafelox.compiler

import cafelox.compiler.phase1_scan.Scanner
import cafelox.compiler.phase2_parse.Parser

object Compiler:
  def main(args: Array[String]): Unit =
    val loxProg =
      """|print "Hello, Scala!";
         |print "Hello, Cafelox!";""".stripMargin

    val scanner = Scanner(loxProg)
    val words   = scanner.tokenize().toOption.get

    val parser = Parser(words)
    val ast    = parser.parse().toOption.get

    pprint.pprintln(ast)
