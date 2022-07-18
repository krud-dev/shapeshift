#!/bin/bash

#
# Copyright KRUD 2022
#
# Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
#

set -e

VERSION=$1

# Exit if git tree is not clean
if [ -n "$(git status --porcelain)" ]; then
  echo "Git tree is not clean, aborting release."
  exit 1
fi

# Exit if version is not correct
if ! [[ $VERSION =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "Version is not correct, aborting release."
  exit 1
fi

# Tag release and publish changelog
conventional-changelog -p angular -i CHANGELOG.md -s -r 0
sed -i '' -E "s/<version>.*<\/version>/<version>$VERSION<\/version>/g" README.md
sed -i '' -E "s/dev.krud:shapeshift:([0-9]+\.[0-9]+\.[0-9]+)/dev.krud:shapeshift:$VERSION/g" README.md
git add CHANGELOG.md
git add README.md
git commit -m "chore(release): release v$VERSION"
./gradlew -Prelease -Pshapeshift.version=$VERSION clean jar publishAllPublicationsToOSSRHRepository
git tag -a v$VERSION -m "Release v$VERSION"
#git push origin v$VERSION