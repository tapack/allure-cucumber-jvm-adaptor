package io.tapack.allure.cucumberjvm;

import cucumber.api.PendingException;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

import static org.junit.Assert.assertEquals;

public class CalculateSteps {

    private int firstDigit;
    private int secondDigit;
    private int sum;

    @Given("^the first number (\\d+)$")
    public void theFirstNumber(int digit) {
        firstDigit = digit;
    }

    @And("^the second number (\\d+)$")
    public void theSecondNumber(int digit) {
        secondDigit = digit;
    }

    @When("^I add them together$")
    public void iAddThemTogether() {
        sum = firstDigit + secondDigit;
    }

    @Then("^the sum is equal to (\\d+)$")
    public void theSumIsEqualTo(int result) {
        assertEquals(result, sum);
    }

    @Given("^the wrong number (\\d+)$")
    public void theWrongNumber(int digit) throws Throwable {
        Object o = digit;
        String fail = (String) o;
    }
}
