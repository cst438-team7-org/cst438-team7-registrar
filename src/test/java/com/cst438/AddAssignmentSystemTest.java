package com.cst438;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

public class AddAssignmentSystemTest {
    // Properties
    // Static Final
    static final String CHROME_DRIVER_FILE_LOCATION = ""; // copy path to chrome driver here
    static final String URL = "http://localhost:5173";   // react dev server
    static final int DELAY = 2000;
    // Other
    WebDriver driver;
    Wait<WebDriver> wait;
    Random random = new Random();

    @BeforeEach
    public void setUpDriver() throws Exception {
        // set properties required by Chrome Driver
        System.setProperty("webdriver.chrome.driver", CHROME_DRIVER_FILE_LOCATION);
        ChromeOptions ops = new ChromeOptions();
        ops.addArguments("--remote-allow-origins=*");
        ops.addArguments("--lang=en-US");

        // Start the driver
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
    public void addGradeAssignment() throws InterruptedException {
        // Login as instructor
        // Instructor Credentials
        String email = "ted@csumb.edu";
        String password = "ted2025";
        // Enter credentials and click login
        driver.findElement(By.id("email")).sendKeys(email);
        driver.findElement(By.id("password")).sendKeys(password);
        driver.findElement(By.id("loginButton")).click();

        // Enter Term year and semester
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("year")));
        driver.findElement(By.id("year")).sendKeys("2025");
        driver.findElement(By.id("semester")).sendKeys("Fall");
        driver.findElement(By.id("selectTermButton")).click();

        // Navigate to cst599 assignments
        String assignmentsLinkPath = "//td[text()='cst599']/../td/*[@id='assignmentsLink']";
        driver.findElement(By.xpath(assignmentsLinkPath)).click();

        // Navigate to add assignment dialog
        driver.findElement(By.id("addAssignmentButton")).click();

        // Add Assignment Dialog Variables
        String addDialogPath = "//button[@id='addAssignmentButton']/following-sibling::dialog//";
        String addTitlePath = addDialogPath + "input[@id='title']";
        String addDueDatePath = addDialogPath + "input[@id='dueDate']";
        String addSaveButtonPath = addDialogPath + "button[@id='saveButton']";

        // Try to add assignment with no title
        driver.findElement(By.xpath(addSaveButtonPath)).click();
        // Check message
        assertNotNull(driver.findElement(By.xpath("//*[contains(text(), 'cannot be blank')]")));

        // Try to add assignment with invalid characters in title
        driver.findElement(By.xpath(addTitlePath)).sendKeys("!@#$%^&*()");
        driver.findElement(By.xpath(addSaveButtonPath)).click();
        // Check message
        assertNotNull(driver.findElement(By.xpath("//*[contains(text(), 'invalid char in title')]")));
        // Clear title input
        driver.findElement(By.xpath(addTitlePath)).clear();

        // Get Due Date
        int year = 2025;
        int month = random.nextInt(5) + 8; // August-December
        int day;
        if(month == 8) { // August
            day = random.nextInt(11) + 20; // 20-30
        } else if (month == 12) { // December
            day = random.nextInt(17) + 1; // 1-17
        } else { // September-November
            day = random.nextInt(29) + 1; // 1-29
        }
        String dueDateInput = String.format("%02d%02d%04d", month, day, year);

        // Add Assignment
        // Enter Title
        String title = "Assignment " + Math.abs(random.nextInt());
        driver.findElement(By.xpath(addTitlePath)).sendKeys(title);
        // Enter Due Date
        driver.findElement(By.xpath(addDueDatePath)).sendKeys(dueDateInput);
        // Click save
        driver.findElement(By.xpath(addSaveButtonPath)).click();
        // Check message
        assertNotNull(driver.findElement(By.xpath("//*[contains(text(), 'successfully created')]")));
        // Click close
        driver.findElement(By.xpath(addDialogPath + "button[@id='closeButton']")).click();
    }
}
