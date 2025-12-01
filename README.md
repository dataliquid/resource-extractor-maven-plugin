# Resource Extractor Maven Plugin

[![CI Build](https://github.com/dataliquid/resource-extractor-maven-plugin/actions/workflows/ci.yml/badge.svg)](https://github.com/dataliquid/resource-extractor-maven-plugin/actions/workflows/ci.yml)

A Maven plugin for extracting resources from JAR dependencies during the build process.

## Requirements

- Java 17 or higher
- Maven 3.6.0 or higher

## Usage

```xml
<plugin>
    <groupId>com.dataliquid.maven</groupId>
    <artifactId>resource-extractor-maven-plugin</artifactId>
    <version>1.0.0</version>
    <executions>
        <execution>
            <goals>
                <goal>extract</goal>
            </goals>
            <configuration>
                <outputDirectory>${project.build.directory}/extracted</outputDirectory>
                <dependencies>
                    <dependency>
                        <groupId>commons-io</groupId>
                        <artifactId>commons-io</artifactId>
                    </dependency>
                </dependencies>
                <includes>
                    <include>META-INF/*.txt</include>
                </includes>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## Configuration

| Parameter | Description | Default |
|-----------|-------------|---------|
| `outputDirectory` | Target directory for extracted files | required |
| `dependencies` | List of dependencies to extract from | - |
| `includes` | Glob patterns to include | all files |
| `excludes` | Glob patterns to exclude | - |
| `overwrite` | Overwrite existing files | `true` |
| `flattenStructure` | Flatten directory structure | `false` |
| `scope` | Dependency scope (compile, test, etc.) | `compile` |
