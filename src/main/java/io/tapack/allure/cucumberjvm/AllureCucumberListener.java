package io.tapack.allure.cucumberjvm;

import gherkin.formatter.model.Feature;
import gherkin.formatter.model.Scenario;
import gherkin.formatter.model.ScenarioOutline;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Ignore;
import org.junit.internal.AssumptionViolatedException;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import ru.yandex.qatools.allure.Allure;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.config.AllureModelUtils;
import ru.yandex.qatools.allure.events.*;
import ru.yandex.qatools.allure.utils.AnnotationManager;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JUnit listener that map Cucumber Scenario to Allure test.
 */
public class AllureCucumberListener extends RunListener {

    private Allure lifecycle = Allure.LIFECYCLE;

    private final Map<String, String> suites = new ConcurrentHashMap<>();

    /**
     * All tests object
     */
    private Description parentDescription;

    @Override
    public void testRunStarted(Description description) {
        parentDescription = description;
    }

    /**
     * <p>
     * Find features level<p>
     * JUnit`s test {@link Description} is multilevel object with liquid
     * hierarchy.<br>
     * This method recursively query
     * {@link #getTestEntityType(Description)} method until it
     * matches {@link Feature} type and when returns list of {@link Feature}
     * descriptions
     *
     * @param description {@link Description} Description to start search where
     * @return {@link List<Description>} features description list
     * @throws IllegalAccessException
     */
    private List<Description> findFeaturesLevel(List<Description> description)
            throws IllegalAccessException {
        if (description.isEmpty()) {
            return new ArrayList<>();
        }
        Object entityType = getTestEntityType(description.get(0));
        if (entityType instanceof Feature) {
            return description;
        } else {
            return findFeaturesLevel(description.get(0).getChildren());
        }

    }

    /**
     * Get Description unique object
     *
     * @param description See {@link Description}
     * @return {@link Object} what represents by uniqueId on {@link Description}
     * creation as an arbitrary object used to define its type.<br>
     * It can be instance of {@link String}, {@link Feature}, {@link Scenario}
     * or {@link ScenarioOutline}.<br>
     * In case of {@link String} object it could be Suite, TestClass or an
     * empty, regardless to level of {@link #parentDescription}
     * @throws IllegalAccessException
     */
    private Object getTestEntityType(Description description) throws IllegalAccessException {
        return FieldUtils.readField(description, "fUniqueId", true);
    }

    /**
     * <p>
     * Find Test classes level<p>
     * JUnit`s test {@link Description} is multilevel object with liquid
     * hierarchy.<br>
     * This method recursively query
     * {@link #getTestEntityType(Description)} method until it
     * matches {@link Feature} type and when returns parent of this object as
     * list of test classes descriptions
     *
     * @param description {@link Description} Description to start search where
     * @return {@link List<Description>} test classes description list
     * @throws IllegalAccessException
     */
    private List<Description> findTestClassesLevel(List<Description> description) throws IllegalAccessException {
        if (description.isEmpty()) {
            return new ArrayList<>();
        }
        Object possibleClass = getTestEntityType(description.get(0));
        if (possibleClass instanceof String && !((String) possibleClass).isEmpty()) {
            if (!description.get(0).getChildren().isEmpty()) {
                Object possibleFeature = getTestEntityType(description.get(0).getChildren().get(0));
                if (possibleFeature instanceof Feature) {
                    return description;
                } else {
                    return findTestClassesLevel(description.get(0).getChildren());
                }
            } else {
                //No scenarios in feature
                return description;
            }

        } else {
            return findTestClassesLevel(description.get(0).getChildren());
        }

    }

    /**
     * Find feature and story for given scenario
     *
     * @param scenarioName
     * @return {@link String[]} of ["<FEATURE_NAME>", "<STORY_NAME>"]s
     * @throws IllegalAccessException
     */
    private String[] findFeatureByScenarioName(String scenarioName) throws IllegalAccessException {
        List<Description> testClasses = findTestClassesLevel(parentDescription.getChildren());

        for (Description testClass : testClasses) {

            List<Description> features = findFeaturesLevel(testClass.getChildren());
            //Feature cycle
            for (Description feature : features) {
                //Story cycle
                for (Description story : feature.getChildren()) {
                    Object scenarioType = getTestEntityType(story);

                    //Scenario
                    if (scenarioType instanceof Scenario
                            && story.getDisplayName().equals(scenarioName)) {
                        return new String[]{feature.getDisplayName(), scenarioName};

                        //Scenario Outline
                    } else if (scenarioType instanceof ScenarioOutline) {
                        List<Description> examples = story.getChildren().get(0).getChildren();
                        // we need to go deeper :)
                        for (Description example : examples) {
                            if (example.getDisplayName().equals(scenarioName)) {
                                return new String[]{feature.getDisplayName(), story.getDisplayName()};
                            }
                        }
                    }
                }
            }
        }
        return new String[]{"Feature: Undefined Feature", scenarioName};
    }


