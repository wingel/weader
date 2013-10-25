#! /bin/sh
set -e
set -x
cd `dirname $0`
fns=
fns="$fns ic_launcher"
while read dir dpi; do
    mkdir -p $dir
    for fn in $fns; do
	inkscape \
	    --export-area-page --export-dpi=$dpi \
	    --export-background='#000000' --export-background-opacity=0 \
	    --export-png=tmp.png $fn.svg
	convert tmp.png -strip $dir/$fn.png
    done
done <<EOF
../res/drawable-ldpi 45
../res/drawable-mdpi 60
../res/drawable-hdpi 90
../res/drawable-xhdpi 120
EOF
fns=
fns="$fns ic_menu_mark_clear"
fns="$fns ic_menu_mark_set"
fns="$fns ic_menu_mark_all_clear"
fns="$fns ic_menu_mark_all_set"
while read dir dpi; do
    mkdir -p $dir
    for fn in $fns; do
	inkscape \
	    --export-area-page --export-dpi=$dpi \
	    --export-background='#000000' --export-background-opacity=0 \
	    --export-png=tmp.png $fn.svg
	convert tmp.png -strip -channel RGBA -colorspace gray $dir/$fn.png
    done
done <<EOF
../res/drawable-ldpi 45
../res/drawable-mdpi 60
../res/drawable-hdpi 90
../res/drawable-xhdpi 120
EOF
rm -f tmp.png
