/*
 * Copyright (C) 2015 denkbares GmbH, Germany
 *
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */

package de.knowwe.uitest;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.Platform;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import de.d3web.strings.Strings;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Created by Veronika Sehne, Albrecht Striffler (denkbares GmbH) on 28.01.15.
 * <p>
 * Test the Test Protocol for DiaFlux (System Test - Manual DiaFlux BMI)
 */
public class DiaFluxSystemTest {

	public static final String RESOURCE_DIR = "src/test/resources/";
	private static WebDriver driver;

	/*
	 *  If you set devMode to true, you can test locally, which will be much faster
	 *  Don't commit this as true, because Jenkins build WILL fail!
	 *
	 *  To test locally, you also need to download the ChromeDriver from
	 *  https://sites.google.com/a/chromium.org/chromedriver/downloads
	 *  and start it on your machine. Also, you need a locally running KnowWE with a page "ST-BMI".
	 *  State of the page does not matter, it will be cleared for each new test.
	 */
	private static boolean devMode = false;

	@BeforeClass
	public static void setUp() throws Exception {
		// Create the connection to Sauce Labs to run the tests
		if (devMode) {
			driver = new RemoteWebDriver(new URL("http://localhost:9515"), DesiredCapabilities.chrome());
		}
		else {
			// Choose the browser, version, and platform to test
			DesiredCapabilities capabilities = DesiredCapabilities.chrome();
			capabilities.setCapability("name", DiaFluxSystemTest.class.getSimpleName());
			capabilities.setCapability("platform", Platform.WINDOWS);
			driver = new RemoteWebDriver(
					new URL("http://d3web:8c7e5a48-56dd-4cde-baf0-b17f83803044@ondemand.saucelabs.com:80/wd/hub"),
					capabilities);
		}
		driver.manage().window().setSize(new Dimension(1024, 768));
	}

	@Before
	public void load() throws Exception {
		if (devMode) {
			driver.get("http://localhost:8080/KnowWE/Wiki.jsp?page=ST-BMI");
		}
		else {
			driver.get("https://www.d3web.de/Wiki.jsp?page=ST-BMI");
			logIn();
		}
	}

