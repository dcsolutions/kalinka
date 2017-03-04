### Release-process

Steps should be executed by build-server (jenkins)

#### Snapshot

* including javadoc, sources and gpg-sign

`mvn clean deploy -DperformRelease=true`

#### 'Official' release (no snapshot)

Project does not use *maven-release-plugin*

* including javadoc, sources and gpg-sign

##### Alter version to release-version

`mvn validate -DprepareRelease=true`

##### perform release

`mvn clean deploy -DperformRelease=true`

##### Alter version to next deployment-version

`mvn validate -DprepareDev=true`
