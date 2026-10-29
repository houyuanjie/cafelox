package cafelox.compiler.phase2_parse

import cafelox.compiler.phase1_scan.Position

final case class Location(startIncluded: Position, endExcluded: Position)
