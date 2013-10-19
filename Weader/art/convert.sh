#! /bin/sh
set -x
cd `dirname $0`
while read dir size; do
    inkscape --export-png=$dir/ic_launcher.png \
	       --export-width=$size --export-height=$size \
	       ic_launcher.svg
done <<EOF
../res/drawable-ldpi 36
../res/drawable-mdpi 48
../res/drawable-hdpi 72
../res/drawable-xhdpi 96
EOF