setup:
	gradle wrapper --gradle-version 7.4

clean:
	./gradlew clean

build:
	./gradlew clean build

start:
	APP_ENV=development ./gradlew run

run:
	APP_ENV=development ./build/install/app/bin/app

install:
	./gradlew install

start-dist:
	APP_ENV=production ./build/install/app/bin/app

generate-migrations:
	./gradlew generateMigrations

lint:
	./gradlew checkstyleMain checkstyleTest

test:
	./gradlew test

report:
	./gradlew jacocoTestReport

image-build:
	docker build -t Linkshegelianer/java-project-72:latest .

image-push:
	docker push Linkshegelianer/java-project-72:latest

.PHONY: build