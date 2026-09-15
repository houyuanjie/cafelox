.PHONY: all reset

all:
	@echo "all   - Print this message"
	@echo "reset - Remove all SBT/Metals/Bloop/BSP generated files"

reset:
	sbt shutdown
	$(RM) -r .bloop/
	$(RM) -r .bsp/
	$(RM) -r .metals/
	$(RM) -r project/.bloop/
	$(RM) -r project/project/
	$(RM) -r project/target/
	$(RM)    project/metals.sbt
	$(RM) -r target/
