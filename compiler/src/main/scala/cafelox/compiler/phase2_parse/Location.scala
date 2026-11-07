package cafelox.compiler.phase2_parse

import cafelox.compiler.phase1_scan.Position

/** 一段选中文本的范围
  *
  * @param startIncluded
  *   开始位置，包含在内容中
  * @param endExcluded
  *   结束位置，不包含
  */
final case class Location(startIncluded: Position, endExcluded: Position)
