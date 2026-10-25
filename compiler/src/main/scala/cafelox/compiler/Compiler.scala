package cafelox.compiler

import cafelox.compiler.parse.Parser
import cafelox.compiler.scan.Scanner

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
