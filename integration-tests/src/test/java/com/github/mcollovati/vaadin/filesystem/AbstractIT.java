package com.github.mcollovati.vaadin.filesystem;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

abstract class AbstractIT {

    private static final String BASE_URL = System.getProperty("it.baseUrl", "http://localhost:8080");

    private static Playwright playwright;
    private static Browser browser;
    private BrowserContext context;
    Page page;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
    }

    @AfterAll
    static void closeBrowser() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    @BeforeEach
    void createContext() {
        context = browser.newContext();
        page = context.newPage();
    }

    @AfterEach
    void closeContext() {
        if (context != null) {
            context.close();
        }
    }

    void navigateTo(String route) {
        page.navigate(BASE_URL + "/" + route);
        page.waitForLoadState();
    }

    void clickButton(String id) {
        page.locator("#" + id).click();
    }

    void waitForLog(String text) {
        assertThat(page.locator("#log")).containsText(text);
    }

    void waitForLog(String text, double timeout) {
        assertThat(page.locator("#log"))
                .containsText(
                        text,
                        new com.microsoft.playwright.assertions.LocatorAssertions.ContainsTextOptions()
                                .setTimeout(timeout));
    }

    String getLogText() {
        return page.locator("#log").textContent();
    }
}
