#!/bin/fish

echo "Run from Forkyz root directory!"

if test (count $argv) -lt 1
    echo "Please pass the version name as the first argument. E.g. release.sh 40."
    exit
end

set forkyz_version $argv[1]

set changelog fastlane/metadata/android/en-US/changelogs/{$forkyz_version}00000.txt

nvim $changelog
git add $changelog
python scripts/make_release_notes.py
nvim app/src/main/AndroidManifest.xml

echo "Ready to commit!"
echo "Don't forget to git add the new changelog."

git status
