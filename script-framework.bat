@echo off

REM Déclaration des variables
    set nom_projet=framework-java
    set temp="C:\Users\ismael\Documents\GitHub\"%nom_projet%
    set lib="C:\Users\ismael\Documents\GitHub\test\lib"
    set lib-framework="C:\Users\ismael\Documents\GitHub\framework-java\lib"
    set src="C:\Users\ismael\Documents\GitHub\framework-java\src"

REM Compilation des fichiers Java dans des packages
    for /r %src% %%i in (*.java) do (
        javac -cp "%lib-framework%\*;" -sourcepath %src% -d %temp% "%%i"
    )

REM Copie des *.jar de notre espace de travail initial ver temp/WEB-INF/lib
    xcopy /s /e /q %lib-framework% %temp%"\lib"

REM Convertir le répertoire temp en .jar
    jar -cvf %nom_projet%.jar -C %temp% .

REM supprimer le reperitoire 
    rmdir %temp%

REM Copie du fichier jar vers lib
    copy /y %nom_projet%.jar %lib%

REM Supprimer le fichier jar temporaire
    del %nom_projet%.jar



