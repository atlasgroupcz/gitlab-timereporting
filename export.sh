#!/bin/bash

# what to export
declare -a StringArray=("users" "issues" "labels" "timelogs" "label_links" "boards" "projects" "merge_requests" "namespaces" )
# temp dir
tmp_dir=$(mktemp -d -t gitlab-export-XXXXXXXXXX)
chmod a+rwX $tmp_dir

# Iterate the string array using for loop
for val in "${StringArray[@]}"; do
   echo "Exporting $val to $tmp_dir/$val.csv"
   sudo gitlab-psql -c "copy $val TO '$tmp_dir/$val.csv' with (format csv, header)"
done

rm export.zip
zip -r -j export.zip $tmp_dir

rm -rf $tmp_dir
