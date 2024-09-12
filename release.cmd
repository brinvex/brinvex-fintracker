set JAVA_HOME="C:\tools\java\jdk-22.0.1"

set new_version=0.0.22

set jsh_content=^
    Files.writeString(Path.of("README.md"), ^
        Files.readString(Path.of("README.md")).replaceAll(^
            "<brinvex-fintracker.version>(.*)</brinvex-fintracker.version>", ^
            "<brinvex-fintracker.version>%%s</brinvex-fintracker.version>".formatted(System.getenv("new_version"))), ^
    StandardOpenOption.TRUNCATE_EXISTING);

echo %jsh_content% | %JAVA_HOME%\bin\jshell -

call mvnw clean package
call mvnw versions:set -DnewVersion=%new_version%
call mvnw versions:commit
call mvnw clean deploy -T 1 -DskipTests

REM Commit and push