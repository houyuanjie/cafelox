package cafelox.compiler.phase1_scan

import java.nio.file.Path

/** 一个处于文本中的具体位置
  *
  * @param line
  *   行号，从1开始
  * @param col
  *   列号，从1开始
  * @param offset
  *   相对文本开头的距离，从0开始
  * @param path
  *   文本文件存储的路径
  */
final case class Position(
    line: Int,
    col: Int,
    offset: Int,
    path: Option[Path] = None
)
