setup:
	gradle wrapper --gradle-version 7.4

run:
	APP_ENV=development ./build/install/app/bin/app

start:
	APP_ENV=development ./gradlew run

start-prod:
	APP_ENV=production ./gradlew run

clean:
	./gradlew clean

build:
	./gradlew clean build

install:
	./gradlew install

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