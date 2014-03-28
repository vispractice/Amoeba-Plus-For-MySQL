@echo on
java -Xms256m -Xmx512m -XX:PermSize=128M -XX:MaxNewSize=256m -XX:MaxPermSize=256m -jar module/libs/org.eclipse.osgi_3.7.1.R37x_v20110808-1106.jar -configuration conf -console
