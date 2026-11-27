# Cafelox — agent notes

Lox dialect on the JVM. Scala 3.9.0, SBT 2.1.0 (pinned in `project/build.properties`), Java ≥ 25.

## Modules
- `runtime` — runtime lib (`cafelox.runtime`, `CAFELOX_RUNTIME_VERSION = "0.0.1"`)
- `compiler` — depends on `runtime`; phases `phase1_scan` → `phase2_parse` → `phase3_analyze` under `cafelox.compiler.*`; `Compiler` object has `main`
- `cli` — depends on `compiler`; `@main def cli(args: String*)` in `cafelox.cli.cli`
- Root project `cafelox` aggregates all three (`publish / skip := true`)

## Commands (run from repo root)
- `sbt test` — all unit tests (munit)
- `sbt compiler/test` — one module's tests (likewise `runtime/test`, `cli/test`)
- `sbt "compiler/testOnly cafelox.compiler.phase1_scan.ScannerTest"` — one test class; munit filters go after `--`
- `sbt cli/run` — run the CLI entrypoint
- `sbt co` — coverage: `cleanFull → coverage → testFull → coverageOff → coverageReport → coverageAggregate` (alias in `command_aliases.sbt`)
- `sbt fix` — `scalafixAll; scalafmtAll; scalafmtSbt` (alias in `command_aliases.sbt`)

## Formatting
- scalafmt 3.11.5 (`.scalafmt.conf`): scala3 dialect, optional braces, `maxColumn = 120`, rewrites AvoidInfix/SortModifiers/RedundantBraces/RedundantParens
- scalafix: `OrganizeImports`, target dialect Scala 3 (`.scalafix.conf`)

## Compiler flags worth knowing
- `-release 25`, `-Yexplicit-nulls` (nullable types must be written `T | Null`), `-Wall -explain -feature -deprecation -unchecked`; `-Werror` is commented out
- `semanticdbEnabled := true` (Metals/Bloop)

## Tests
- munit `FunSuite`; existing test names use Chinese — that's accepted convention
- Tests in `src/test/scala/<module>/...`

## Conventions
- Comments and docs: Simplified Chinese; error messages and CLI help: English
- Commit messages: plain Simplified Chinese
- Style: "Lean Scala" — immutable-first, direct style, Scala 3 only, no effect systems as default (`STYLE_GUIDE.md`)

## Workspace hygiene
- `target/`, `.bsp/`, `.bloop/`, `.metals/`, `project/target/` etc. are gitignored
- `make reset` — run `sbt shutdown` first, then removes SBT/Metals/Bloop/BSP generated files

## CI
- None (`/.github` doesn't exist); verification is local via sbt.
