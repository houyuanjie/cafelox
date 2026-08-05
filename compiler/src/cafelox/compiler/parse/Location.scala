package cafelox.compiler.parse

import cafelox.compiler.scan.Position

final case class Location(start: Position, end: Position)
