@echo on
set dirname=%0%\..
java -classpath "%dirname:"=%\jackpot.jar" org.netbeans.modules.jackpot30.cmdline.Main %*
