[![release](http://github-release-version.herokuapp.com/github/tapack/allure-cucumber-jvm-adaptor/release.svg?style=flat)](https://github.com/tapack/allure-cucumber-jvm-adaptor/releases/latest) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.tapack/allure-cucumber-jvm-adaptor/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/io.tapack/allure-cucumber-jvm-adaptor)


# Allure Cucumber-JVM Adaptor
This adaptor allows to generate allure xml reports after cucumber-jvm Junit test execution. (Scenario -> Test)

## Difference from [original adaptor](https://github.com/allure-framework/allure-cucumber-jvm-adaptor)

Is other mapping:
  - Feature -> Allure Test Suite
  - Scenario -> Allure Test Case
  - Step -> Allure Step
  - Scenario Outline -> Allure Test Cases

## Usage
Simply add **allure-allure-cucumber-jvm-adaptor** as dependency to your project and add **build** section with adaptor listener:
```xml
<project>
...
    <dependencies>
        <dependency>
            <groupId>io.tapack</groupId>
            <artifactId>allure-cucumber-jvm-adaptor</artifactId>
            <version>0.2</version>
        </dependency>
    </dependencies>
        <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>2.18.1</version>
                <configuration>
                    <testFailureIgnore>false</testFailureIgnore>
                    <argLine>
                        -javaagent:${settings.localRepository}/org/aspectj/aspectjweaver/${aspectj.version}/aspectjweaver-${aspectj.version}.jar
                    </argLine>
                    <properties>
                        <property>
                            <name>listener</name>
                            <value>io.tapack.allure.cucumberjvm.AllureCucumberListener</value>
                        </property>
                    </properties>
                </configuration>
                <dependencies>
                    <dependency>
                        <groupId>org.aspectj</groupId>
                        <artifactId>aspectjweaver</artifactId>
                        <version>1.7.4</version>
                    </dependency>
                </dependencies>
            </plugin>
        </plugins>
    </build>
</project>
```

Then execute **mvn clean test** goal.
After tests executed allure xml files will be placed in **target/allure-results/** directory