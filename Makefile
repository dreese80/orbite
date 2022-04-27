Orbite:
	(cd src; $(MAKE))
	(cd classes; \
	jar cvfm ../Orbite.jar manifest.mf \
		*.class graph/Plot.class resource_files)

clean:
	rm -rf classes/*.class classes/graph
