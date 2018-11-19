# fc-graphql
[![Build Status](https://travis-ci.org/xuybin/fc-graphql.svg?branch=master)](https://travis-ci.org/xuybin/fc-graphql)
[![Download](https://api.bintray.com/packages/xuybin/maven/fc-graphql/images/download.svg) ](https://bintray.com/xuybin/maven/fc-graphql/_latestVersion)

同时兼容Microservice和Serverless架构,基于fc+graphql-spring封装的基础类库

example: https://github.com/xuybin/fc-graphql-example

## Serverless

Aliyun Function Comput(支持)

Tencent Cloud(计划支持)

## Microservice
### Docker Swarm环境下
``` bash
```
### kubernetes环境下
``` bash
```

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
