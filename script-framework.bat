@echo off

REM Déclaration des variables
    set nom_projet=framework
    set temp=C:\Users\ismab\OneDrive\Documents\GitHub\test\%nom_projet%
    set lib=C:\Users\ismab\OneDrive\Documents\GitHub\test\lib
    set lib-framework=C:\Users\ismab\OneDrive\Documents\GitHub\test\lib
    set src=C:\Users\ismab\OneDrive\Documents\GitHub\main\src

REM Compilation des fichiers Java dans des packages
    for /r %src% %%i in (*.java) do (
        javac -cp "%lib-framework%\*;" -sourcepath %src% -d %temp% "%%i"
    )

REM Convertir le répertoire temp en .jar
    jar -cvf %nom_projet%.jar -C %temp% .

REM supprimer le reperitoire 
    rmdir %temp%

REM Copie du fichier jar vers lib
    copy /y %nom_projet%.jar %lib%

REM Supprimer le fichier jar temporaire
    del %nom_projet%.jar



