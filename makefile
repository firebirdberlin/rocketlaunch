

release:
	ant release -f RocketLaunchPro.build.xml

debug:
	ant debug -f RocketLaunchPro.build.xml

clean:
	ant clean -f RocketLaunchPro.build.xml

uninstall:
	adb $(OPT) uninstall com.firebirdberlin.rocketlaunch

install:
	adb $(OPT) install -r  RocketLaunch_Pro/RocketLaunch-release.apk

clear-data:
	adb $(OPT) shell pm clear com.firebirdberlin.rocketlaunch
