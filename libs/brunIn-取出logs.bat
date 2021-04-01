adb wait-for-device
adb root
adb shell getprop ro.boot.serialno > serno.txt
set /p SERNO=<serno.txt
mkdir %SERNO%
del serno.txt

echo "Pull logd.."
adb pull data/misc/logd

echo "Pull BurnInTestLog.."
adb pull storage/emulated/0/DCIM/BurnInTestLog.ini logd
echo "Pull Config.."
adb pull storage/emulated/0/DCIM/BurnInTestConfig.ini logd

mv logd %SERNO%
echo "Clear logd.."
adb shell setprop logd.logpersistd clear
echo "Clear BurnInTestLog.."
adb shell rm storage/emulated/0/DCIM/BurnInTestLog.ini
pause