#!/bin/python

# Convencience script for generating release.html and changes.md from fastlane
# changelogs.
#
# To be run from project root.

import glob
import os

release_file="app/src/main/assets/release.html"
changes_md="docs/changes.md"

def version_number(filename: str) -> int:
    start = filename.rfind(os.path.sep) + 1
    end = -len("00000.txt")
    return int(filename[start:end])

with open(release_file, "w") as release_file:
    release_file.write("""
        <html>
        <style>
        * { font-family: Roboto, sans-serif; color:black; }
        body { background: #eeeeee; height: 100%; }
        ul { padding-left: 2em; }
        </style>
        <body>
            <h1>Forkyz</h1>

            <p>
                This is an unofficial fork of the <a
                href="https://github.com/kebernet/shortyz">Shortyz</a>
                crossword app. It implements a number of new features and
                removes some non-open libraries / trackers.
            </p>
    """)

    changelogs = glob.glob("fastlane/metadata/android/en-US/changelogs/*")
    changelogs.sort(key=lambda file: -version_number(file))

    for file in changelogs:
        release_file.write(f"<h2>Version {version_number(file)}</h2>\n")
        release_file.write("<ul>\n")
        with open(file) as changelog:
            for line in changelog:
                if line.startswith("-"):
                    line = line[1:]
                line = line.strip()
                if len(line) > 0:
                    release_file.write(f"<li>{line}</li>\n")
        release_file.write("</ul>\n")

    release_file.write("""
        </body>
        </html>
    \n""")

with open(changes_md, "w") as changes_md:
    changes_md.write("# Forkyz Changelog\n\n")

    changelogs = glob.glob("fastlane/metadata/android/en-US/changelogs/*")
    changelogs.sort(key=lambda file: -version_number(file))

    for file in changelogs:
        changes_md.write(f"## Version {version_number(file)}\n\n")
        with open(file) as changelog:
            for line in changelog:
                changes_md.write(line)
        changes_md.write("\n")
