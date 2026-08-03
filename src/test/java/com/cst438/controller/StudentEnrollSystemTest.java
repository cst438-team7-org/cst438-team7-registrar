package com.cst438.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

public class StudentEnrollSystemTest {

    static final String URL = "http://localhost:5173";
    static final int DELAY = 2000;

    WebDriver driver;
    Wait<WebDriver> wait;

    @BeforeEach
    public void setUpDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--lang=en-US");

        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(3));
        wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        driver.get(URL);
    }

    @AfterEach
    public void quit() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    public void studentEnrollsIntoSection() throws InterruptedException {

        // Log in as student Sam.
        driver.findElement(By.id("email")).sendKeys("sam@csumb.edu");
        driver.findElement(By.id("password")).sendKeys("sam2025");
        driver.findElement(By.id("loginButton")).click();

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.id("scheduleLink")
                )
        );

        // View Sam's Fall 2025 class schedule.
        driver.findElement(By.id("scheduleLink")).click();

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.id("year")
                )
        );

        driver.findElement(By.id("year")).sendKeys("2025");
        driver.findElement(By.id("semester")).sendKeys("Fall");
        driver.findElement(By.id("selectTermButton")).click();

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//td[text()='cst599']")
                )
        );

        // Drop CST599.
        String cst599ScheduleRow =
                "//td[text()='cst599']/parent::tr";

        driver.findElement(
                By.xpath(cst599ScheduleRow + "//button[text()='Drop']")
        ).click();

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//button[text()='Yes']")
                )
        );

        driver.findElement(By.xpath("//button[text()='Yes']")).click();

        wait.until(
                ExpectedConditions.invisibilityOfElementLocated(
                        By.xpath("//td[text()='cst599']")
                )
        );

        List<WebElement> droppedRows =
                driver.findElements(
                        By.xpath("//td[text()='cst599']")
                );

        assertEquals(
                0,
                droppedRows.size(),
                "CST599 should be removed from Sam's schedule"
        );

        // Navigate to the enrollment page.
        driver.findElement(By.id("addCourseLink")).click();

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//td[text()='cst599']")
                )
        );

        // Enroll in CST599.
        String cst599EnrollmentRow =
                "//td[text()='cst599']/parent::tr";

        driver.findElement(
                By.xpath(cst599EnrollmentRow + "//button[text()='Enroll']")
        ).click();

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//button[text()='Yes']")
                )
        );

        driver.findElement(By.xpath("//button[text()='Yes']")).click();

        // View transcript and verify CST599 appears without a grade.
        driver.findElement(By.id("transcriptLink")).click();

        WebElement transcriptCourse =
                wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                By.xpath("//td[text()='cst599']")
                        )
                );

        assertNotNull(transcriptCourse);

        WebElement transcriptRow =
                transcriptCourse.findElement(By.xpath("./parent::tr"));

        List<WebElement> transcriptCells =
                transcriptRow.findElements(By.tagName("td"));

        assertEquals(
                7,
                transcriptCells.size(),
                "Transcript row should contain seven columns"
        );

        assertEquals(
                "",
                transcriptCells.get(6).getText(),
                "CST599 should appear without a grade"
        );

        // Log out as Sam.
        driver.findElement(By.id("logoutLink")).click();

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.id("loginButton")
                )
        );

        // Log in as instructor Ted.
        driver.findElement(By.id("email")).sendKeys("ted@csumb.edu");
        driver.findElement(By.id("password")).sendKeys("ted2025");
        driver.findElement(By.id("loginButton")).click();

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.id("year")
                )
        );

        // List Fall 2025 sections.
        driver.findElement(By.id("year")).sendKeys("2025");
        driver.findElement(By.id("semester")).sendKeys("Fall");
        driver.findElement(By.id("selectTermButton")).click();

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//td[text()='cst599']")
                )
        );

        // Open the CST599 roster.
        String cst599InstructorRow =
                "//td[text()='cst599']/parent::tr";

        driver.findElement(
                By.xpath(
                        cst599InstructorRow +
                        "//*[@id='enrollmentsLink']"
                )
        ).click();

        // RabbitMQ may need a moment to synchronize the re-enrollment.
        Thread.sleep(DELAY);

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//td[text()='sam@csumb.edu']")
                )
        );

        // Verify Sam appears exactly once in the roster.
        List<WebElement> samRosterEntries =
                driver.findElements(
                        By.xpath("//td[text()='sam@csumb.edu']")
                );

        assertEquals(
                1,
                samRosterEntries.size(),
                "Sam should appear exactly once in the CST599 roster"
        );

        WebElement samRosterRow =
                samRosterEntries.get(0).findElement(By.xpath("./parent::tr"));

        List<WebElement> rosterCells =
                samRosterRow.findElements(By.tagName("td"));

        assertEquals("sam", rosterCells.get(2).getText());
        assertEquals("sam@csumb.edu", rosterCells.get(3).getText());
    }
}