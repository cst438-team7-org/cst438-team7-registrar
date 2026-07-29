package com.cst438.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Random;

public class StudentViewsAssignmentsAndGradesSystemTest {

    static final String CHROME_DRIVER_FILE_LOCATION = "/Users/jian/chromedriver/chromedriver";
    static final String URL = "http://localhost:5173";

    static final int DELAY = 2000;
    WebDriver driver;

    Wait<WebDriver> wait;

    Random random = new Random();

    @BeforeEach
    public void setUpDriver() throws Exception {
        // set properties required by Chrome Driver
        System.setProperty(
                "webdriver.chrome.driver", CHROME_DRIVER_FILE_LOCATION);
        ChromeOptions ops = new ChromeOptions();
        ops.addArguments("--remote-allow-origins=*");

        // start the driver
        driver = new ChromeDriver(ops);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2));
        wait = new WebDriverWait(driver, Duration.ofSeconds(2));
        driver.get(URL);
    }

    @AfterEach
    public void quit() {
        driver.quit();
    }

    @Test
    public void studentViewsAssignmentsAndGrades() throws InterruptedException {

    }
}
