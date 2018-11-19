# fc-graphql
[![Build Status](https://travis-ci.org/xuybin/fc-graphql.svg?branch=master)](https://travis-ci.org/xuybin/fc-graphql)
[![Download](https://api.bintray.com/packages/xuybin/maven/fc-graphql/images/download.svg) ](https://bintray.com/xuybin/maven/fc-graphql/_latestVersion)

Serverless Cloud Function && Microservice,based on [fc](https://github.com/aliyun/fc-java-libs) [graphql](https://github.com/graphql-java/graphql-java) [http4k](https://github.com/http4k/http4k)

[example](https://github.com/xuybin/fc-graphql-example)

## Using in your project
### Maven

Add Bintray JCenter repository to <repositories> section:

```
<repository>
    <id>jcenter</id>
    <url>https://jcenter.bintray.com</url>
</repository>
```

Add dependency:

```
<dependency>
  <groupId>com.github.xuybin</groupId>
  <artifactId>fc-graphql</artifactId>
  <version>${version}</version>
</dependency>
```

And make sure that you use the right Kotlin version:

```
<properties>
    <kotlin.version>1.3.10</kotlin.version>
</properties>
```

### Gradle

Add Bintray JCenter repository:

```
repositories {
    jcenter()
}
```
Add dependencies (you can also add other modules that you need):

```
compile 'com.github.xuybin:fc-graphql:${version}'
```

And make sure that you use the right Kotlin version:

```
buildscript {
    ext.kotlin_version = '1.3.10'
}
```
