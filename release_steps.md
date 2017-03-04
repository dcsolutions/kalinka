### Regular build
* without javadoc, sources and gpg-sign
`mvn clean install`

### Release

#### Snapshot
* including javadoc, sources and gpg-sign
`mvn clean deploy -DperformRelease=true`

#### 'Official Release (no Snapshot)
We don't use *maven-release-plugin*

* Alter Version to Release-version


