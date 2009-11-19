/*
 * Copyright (C) 2009 Chair of Artificial Intelligence and Applied Informatics
 *                    Computer Science VI, University of Wuerzburg
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package de.d3web.we.selenium.tests;

public class CommentTest extends KnowWETestCase{
	
	
	public void testNewCommentEntry() throws Exception{
		selenium.open("");
		assertEquals("KnowWE: Main", selenium.getTitle());
		loadAndWait("link=Selenium-Test");
		
		openWindowBlank(rb.getString("KnowWE.SeleniumTest.server") + 
				"Wiki.jsp?page=Selenium-Comment", "KnowWE: Selenium-Comment");
		assertTrue(selenium.getTitle().contains("KnowWE: Selenium-Comment"));
		verifyTrue(selenium.isTextPresent("<< back"));
		selenium.type("text", "Das ist ein Selenium-Test");
		selenium.click("//div[@onclick='saveForumBox()']");
		
		loadAndWait("link=<< back");
		verifyEquals("KnowWE: Selenium-Test", selenium.getTitle());
		
		openWindowBlank(rb.getString("KnowWE.SeleniumTest.server") + 
				"Wiki.jsp?page=Selenium-Comment", "KnowWE: Selenium-Comment");		
		assertEquals(selenium.getTitle(), "KnowWE: Selenium-Comment");
		assertTrue("Neuer Beitrag wurde nicht/nicht richtig gespeichert",
				selenium.isTextPresent("Das ist ein Selenium-Test"));
		}
}
