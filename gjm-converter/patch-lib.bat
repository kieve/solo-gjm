@echo off

call :patch slf4j.api slf4j-api-1.7.10.jar

pause
exit /b

:patch
jdeps --ignore-missing-deps --generate-module-info target/mods target/lib/%~2
javac --patch-module %~1=target/lib/%~2 target/mods/%~1/module-info.java
jar uf target/lib/%~2 -C target/mods/%~1 module-info.class
goto:eof
