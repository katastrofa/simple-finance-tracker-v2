# simple-finance-tracker-v2

`fastOptJS::webpack` is a command used in Scala.js projects to compile Scala code into JavaScript. It is part of the SBT (Scala Build Tool) ecosystem and is commonly used in web development with Scala.

## How to run

**Start MySQL (first time pulls image + runs migrations)**

`podman-compose up -d`

**Run backend**

`sbt -Dconfig.file=backend/src/main/resources/dev.conf "backend/run"`

Or in IntelliJ: add VM option `-Dconfig.file=backend/src/main/resources/dev.conf`
