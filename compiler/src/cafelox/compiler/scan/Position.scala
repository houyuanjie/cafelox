package cafelox.compiler.scan

import java.nio.file.Path

final case class Position(
    path: Option[Path],
    line: Int,
    col: Int,
    offset: Int
)
