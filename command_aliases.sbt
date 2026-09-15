addCommandAlias("ls", "projects")
addCommandAlias("cd", "project")

addCommandAlias("fix", ";scalafixAll;scalafmtAll;scalafmtSbt;")
addCommandAlias("co", ";cleanFull;coverage;testFull;coverageOff;coverageReport;coverageAggregate;")
