/*
 * Copyright (C) 2016 denkbares GmbH, Germany
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

import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;

import static org.junit.Assert.assertEquals;

/**
 * Testing Formatter button.
 * <p>
 * Created by Adrian Müller on 13.10.16.
 */
public abstract class FormatterUITest extends KnowWEUITest {

	public FormatterUITest() {
		super();
	}

	@Override
	public String getTestName() {
		return "Formatter";
	}

	@Rule
	public UITestUtils.RetryRule retry = new UITestUtils.RetryRule(2);

	@Test
	public void testSparqlFormatButton() throws InterruptedException, IOException {
		changeArticleText(readFile("FormatterUITest.txt"));

		String unformatted = "%%Sparql\n" +
				"\n" +
				"  SELECT ?Substance ?CAS ?EC ?Name\n" +
				"  \n" +
				"  WHERE {\n" +
				"  \n" +
				"    ?Substance rdf:type lns:Substance .\n" +
				"    OPTIONAL { ?Substance <lns:hasFirstCAS+Number> ?CAS }\n" +
				"    OPTIONAL { ?Substance <lns:hasFirstEC+Number> ?EC }\n" +
				"    OPTIONAL { ?Substance <lns:hasFirstSubstance+Name> ?Name }\n" +
				"    OPTIONAL { \n" +
				"?Substance <lns:hasFirstGroup+Name> ?Name \n" +
				"}\n" +
				"    \n" +
				"    ?Substance lns:hasEstablished <lns:OECD+HPV+Chemical>\n" +
				"    \n" +
				"    }\n" +
				"        ORDER BY ?Name ?CAS ?EC\n" +
				"%\n";

		String actual = testFormatButton(unformatted);
		String expectedSparql = "%%Sparql\n" +
				"\n" +
				"SELECT ?Substance ?CAS ?EC ?Name\n" +
				"\n" +
				"WHERE {\n" +
				"\t\n" +
				"\t?Substance rdf:type lns:Substance .\n" +
				"\tOPTIONAL { ?Substance <lns:hasFirstCAS+Number> ?CAS }\n" +
				"\tOPTIONAL { ?Substance <lns:hasFirstEC+Number> ?EC }\n" +
				"\tOPTIONAL { ?Substance <lns:hasFirstSubstance+Name> ?Name }\n" +
				"\tOPTIONAL { ?Substance <lns:hasFirstGroup+Name> ?Name }\n" +
				"\t\n" +
				"\t?Substance lns:hasEstablished <lns:OECD+HPV+Chemical>\n" +
				"\t\n" +
				"}\n" +
				"ORDER BY ?Name ?CAS ?EC\n" +
				"%\n";

		assertEquals("Compared Sparqls are not equal", expectedSparql, actual);
	}

	@Test
	public void testTurtleFormatButton() throws InterruptedException, IOException {
		changeArticleText(readFile("FormatterUITest.txt"));

		String unformatted = "%%turtle\n" +
				"si:SimpsonsConcept\n" +
				"\t?a\n" +
				"\t\trdfs:Class .\n" +
				"\n" +
				"si:LivingBeing rdfs:subClassOf si:SimpsonsConcept ;\n" +
				"\trdfs:label \"Lebewesen\"@de, \"Living being\" .\n" +
				"\n" +
				"si:Human\n" +
				"\trdfs:subClassOf si:LivingBeing ;\n" +
				"\t?a rdfs:Class ;\n" +
				"\t\n" +
				"\t\n" +
				"\t\n" +
				"\trdfs:label \"Mensch\"@de, \"Human\"@en .\n" +
				"\n" +
				"%";
		String actual = testFormatButton(unformatted);
		String expectedSparql = "%%turtle\n" +
				"si:SimpsonsConcept\n" +
				"\t?a\n" +
				"\t\trdfs:Class .\n" +
				"\n" +
				"si:LivingBeing rdfs:subClassOf si:SimpsonsConcept ;\n" +
				"\trdfs:label \"Lebewesen\"@de, \"Living being\" .\n" +
				"\n" +
				"si:Human\n" +
				"\trdfs:subClassOf si:LivingBeing ;\n" +
				"\t?a rdfs:Class ;\n" +
				"\t\n" +
				"\trdfs:label \"Mensch\"@de, \"Human\"@en .\n" +
				"\n" +
				"%\n";
		assertEquals("Compared Turtle are not equal", expectedSparql, actual);
	}

	@Test
	public void testRuleFormatButton() throws InterruptedException, IOException {
		changeArticleText(readFile("FormatterUITest.txt"));

		String unformatted = "%%Rule\n" +
				"\n" +
				"IF Driving = insufficient power on full load \n" +
				"   AND Mileage evaluation = slightly increased\n" +
				"THEN Leaking air intake system = P5\n" +
				"\n" +
				"IF NOT (Driving = insufficient power on sorted partial load OR \n" +
				"     Driving = unsteady idle speed OR \n" +
				"     Driving = insufficient power on full load) AND something = \"Hallo OR , ich stehe in der Zeile\"\n" +
				"THEN Leaking air intake system  = N3\n" +
				"\n" +
				"IF Driving = insufficient power on full load\n" +
				"THEN Leaking air intake system = P5\n" +
				"\n" +
				"%";
		String actual = testFormatButton(unformatted);
		String expectedSparql = "%%Rule\n" +
				"\n" +
				"IF Driving = insufficient power on full load\n" +
				"\tAND Mileage evaluation = slightly increased\n" +
				"THEN Leaking air intake system = P5\n" +
				"\n" +
				"IF NOT (Driving = insufficient power on sorted partial load\n" +
				"\t\tOR Driving = unsteady idle speed\n" +
				"\t\tOR Driving = insufficient power on full load)\n" +
				"\tAND something = \"Hallo OR , ich stehe in der Zeile\"\n" +
				"THEN Leaking air intake system = N3\n" +
				"\n" +
				"IF Driving = insufficient power on full load\n" +
				"THEN Leaking air intake system = P5\n" +
				"\n" +
				"%\n";
		assertEquals("Compared Rule are not equal", expectedSparql, actual);
	}

	private String testFormatButton(String unformatted) {
		waitUntilClickable(By.id("edit-mode-button")).click();
		waitUntilClickable(By.className("markupHeader")).click();
		waitUntilPresent(By.className("defaultEditTool"));
		find(By.className("defaultEditTool")).clear();
		find(By.className("defaultEditTool")).sendKeys(unformatted);
		saveAndReopen();
		find(By.cssSelector(".action.format")).click();
		saveAndReopen();
		waitUntilPresent(By.className("defaultEditTool"));
		return find(By.className("defaultEditTool")).getAttribute("value");
	}

	private void saveAndReopen() {
		find(By.cssSelector(".action.save")).click();
		waitUntilPresent(By.id("edit-mode-button"));
		waitUntilVisible(By.id("edit-mode-button")).click();
		waitUntilClickable(By.className("markupHeader")).click();
	}

}
