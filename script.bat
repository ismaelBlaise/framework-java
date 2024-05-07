@echo off

REM Déclaration des variables
    set nom_projet=Sprint0-2391
    set temp=C:\Users\ismab\OneDrive\Documents\GitHub\test\%nom_projet%
    set lib=C:\Users\ismab\OneDrive\Documents\GitHub\test\lib
    set src=C:\Users\ismab\OneDrive\Documents\GitHub\main\src

REM Compilation des fichiers Java dans des packages
    for /r %src% %%i in (*.java) do (
        javac -cp "%lib%\*;" -sourcepath %src% -d %temp% "%%i"
    )

REM Convertir le répertoire temp en .war
    jar -cvf %nom_projet%.war -C %temp% .

REM Copie du fichier war vers tomcat/webapps
    copy /y %nom_projet%.war C:\Apache_tomcat\webapps

REM Supprimer le fichier WAR temporaire
    del %nom_projet%.war



