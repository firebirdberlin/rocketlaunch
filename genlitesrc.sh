#! /bin/bash

sub=com/firebirdberlin/rocketlaunch/
tgt=src_RocketLaunchLite/${sub}

mkdir -p ${tgt}
for i in src/${sub}/*.java; do
	base=`basename $i`;
	sed 's/com\.firebirdberlin\.rocketlaunch/com.firebirdberlin.rocketlaunchlite/g' ${i} > ${tgt}/${base};
done;

#sed -e 's/com\.firebirdberlin\.rocketlaunch/com.firebirdberlin.rocketlaunchlite/g' \
#-e 's/android:label="RocketLaunch"/android:label="RocketLaunchLite"/g' template/AndroidManifest.xml > AndroidManifest.xml;
