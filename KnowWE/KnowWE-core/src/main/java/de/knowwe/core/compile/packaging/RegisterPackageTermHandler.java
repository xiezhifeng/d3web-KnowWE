/*
 * Copyright (C) 2013 University Wuerzburg, Computer Science VI
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
package de.knowwe.core.compile.packaging;

import java.util.Collection;
import java.util.Set;

import de.d3web.strings.Identifier;
import de.knowwe.core.Environment;
import de.knowwe.core.compile.terminology.TerminologyManager;
import de.knowwe.core.kdom.Article;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.subtreeHandler.SubtreeHandler;
import de.knowwe.core.report.Message;
import de.knowwe.core.report.Messages;
import de.knowwe.core.utils.KnowWEUtils;
import de.knowwe.kdom.defaultMarkup.DefaultMarkupType;


/**
 * 
 * @author stefan
 * @created 30.08.2013
 */
public class RegisterPackageTermHandler extends SubtreeHandler<DefaultMarkupType> {

	@Override
	public Collection<Message> create(Article article, Section<DefaultMarkupType> section) {
		TerminologyManager terminologyHandler = KnowWEUtils.getTerminologyManager(article);

		String[] annotationStrings = DefaultMarkupType.getAnnotations(section,
				PackageManager.PACKAGE_ATTRIBUTE_NAME);
		// register definition for the default package if there is no
		// annotation
		// to specify another package
		if (annotationStrings.length == 0) {

			PackageManager packageManager = Environment.getInstance().getPackageManager(
					article.getWeb());
			Set<String> defaultPackages = packageManager.getDefaultPackages(article);
			for (String defaultPackage : defaultPackages) {
				terminologyHandler.registerTermReference(section,
						Package.class,
						new Identifier(defaultPackage));
			}
		}
		else {
			for (String annotationString : annotationStrings) {
				terminologyHandler.registerTermReference(section,
						Package.class,
						new Identifier(annotationString));

			}
		}
		return Messages.noMessage();
	}

}