@echo off

REM Remove read-only flag and delete original EXE
attrib -r SoloGJM-*.exe
DEL SoloGJM-*.exe

REM Package
jpackage ^
    --name SoloGJM ^
    --vendor "kieve" ^
    --app-version 1.0 ^
    --module ca.kieve.sologjm/ca.kieve.sologjm.Main ^
    --module-path target/lib;target/classes ^
    --win-dir-chooser ^
    --win-shortcut
