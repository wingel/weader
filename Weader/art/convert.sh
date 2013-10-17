#! /bin/sh
set -x
cd `dirname $0`
while read dir size; do
    inkscape --export-png=$dir/logo.png \
	       --export-width=$size --export-height=$size \
	       logo.svg
done <<EOF
../res/drawable 48
../res/drawable-ldpi 36
../res/drawable-mdpi 48
../res/drawable-hdpi 72
../res/drawable-xhdpi 96
EOF