	private void logIn() {
		List<WebElement> elements = driver.findElements(By.cssSelector("a.action.login"));
		if (elements.isEmpty()) return; // already logged in
		elements.get(0).click();
		driver.findElement(By.id("j_username")).sendKeys("test");
		driver.findElement(By.id("j_password")).sendKeys("8bGNmPjn");
		driver.findElement(By.name("submitlogin")).click();
		new WebDriverWait(driver, 10).until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("a.action.logout")));
		new WebDriverWait(driver, 10).until(ExpectedConditions.presenceOfElementLocated(By.id("edit-source-button")));
	}

	@Test
	public void addTerminology() throws IOException {
		changeArticleText(readFile("Step1.txt"));

		checkNoErrorsExist();
	}

	@Test
	public void addFlowchartStumps() throws Exception {
		changeArticleText(readFile("Step2.txt"));

		String article = driver.getWindowHandle();

		// first DiaFlux panel
		createNextFlow();
		switchToEditor(article);

		driver.findElement(By.id("properties.autostart")).click();

		setFlowName("BMI-Main");
		addStartNode(-300, 300);
		addExitNode(100, 300);

		saveAndSwitchBack(article);

		// second DiaFlux panel
		createNextFlow();
		switchToEditor(article);

		setFlowName("BMI-Anamnesis");

		addStartNode(-300, 300);
		addExitNode(-100, 450, "Illegal arguments");
		addExitNode(-250, 600, "Weight ok");
		addExitNode(0, 600, "Weight problem");

		saveAndSwitchBack(article);

		// third DiaFlux panel
		createNextFlow();
		switchToEditor(article);

		setFlowName("BMI-SelectTherapy");
		addStartNode(-300, 300, "Mild therapy");
		addStartNode(-300, 400, "Rigorous therapy");
		addExitNode(-100, 350, "Done");

		saveAndSwitchBack(article);

		// third DiaFlux panel
		createNextFlow();
		switchToEditor(article);

		setFlowName("BMI-SelectMode");
		addStartNode(-300, 300);
		addExitNode(-100, 450, "Pediatrics");
		addExitNode(0, 450, "Adult");

		saveAndSwitchBack(article);

	}

	@Test
	public void implementBMIMain() throws Exception {
		changeArticleText(readFile("Step3.txt"));

		String articleHandle = driver.getWindowHandle();

		openVisualEditor(1);

		switchToEditor(articleHandle);

		addActionNode(-300, 60, "BMI-SelectMode");

		connect(2, 4);
		connect(4, 3, "Pediatrics");

		addActionNode(-100, 150, "BMI-Anamnesis"); // 7

		connect(4, 7, "Adult");
		connect(7, 3, "Illegal arguments");
		connect(7, 3, "Weight ok");

		addActionNode(-150, 220, "bmi"); // 11

		connect(7, 11, "Weight problem");

		addActionNode(-500, 220, "BMI-SelectTherapy"); // 13
		addActionNode(-500, 320, "BMI-SelectTherapy", "Rigorous therapy"); // 14

		connect(11, 13, "Formula", "gradient(bmi[-7d, 0s]) >= 0 & gradient(bmi[-7d, 0s]) < 5");
		connect(11, 14, "Formula", "gradient(bmi[-7d, 0s]) >= 5");

		addActionNode(-150, 280, "Continue selected therapy", "ask"); // 17

		connect(11, 17, "Formula", "gradient(bmi[-7d, 0s]) < 0");

		addSnapshotNode(-160, 340); // 19

		connect(13, 19, "Done");
		connect(14, 19, "Done");
		connect(17, 19);

		connect(19, 7);

		saveAndSwitchBack(articleHandle);

		checkNoErrorsExist();
	}

	@Test
	public void implementBMIAnamnesis() throws Exception {
		changeArticleText(readFile("Step4.txt"));

		String articleHandle = driver.getWindowHandle();

		openVisualEditor(2);

		switchToEditor(articleHandle);

		addActionNode(-300, 60, "Height", "ask"); // 6
		addActionNode(-100, 60, "Weight", "always ask"); // 7

		connect(2, 6);
		connect(6, 7, "> ", "0");

		addActionNode(-300, 150, "Illegal arguments", "established"); // 10

		connect(6, 10, "= ", "0");
		connect(10, 3);

		addActionNode(-100, 250, "bmi", "Formula", "Weight / (Height * Height)"); // 13

		connect(7, 13, "known");

		addActionNode(-500, 250, "Weight classification", "Normal weight"); // 15
		addActionNode(-200, 320, "Weight classification", "Overweight"); // 16
		addActionNode(0, 320, "Weight classification", "Severe overweight"); // 17

		connect(13, 15, "[  ..  [", "18.5", "25");
		connect(13, 16, "[  ..  [", "25", "30");
		connect(13, 17, "≥ ", "30");

		connect(15, 4);
		connect(16, 5);
		connect(17, 5);

		saveAndSwitchBack(articleHandle);

		checkNoErrorsExist();
	}

	@Test
	public void implementBMISelectTherapy() throws Exception {
		changeArticleText(readFile("Step5.txt"));

		String articleHandle = driver.getWindowHandle();

		openVisualEditor(3);

		switchToEditor(articleHandle);

		// white spaces to give the auto complete some time
		addActionNode(-300, 60, "Therapy       " + Keys.ARROW_DOWN + Keys.ARROW_DOWN, "Mild therapy"); // 5
		addActionNode(-300, 120, "Therapy       " + Keys.ARROW_DOWN + Keys.ARROW_DOWN, "Rigorous therapy"); // 6

		connect(2, 5);
		connect(3, 6);
		connect(5, 4);
		connect(6, 4);

		saveAndSwitchBack(articleHandle);

		checkNoErrorsExist();
	}

	@Test
	public void implementBMISelectMode() throws Exception {
		changeArticleText(readFile("Step6.txt"));

		String articleHandle = driver.getWindowHandle();

		openVisualEditor(4);

		switchToEditor(articleHandle);

		addActionNode(-400, 60, "Age ", "ask"); // 5
		addActionNode(-220, 60, "Age classification", "Adult"); // 6
		addActionNode(-400, 120, "Age classification", "Pediatrics"); // 7
		addActionNode(-220, 120, "Age classification"); // 8

		connect(2, 5);
		connect(5, 6, "> ", "14");
		connect(5, 7, "≤ ", "14");
		connect(6, 8);
		connect(7, 8);
		connect(8, 3, "Pediatrics");
		connect(8, 4, "Adult");

		saveAndSwitchBack(articleHandle);

		checkNoErrorsExist();
	}

	@Test
	public void testKB1() throws IOException {
		changeArticleText(readFile("Step7.txt"));

		reset();
		setAge("21");
		setHeight("1.9");
		setWeight("90");

		assertBMI("24.930747922437675");

		assertEquals("Adult", driver.findElements(By.className("answerClicked")).get(0).getText());
		assertEquals("Normal weight", driver.findElements(By.className("answerClicked")).get(1).getText());

		reset();
	}

	@Test
	public void testKB2() throws IOException {
		changeArticleText(readFile("Step7.txt"));

		reset();
		setAge("14");
		setHeight("1.9");
		setWeight("90");

		assertBMI("");
		assertEquals("Pediatrics", driver.findElements(By.className("answerClicked")).get(0).getText());
		assertEquals(1, driver.findElements(By.className("answerClicked")).size());

		reset();
	}

	@Test
	public void testKB3() throws IOException {
		changeArticleText(readFile("Step7.txt"));

		reset();
		setAge("15");
		setHeight("1.9");
		setWeight("200");

		assertBMI("55.4016620498615");

		assertEquals("Adult", driver.findElements(By.className("answerClicked")).get(0).getText());
		assertEquals("Severe overweight", driver.findElements(By.className("answerClicked")).get(1).getText());

		reset();
	}

	@Test
	public void testKB4() throws IOException {
		changeArticleText(readFile("Step7.txt"));

		reset();
		setAge("21");
		setHeight("0");

		assertBMI("");

		assertEquals("Adult", driver.findElements(By.className("answerClicked")).get(0).getText());
		assertEquals("Illegal arguments", driver.findElement(By.cssSelector(".SOLUTION-ESTABLISHED a")).getText());
		// solution highlighted
		assertEquals("color: rgb(150, 110, 120);",
				driver.findElement(By.cssSelector(".type_Solution .clickable-term")).getAttribute("style"));

		reset();
	}

	@Test
	public void testTraces() throws IOException, InterruptedException {
		changeArticleText(readFile("Step7.txt"));

		reset();

		showTraces();

		// just checking the amount of highlighted nodes and edges...
		assertActiveNodes("BMI-Main", 2, 0);
		assertActiveEdges("BMI-Main", 1, 0);

		reset();
		setAge("21");
		setHeight("1.9");
		setWeight("95");

		assertActiveNodes("BMI-Main", 2, 4);
		assertActiveEdges("BMI-Main", 1, 5);

		assertActiveNodes("BMI-Anamnesis", 3, 3);
		assertActiveEdges("BMI-Anamnesis", 2, 3);

		assertActiveNodes("BMI-SelectTherapy", 0, 3);
		assertActiveEdges("BMI-SelectTherapy", 0, 2);

		assertActiveNodes("BMI-SelectMode", 0, 5);
		assertActiveEdges("BMI-SelectMode", 0, 4);
	}

	@Test
	public void testSpecialChars() throws IOException, InterruptedException {
		changeArticleText(readFile("Step8.txt"));

		checkErrorsExist();

		String articleHandle = driver.getWindowHandle();

		openVisualEditor(3);

		switchToEditor(articleHandle);

		editActionNode(5, "Therapy üöä", "" + Keys.ARROW_DOWN);
		editActionNode(5, "Therapy üöä", "" + Keys.ARROW_DOWN);
		// for whatever reason, clicking ok after editing the first time does not work (also when done manually)
		editActionNode(6, "Therapy       " + Keys.ARROW_DOWN + Keys.ARROW_DOWN, "" + Keys.ARROW_DOWN + Keys.ARROW_DOWN);

		saveAndSwitchBack(articleHandle);

		checkNoErrorsExist();

		openVisualEditor(3);

		switchToEditor(articleHandle);

		setFlowName("BMI-SelectTherapy üöäß$`´/\\=,!{};:_-");

		editStartNode(2, "Mild therapy üöäß$`´#/\\\\|=,!{};:_-");
		editStartNode(3, "Rigorous therapy üöäß$`´#/\\\\|=,!{};:_-");

		addCommentNode(-400, 300, "Here we test a lot of special characters üöäß&%$§`´<#>/\\\\|=,!(){};:_-"); // 11

		new Actions(driver).dragAndDropBy(driver.findElement(By.id("#node_3")), -100, 0).perform();
		connect(driver.findElement(By.id("#rule_8")).findElement(By.className("rule_selector")), 11);
		connect(11, 6);

		editExitNode(4, "Done üöäß$`´#/\\|=,!{};:_-");

		saveAndSwitchBack(articleHandle);

		checkErrorsExist();

		openVisualEditor(1);

		switchToEditor(articleHandle);

		editActionNode(13, "BMI-SelectTh", "" + Keys.ARROW_DOWN);
		// for whatever reason, clicking ok after editing the first time does not work (also when done manually)
		editActionNode(13, "BMI-SelectTh", "" + Keys.ARROW_DOWN);
		editActionNode(14, "BMI-SelectTh", "" + Keys.ARROW_DOWN + Keys.ARROW_DOWN);

		editEdge(20, "" + Keys.ARROW_DOWN);
		editEdge(21, "" + Keys.ARROW_DOWN);

		saveAndSwitchBack(articleHandle);

		checkNoErrorsExist();
	}

	@Test
	public void testSpecialCharTraces() throws IOException, InterruptedException {
		changeArticleText(readFile("Step9.txt"));

		reset();

		showTraces();

		// just checking the amount of highlighted nodes and edges...
		assertActiveNodes("BMI-Main", 2, 0);
		assertActiveEdges("BMI-Main", 1, 0);

		reset();
		setAge("21");
		setHeight("1.9");
		setWeight("95");

		assertBMI("26.315789473684212");
		assertEquals("Adult", driver.findElements(By.className("answerClicked")).get(0).getText());
		assertEquals("Overweight", driver.findElements(By.className("answerClicked")).get(1).getText());

		assertActiveNodes("BMI-Main", 2, 4);
		assertActiveEdges("BMI-Main", 1, 5);

		assertActiveNodes("BMI-Anamnesis", 3, 3);
		assertActiveEdges("BMI-Anamnesis", 2, 3);

		assertActiveNodes("flow_1c072bbf", 0, 4); // BMI-SelectTherapy
		assertActiveEdges("flow_1c072bbf", 0, 3); // BMI-SelectTherapy

		assertActiveNodes("BMI-SelectMode", 0, 5);
		assertActiveEdges("BMI-SelectMode", 0, 4);
	}

	private void showTraces() {
		if (driver.findElements(By.className("traceActive")).isEmpty()) {
			clickTool("type_DiaFlux", 2, "highlights active nodes");
			new WebDriverWait(driver, 10).until(ExpectedConditions.presenceOfElementLocated(By.className("traceActive")));
		}
	}

	private void editEdge(int edgeId, String text) {
		WebElement rule = driver.findElement(By.id("#rule_" + edgeId));
		rule.findElement(By.className("rule_selector")).click();
		WebElement ruleSelect = rule.findElement(By.tagName("select"));
		ruleSelect.click();
		ruleSelect.sendKeys(text + Keys.ENTER);
	}

	private void checkNoErrorsExist() {
		assertEquals(0, driver.findElements(By.className("error")).size());
	}

	private void checkErrorsExist() {
		assertFalse(driver.findElements(By.className("error")).isEmpty());
	}

	private void assertActiveNodes(String flow, int expectedActive, int expectedSnap) {
		assertEquals(expectedActive, driver.findElements(By.cssSelector("#" + flow + " .Node.traceActive")).size());
		assertEquals(expectedSnap, driver.findElements(By.cssSelector("#" + flow + " .Node.traceSnap")).size());
	}

	private void assertActiveEdges(String flow, int expectedActive, int expectedSnap) {
		int actualActive = 0;
		int actualSnap = 0;
		for (WebElement rule : driver.findElements(By.cssSelector("#" + flow + " .Rule"))) {
			if (!rule.findElements(By.className("traceSnap")).isEmpty()) actualSnap++;
			if (!rule.findElements(By.className("traceActive")).isEmpty()) actualActive++;
		}
		assertEquals(expectedActive, actualActive);
		assertEquals(expectedSnap, actualSnap);
	}

	private void reset() {
		driver.findElement(By.className("reset")).click();
		awaitRerender(By.className("reset"));
	}

	private void assertBMI(String expected) {
		assertEquals(expected, driver.findElements(By.className("numinput")).get(3).getAttribute("value"));
	}

	private void setWeight(String value) {
		setValue(value, 2);
	}

	private void setHeight(String value) {
		setValue(value, 1);
	}

	private void setAge(String value) {
		setValue(value, 0);
	}

	private void setValue(String value, int index) {
		driver.findElements(By.className("numinput")).get(index).sendKeys(value + Keys.ENTER);
		awaitRerender(By.className("reset"));
	}

	private void connect(int sourceId, int targetId, String... text) {
		connect(driver.findElement(By.id("#node_" + sourceId)), targetId, text);
	}

	private void connect(WebElement source, int targetId, String... text) {
		source.click();
		WebElement arrowTool = driver.findElement(By.className("ArrowTool"));
		WebElement targetNode = driver.findElement(By.id("#node_" + targetId));
		(new Actions(driver)).dragAndDrop(arrowTool, targetNode).perform();
		if (text.length > 0) {
			WebElement select = driver.findElement(By.cssSelector(".selectedRule select"));
			select.click();
			if (text[0].equalsIgnoreCase("formula")) {
				select.findElement(By.xpath("//option[@value='" + 13 + "']")).click();
				driver.findElement(By.cssSelector(".selectedRule textarea")).sendKeys(text[1] + Keys.ENTER);
			}
			else {
				select.findElement(By.xpath("//option[text()='" + text[0] + "']")).click();
				if (text.length > 1) {
					List<WebElement> inputs = driver.findElements(By.cssSelector(".GuardEditor input"));
					inputs.get(0).sendKeys(text[1]);
					if (text.length > 2) {
						inputs.get(1).sendKeys(text[2] + Keys.ENTER);
					}
					else {
						inputs.get(0).sendKeys(Keys.ENTER);
					}
				}
			}
		}
	}

	private void openVisualEditor(int nth) throws InterruptedException {
		int attempt = 0;
		while (attempt < 5 && driver.getWindowHandles().size() == 1) {
			clickTool("type_DiaFlux", nth, "visual editor");
			Thread.sleep(500);
			attempt++;
		}
	}

	private void clickTool(String markupClass, int nth, String tooltipContains) {
		WebElement markup = driver.findElements(By.className(markupClass)).get(nth - 1);
		WebElement toolMenu = markup.findElement(By.className("headerMenu"));
		WebElement editTool = markup.findElements(By.cssSelector(".markupMenu a.markupMenuItem"))
				.stream()
				.filter(element -> Strings.containsIgnoreCase(element.getAttribute("title"), tooltipContains))
				.findFirst().get();
		new Actions(driver).moveToElement(toolMenu).moveToElement(editTool).click(editTool).perform();
	}

	private void changeArticleText(String newText) {
		new WebDriverWait(driver, 10).until(ExpectedConditions.presenceOfElementLocated(By.id("edit-source-button")));
		driver.findElement(By.id("edit-source-button")).click();
		WebElement editorArea = new WebDriverWait(driver, 10).until(ExpectedConditions.presenceOfElementLocated(By.id("editorarea")));
		if (driver instanceof JavascriptExecutor) {
			// hacky but fast/instant!
			((JavascriptExecutor) driver).executeScript("document.getElementById('editorarea').value = arguments[0]", newText);
		}
		else {
			// sets the keys one by one, pretty slow...
			editorArea.clear();
			editorArea.sendKeys(newText);
		}
		driver.findElement(By.name("ok")).click();
	}

	private void createNextFlow() {
		driver.findElement(By.linkText("Click here to create one.")).click();
	}

	private void saveAndSwitchBack(String winHandleBefore) {
		driver.findElement(By.id("saveClose")).click();
		driver.switchTo().window(winHandleBefore);
		awaitRerender(By.id("pagecontent"));
	}

	private void addActionNode(int xOffset, int yOffset, String... text) throws InterruptedException {
		addNode(xOffset, yOffset, By.id("decision_prototype"), By.cssSelector(".NodeEditor .ObjectSelect *"), text);
	}

	private void addStartNode(int xOffset, int yOffset, String... text) throws InterruptedException {
		addNode(xOffset, yOffset, By.id("start_prototype"), By.cssSelector(".NodeEditor .startPane input"), text);
	}

	private void addSnapshotNode(int xOffset, int yOffset, String... text) throws InterruptedException {
		addNode(xOffset, yOffset, By.id("snapshot_prototype"), By.cssSelector(".NodeEditor .snapshotPane input"), text);
	}

	private void addCommentNode(int xOffset, int yOffset, String... text) throws InterruptedException {
		addNode(xOffset, yOffset, By.id("comment_prototype"), By.cssSelector(".NodeEditor .commentPane textarea"), text);
	}

	private void addExitNode(int xOffset, int yOffset, String... text) throws InterruptedException {
		addNode(xOffset, yOffset, By.id("exit_prototype"), By.cssSelector(".NodeEditor .exitPane input"), text);
	}

	private void addNode(int xOffset, int yOffset, By prototypeSelector, By textSelector, String... text) throws InterruptedException {
		WebElement start = driver.findElement(prototypeSelector);
		new Actions(driver).dragAndDropBy(start, xOffset, yOffset).perform();
		Thread.sleep(200);
		if (text.length > 0) {
			List<WebElement> nodes = driver.findElements(By.cssSelector(".Flowchart > .Node"));
			WebElement newNode = nodes.get(nodes.size() - 1);
			new Actions(driver).doubleClick(newNode).perform();
			setNodeAttributes(textSelector, text);
		}

	}

	private void editStartNode(int nodeId, String... text) throws InterruptedException {
		editNode(nodeId, By.cssSelector(".NodeEditor .startPane input"), text);
	}

	private void editExitNode(int nodeId, String... text) throws InterruptedException {
		editNode(nodeId, By.cssSelector(".NodeEditor .exitPane input"), text);
	}

	private void editActionNode(int nodeId, String... text) throws InterruptedException {
		editNode(nodeId, By.cssSelector(".NodeEditor .ObjectSelect *"), text);
	}

	private void editNode(int nodeId, By textSelector, String... text) throws InterruptedException {
		new Actions(driver).doubleClick(driver.findElement(By.id("#node_" + nodeId))).perform();
		setNodeAttributes(textSelector, text);
	}

	private void setNodeAttributes(By textSelector, String... text) throws InterruptedException {
		driver.findElement(textSelector).click();
		driver.findElement(textSelector).clear();
		driver.findElement(textSelector).sendKeys(text[0]);
		Thread.sleep(200);
		driver.findElement(textSelector).sendKeys(Keys.ENTER);
		if (text.length > 1) {
			WebElement actionSelect = driver.findElement(By.cssSelector(".ActionEditor select"));
			actionSelect.click();
			if (text[1].equalsIgnoreCase("formula")) {
				actionSelect.findElement(By.xpath("//option[@value='" + 1 + "']")).click();
				driver.findElement(By.cssSelector(".ActionEditor textarea")).sendKeys(text[2] + Keys.ENTER);
			}
			else if (text[1].startsWith("" + Keys.ARROW_DOWN)) {
				actionSelect.sendKeys(text[1] + Keys.ENTER);
			}
			else {
				actionSelect.findElement(By.xpath("//option[text()='" + text[1] + "']")).click();
			}
		}

		List<WebElement> okButtons = driver.findElements(By.cssSelector(".NodeEditor .ok"));
		if (okButtons.size() == 1) okButtons.get(0).click();
	}

	private void setFlowName(String flowName) {
		driver.findElement(By.id("properties.editName")).clear();
		driver.findElement(By.id("properties.editName")).sendKeys(flowName);
	}

	private void switchToEditor(String articleHandle) throws InterruptedException {
		Thread.sleep(500);
		Set<String> windowHandles = new HashSet<>(driver.getWindowHandles());
		windowHandles.remove(articleHandle);
		driver.switchTo().window(windowHandles.iterator().next());
		new WebDriverWait(driver, 10).until(ExpectedConditions.presenceOfElementLocated(By.id("start_prototype")));
	}

	private void awaitRerender(By by) {
		try {
			new WebDriverWait(driver, 10).until(ExpectedConditions.stalenessOf(driver.findElement(by)));
		}
		catch (TimeoutException ignore) {
		}
		new WebDriverWait(driver, 10).until(ExpectedConditions.presenceOfElementLocated(by));
	}

	private String readFile(String fileName) throws IOException {
		return Strings.readFile(RESOURCE_DIR + fileName);
	}

	@AfterClass
	public static void tearDown() throws Exception {
		// if we quit, we don't see the status of the test at the end
		if (!devMode) driver.quit();
	}
}
