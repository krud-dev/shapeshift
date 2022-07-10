#!/bin/bash

set -e

VERSION=$1
SNAPSHOT_VERSION=$2

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

# Exit if snapshot version is not correct
if ! [[ $SNAPSHOT_VERSION =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "Snapshot Version is not correct, aborting release."
  exit 1
fi

# Set version on pom

mvn versions:set -DnewVersion=$VERSION
git add -A
git commit -m "chore(release): v$VERSION"
git tag -a v$VERSION -m "Release v$VERSION"
conventional-changelog -p angular -i CHANGELOG.md -s -r 0
git add CHANGELOG.md
git commit -m "chore(release): add changelog for v$VERSION"
mvn -Possrh clean package
mvn -Possrh deploy

# Set next snapshot

mvn versions:set -DnewVersion=${SNAPSHOT_VERSION}-SNAPSHOT
git add -A
git commit -m "chore(release): set new snapshot version $SNAPSHOT_VERSION"

#git push origin v$VERSION