    private String findFeatureByScenario(Description scenario) throws IllegalAccessException {
        String scenarioToFindName = scenario.getClassName();
        String scenarioToFindId = scenarioToFindName;
        Object scenarioToFindType = getTestEntityType(scenario);
        if (scenarioToFindType instanceof Scenario) {
            scenarioToFindId = ((Scenario) scenarioToFindType).getId();
        }

        List<Description> testClasses = findTestClassesLevel(parentDescription.getChildren());

        for (Description testClass : testClasses) {

            List<Description> features = findFeaturesLevel(testClass.getChildren());
            //Feature cycle
            for (Description feature : features) {
                //Story cycle
                for (Description story : feature.getChildren()) {
                    Object scenarioType = getTestEntityType(story);

                    //Scenario
                    if (scenarioType instanceof Scenario
                            && story.getDisplayName().equals(scenarioToFindName)
                            && ((Scenario) scenarioType).getId().equals(scenarioToFindId)) {
                        return feature.getDisplayName();

                        //Scenario Outline
                    } else if (scenarioType instanceof ScenarioOutline) {
                        List<Description> examples = story.getChildren().get(0).getChildren();
                        // we need to go deeper :)
                        for (Description example : examples) {
                            if (example.getDisplayName().equals(scenarioToFindName)) {
                                Object exampleType = getTestEntityType(example);
                                if (exampleType instanceof Scenario && ((Scenario) exampleType).getId().equals(scenarioToFindId)) {
                                    return feature.getDisplayName();
                                }
                            }
                        }
                    }
                }
            }
        }
        return "Feature: Undefined Feature";
    }

    private void testSuiteStarted(Description description, String suiteName, String scenarioName) throws IllegalAccessException {

        String[] annotationParams = findFeatureByScenarioName(scenarioName);

        //Create feature and story annotations. Remove unnecessary words from it
        Features feature = getFeaturesAnnotation(new String[]{annotationParams[0].split(":")[1].trim()});

        String uid = generateSuiteUid(suiteName);
        TestSuiteStartedEvent event = new TestSuiteStartedEvent(uid, feature.value()[0]);

        event.setTitle(feature.value()[0]);

        //Add feature and story annotations
        Collection<Annotation> annotations = new ArrayList<>();
        for (Annotation annotation : description.getAnnotations()) {
            annotations.add(annotation);
        }
        annotations.add(feature);
        AnnotationManager am = new AnnotationManager(annotations);
        am.update(event);

        event.withLabels(AllureModelUtils.createTestFrameworkLabel("CucumberJVM"));

        getLifecycle().fire(event);
    }

    @Override
    public void testStarted(Description description) throws IllegalAccessException {

        if (description.isSuite()) {
            String methodName = description.getClassName();
            //If it`s Scenario Outline, add example string to story name
            if (methodName.startsWith("|")
                    || description.getDisplayName().endsWith("|")) {
                methodName = getScenarioOutlineName(description) + methodName;
            }
            methodName = methodName.replaceFirst("^(.*): ", "");
            TestCaseStartedEvent event = new TestCaseStartedEvent(getSuiteUid(description), methodName);
            event.setTitle(methodName);

            Stories story = getStoriesAnnotation(new String[]{methodName});

            Collection<Annotation> annotations = new ArrayList<>();
            for (Annotation annotation : description.getAnnotations()) {
                annotations.add(annotation);
            }

            annotations.add(story);

            AnnotationManager am = new AnnotationManager(annotations);
            am.update(event);
            getLifecycle().fire(event);
        } else {
            String stepName = extractMethodName(description);
            getLifecycle().fire(new StepStartedEvent(stepName).withTitle(stepName));
        }
    }

    @Override
    public void testFailure(Failure failure) {
        Throwable throwable = failure.getException();
        if (failure.getDescription().isTest()) {
            getLifecycle().fire(new StepFailureEvent().withThrowable(throwable));
        } else {
            // Produces additional failure step for all test case
            if (throwable instanceof AssumptionViolatedException) {
                getLifecycle().fire(new TestCaseCanceledEvent().withThrowable(throwable));
            } else {
                getLifecycle().fire(new TestCaseFailureEvent().withThrowable(throwable));
            }
        }
    }

    @Override
    public void testAssumptionFailure(Failure failure) {
        testFailure(failure);
    }

