


release:
	ant release -f RocketLaunchPro.build.xml 

debug:
	ant debug -f RocketLaunchPro.build.xml

releaselite:
	./genlitesrc.sh
	ant release -f RocketLaunchLite.build.xml

debuglite:
	./genlitesrc.sh
	ant debug -f RocketLaunchLite.build.xml

clean:
	ant clean -f RocketLaunchPro.build.xml

cleanlite:
	ant clean -f RocketLaunchLite.build.xml
	rm -rf src_RocketLaunchLite

uninstall:
	adb -d uninstall com.firebirdberlin.rocketlaunch

install:
	adb -d install -r  RocketLaunch_Pro/RocketLaunch-release.apk


uninstalllite:
	adb -d uninstall com.firebirdberlin.rocketlaunchlite
