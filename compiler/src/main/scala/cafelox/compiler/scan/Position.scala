package cafelox.compiler.scan

import java.nio.file.Path

final case class Position(
    line: Int,
    col: Int,
    offset: Int,
    path: Option[Path] = None
)
