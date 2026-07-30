package com.cst438.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

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

        // login as instructor
        driver.findElement(By.id("email")).sendKeys("ted@csumb.edu");
        driver.findElement(By.id("password")).sendKeys("ted2025");
        driver.findElement(By.id("loginButton")).click();
        Thread.sleep(DELAY);

        // enter term 2025 Fall
        driver.findElement(By.id("year")).sendKeys("2025");
        driver.findElement(By.id("semester")).sendKeys("Fall");
        driver.findElement(By.id("selectTermButton")).click();
        Thread.sleep(DELAY);

        // navigate to cst599 assignments
        driver.findElement(By.xpath("//td[text()='cst599']/..//*[@id='assignmentsLink']")).click();
        Thread.sleep(DELAY);

        // open add assignment dialog
        driver.findElement(By.id("addAssignmentButton")).click();
        Thread.sleep(DELAY);

        // enter assignment details
        String title = "Assignment " + Math.abs(random.nextInt());
        String addDialogPath  = "//button[@id='addAssignmentButton']/following-sibling::dialog//";

        driver.findElement(By.xpath(addDialogPath + "input[@id='title']")).sendKeys(title);
        driver.findElement(By.xpath(addDialogPath + "input[@id='dueDate']")).sendKeys("09152025");
        driver.findElement(By.xpath(addDialogPath + "button[@id='saveButton']")).click();
        Thread.sleep(DELAY);

        // verify assignment was created
        assertNotNull(driver.findElement(By.xpath("//*[contains(text(), 'successfully created')]")));

        // close dialog
        driver.findElement(By.xpath(addDialogPath + "button[@id='closeButton']")).click();
        Thread.sleep(DELAY);

        // verify assignment appears in the assignments list
        assertNotNull(driver.findElement(By.xpath("//td[text()='" + title + "']")));

        // logout as instructor
        driver.findElement(By.id("logoutLink")).click();
        Thread.sleep(DELAY);

        // login as student samb
        driver.findElement(By.id("email")).sendKeys("samb@csumb.edu");
        driver.findElement(By.id("password")).sendKeys("samb2025");
        driver.findElement(By.id("loginButton")).click();
        Thread.sleep(DELAY);

        // navigate to view assignments
        driver.findElement(By.id("viewAssignmentsLink")).click();
        Thread.sleep(DELAY);

        // enter term 2025 Fall
        driver.findElement(By.id("year")).sendKeys("2025");
        driver.findElement(By.id("semester")).sendKeys("Fall");
        driver.findElement(By.id("selectTermButton")).click();
        Thread.sleep(DELAY);

        // verify new assignment appears in cst599
        assertNotNull(driver.findElement(By.xpath("//td[text()='" + title + "']")));

        // verify score is blank
        String scoreText = driver.findElement(By.xpath("//td[text()='" + title + "']/following-sibling::td[last()]")).getText();
        assertEquals("", scoreText);

    }
}
