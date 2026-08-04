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
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

public class EnterEnrollmentGradesSystemTest {

    static final String URL = "http://localhost:5173";

    WebDriver driver;
    Wait<WebDriver> wait;

    @BeforeEach
    public void setUpDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--lang=en-US");

        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(3));
        wait = new WebDriverWait(driver, Duration.ofSeconds(8));
        driver.get(URL);
    }

    @AfterEach
    public void quit() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    public void instructorEntersEnrollmentGrades() throws InterruptedException {

        // Log in as instructor Ted.
        driver.findElement(By.id("email")).sendKeys("ted@csumb.edu");
        driver.findElement(By.id("password")).sendKeys("ted2025");
        driver.findElement(By.id("loginButton")).click();

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.id("year")
                )
        );

        // View Ted's Fall 2025 sections.
        driver.findElement(By.id("year")).sendKeys("2025");
        driver.findElement(By.id("semester")).sendKeys("Fall");
        driver.findElement(By.id("selectTermButton")).click();

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//td[text()='cst599']")
                )
        );

        // Open the CST599 enrollment roster.
        String cst599SectionRow =
                "//td[text()='cst599']/parent::tr";

        driver.findElement(
                By.xpath(
                        cst599SectionRow +
                        "//*[@id='enrollmentsLink']"
                )
        ).click();

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//td[text()='sama']")
                )
        );

        // Enter final grades.
        setGradeForStudent("sama", "A");
        setGradeForStudent("samb", "B+");
        setGradeForStudent("samc", "C");

        // Save the grades.
        driver.findElement(By.id("saveGradesButton")).click();

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//*[contains(text(), 'Final grades saved')]")
                )
        );

        // Allow RabbitMQ time to send the grade updates to registrar.
        Thread.sleep(2000);

        // Return to the instructor home page.
        driver.findElement(By.id("homeLink")).click();

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.id("year")
                )
        );

        // View Fall 2025 sections again.
        driver.findElement(By.id("year")).sendKeys("2025");
        driver.findElement(By.id("semester")).sendKeys("Fall");
        driver.findElement(By.id("selectTermButton")).click();

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//td[text()='cst599']")
                )
        );

        // Reopen the CST599 roster.
        driver.findElement(
                By.xpath(
                        cst599SectionRow +
                        "//*[@id='enrollmentsLink']"
                )
        ).click();

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//td[text()='sama']")
                )
        );

        // Verify the saved grades.
        assertEquals("A", getGradeForStudent("sama"));
        assertEquals("B+", getGradeForStudent("samb"));
        assertEquals("C", getGradeForStudent("samc"));

        // Log out as Ted.
        driver.findElement(By.id("logoutLink")).click();

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.id("loginButton")
                )
        );

        // Log in as student Samb.
        driver.findElement(By.id("email")).sendKeys("samb@csumb.edu");
        driver.findElement(By.id("password")).sendKeys("samb2025");
        driver.findElement(By.id("loginButton")).click();

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.id("transcriptLink")
                )
        );

        // Open Samb's transcript.
        driver.findElement(By.id("transcriptLink")).click();

        WebElement cst599Course =
                wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                By.xpath("//td[text()='cst599']")
                        )
                );

        assertNotNull(cst599Course);

        // Verify CST599 is listed with a grade of B+.
        WebElement transcriptRow =
                cst599Course.findElement(By.xpath("./parent::tr"));

        List<WebElement> transcriptCells =
                transcriptRow.findElements(By.tagName("td"));

        assertEquals(
                7,
                transcriptCells.size(),
                "Transcript row should contain seven columns"
        );

        assertEquals(
                "B+",
                transcriptCells.get(6).getText(),
                "Samb should have a B+ in CST599"
        );
    }

    private void setGradeForStudent(String studentName, String grade) {
        String selectPath =
                "//td[text()='" + studentName + "']" +
                "/parent::tr//select";

        WebElement selectElement =
                driver.findElement(By.xpath(selectPath));

        Select gradeSelect = new Select(selectElement);
        gradeSelect.selectByValue(grade);
    }

    private String getGradeForStudent(String studentName) {
        String selectPath =
                "//td[text()='" + studentName + "']" +
                "/parent::tr//select";

        WebElement selectElement =
                driver.findElement(By.xpath(selectPath));

        Select gradeSelect = new Select(selectElement);
        return gradeSelect.getFirstSelectedOption().getAttribute("value");
    }
}
