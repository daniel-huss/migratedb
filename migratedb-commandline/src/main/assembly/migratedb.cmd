@REM
@REM Copyright (C) Red Gate Software Ltd 2010-2021
@REM Copyright 2022-2024 The MigrateDB contributors
@REM
@REM Licensed under the Apache License, Version 2.0 (the "License");
@REM you may not use this file except in compliance with the License.
@REM You may obtain a copy of the License at
@REM
@REM         http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing, software
@REM distributed under the License is distributed on an "AS IS" BASIS,
@REM WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@REM See the License for the specific language governing permissions and
@REM limitations under the License.
@REM

@Echo off

setlocal

set INSTALLDIR=%~dp0

if exist "%INSTALLDIR%\jre\bin\java.exe" (
 set JAVA_CMD="%INSTALLDIR%\jre\bin\java.exe"
) else (
 @REM Use JAVA_HOME if it is set
 if "%JAVA_HOME%"=="" (
  set JAVA_CMD=java
 ) else (
  set JAVA_CMD="%JAVA_HOME%\bin\java.exe"
 )
)

if "%JAVA_ARGS%"=="" (
  set JAVA_ARGS=
)

%JAVA_CMD% -Djava.library.path="%INSTALLDIR%\native" %JAVA_ARGS% -cp "%CLASSPATH%;%INSTALLDIR%\lib\*;%INSTALLDIR%\drivers\*" migratedb.v1.commandline.Main "-baseDirectory=%INSTALLDIR%" %*

@REM Exit using the same code returned from Java
EXIT /B %ERRORLEVEL%
