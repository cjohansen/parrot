test:
	clojure -A:dev:test

parrot.jar: src/parrot/*.*
	clojure -A:jar

clean:
	rm -fr target parrot.jar

deploy: parrot.jar
	mvn deploy:deploy-file -Dfile=parrot.jar -DrepositoryId=clojars -Durl=https://clojars.org/repo -DpomFile=pom.xml

.PHONY: test deploy clean