    @Override
    public void testIgnored(Description description) throws IllegalAccessException {
        if (description.isTest()) {
            String stepName = extractMethodName(description);
            getLifecycle().fire(new StepStartedEvent(stepName).withTitle(stepName));
            getLifecycle().fire(new StepCanceledEvent());
            getLifecycle().fire(new StepFinishedEvent());
        } else {
            getLifecycle().fire(new TestCasePendingEvent().withMessage(getIgnoredMessage(description)));
        }
    }

    @Override
    public void testFinished(Description description) throws IllegalAccessException {
        if (description.isTest()) {
            getLifecycle().fire(new StepFinishedEvent());
        } else {
            getLifecycle().fire(new TestCaseFinishedEvent());
            if (isLastScenario(description)) {
                getLifecycle().fire(new TestSuiteFinishedEvent(getSuiteUid(description)));
            }
        }
    }

    private String getScenarioOutlineName(Description description) throws IllegalAccessException {
        Object testEntityType = getTestEntityType(description);
        if (testEntityType instanceof Scenario) {
            return ((Scenario) testEntityType).getName();
        }
        return "Undefined Scenario Outline";
    }

    private boolean isLastScenario(Description description) throws IllegalAccessException {

        String scenarioToFindId = description.getClassName();
        Object scenarioToFindType = getTestEntityType(description);
        if (scenarioToFindType instanceof Scenario) {
            scenarioToFindId = ((Scenario) scenarioToFindType).getId();
        }

        List<String> lastScenariosIds = new ArrayList<>();
        List<Description> testClasses = findTestClassesLevel(parentDescription.getChildren());
        for (Description testClass : testClasses) {
            List<Description> features = findFeaturesLevel(testClass.getChildren());
            for (Description feature : features) {
                Description lastScenarioDescription = feature.getChildren().get(feature.getChildren().size() - 1);
                Object scenarioType = getTestEntityType(lastScenarioDescription);
                if (scenarioType instanceof Scenario) {
                    lastScenariosIds.add(((Scenario) scenarioType).getId());
                } else if (scenarioType instanceof ScenarioOutline) {
                    ArrayList<Description> examples = lastScenarioDescription.getChildren().get(0).getChildren();
                    Description lastExample = examples.get(examples.size() - 1);
                    Object exampleType = getTestEntityType(lastExample);
                    if (exampleType instanceof Scenario) {
                        lastScenariosIds.add(((Scenario) exampleType).getId());
                    }
                }
            }
        }
        return lastScenariosIds.contains(scenarioToFindId);
    }

    private String generateSuiteUid(String suiteName) {
        String uid = UUID.randomUUID().toString();
        synchronized (getSuites()) {
            getSuites().put(suiteName, uid);
        }
        return uid;
    }

    private String getSuiteUid(Description description) throws IllegalAccessException {
        String scenarioName = description.getClassName();
        String suiteName = findFeatureByScenario(description);
        if (!description.isSuite()) {
            suiteName = extractClassName(description);
        }
        if (!getSuites().containsKey(suiteName)) {
            //Fix NPE
            Description suiteDescription = Description.createSuiteDescription(suiteName);
            testSuiteStarted(suiteDescription, suiteName, scenarioName);
        }
        return getSuites().get(suiteName);
    }

    private String getIgnoredMessage(Description description) {
        Ignore ignore = description.getAnnotation(Ignore.class);
        return ignore == null || ignore.value().isEmpty() ? "Step is not implemented yet!" : ignore.value();
    }

    private Allure getLifecycle() {
        return lifecycle;
    }

    private Map<String, String> getSuites() {
        return suites;
    }

    private String extractClassName(Description description) {
        String displayName = description.getDisplayName();
        Pattern pattern = Pattern.compile("\\(\\|(.*)\\|\\)");
        Matcher matcher = pattern.matcher(displayName);
        if (matcher.find()) {
            return "|" + matcher.group(1) + "|";
        }
        return description.getClassName();
    }

    private String extractMethodName(Description description) {
        String displayName = description.getDisplayName();
        Pattern pattern = Pattern.compile("^(.*)\\(\\|");
        Matcher matcher = pattern.matcher(displayName);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return description.getMethodName();
    }

    /**
     * Creates Story annotation object
     *
     * @param value story names array
     * @return Story annotation object
     */
    private Stories getStoriesAnnotation(final String[] value) {
        return new Stories() {

            @Override
            public String[] value() {
                return value;
            }

            @Override
            public Class<Stories> annotationType() {
                return Stories.class;
            }
        };
    }

    /**
     * Creates Feature annotation object
     *
     * @param value feature names array
     * @return Feature annotation object
     */
    private Features getFeaturesAnnotation(final String[] value) {
        return new Features() {

            @Override
            public String[] value() {
                return value;
            }

            @Override
            public Class<Features> annotationType() {
                return Features.class;
            }
        };
    }
}

