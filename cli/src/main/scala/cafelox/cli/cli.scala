package cafelox.cli

@main
def cli(args: String*): Unit =
  println("Hello, Cafelox!")
  println(s"args=${args.mkString(",")}")
