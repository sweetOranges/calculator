@echo OFF
if not exist ./Calculator.class javac ./Calculator.java
:A
set /p var=请输入计算表达式:
java Calculator %var%


set/p input= 是否继续(y/n):
if "%input%"=="y" goto A
if "%input%"=="n" goto B

:B
pause
exit
