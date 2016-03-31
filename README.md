[![release](http://github-release-version.herokuapp.com/github/tapack/allure-cucumber-jvm-adaptor/release.svg?style=flat)](https://github.com/tapack/allure-cucumber-jvm-adaptor/releases/latest) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.tapack/allure-cucumber-jvm-adaptor/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/io.tapack/allure-cucumber-jvm-adaptor)


# Allure Cucumber-JVM Adaptor
This adaptor allows to generate allure xml reports after cucumber-jvm Junit test execution. (Scenario -> Test)

## Difference from [original adaptor](https://github.com/allure-framework/allure-cucumber-jvm-adaptor)

Is other mapping:
  - Feature -> Allure Test Suite
  - Scenario -> Allure Test Case
  - Step -> Allure Step
  - Scenario Outline -> Allure Test Cases