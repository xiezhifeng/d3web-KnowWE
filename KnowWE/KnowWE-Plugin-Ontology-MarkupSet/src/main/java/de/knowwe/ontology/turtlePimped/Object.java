/*
 * Copyright (C) 2013 denkbares GmbH
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
package de.knowwe.ontology.turtlePimped;

import java.util.List;

import org.ontoware.rdf2go.model.node.Node;
import org.ontoware.rdf2go.model.node.Resource;
import org.ontoware.rdf2go.model.node.URI;

import de.d3web.strings.Strings;
import de.knowwe.core.Environment;
import de.knowwe.core.compile.terminology.TerminologyManager;
import de.knowwe.core.kdom.AbstractType;
import de.knowwe.core.kdom.Article;
import de.knowwe.core.kdom.Type;
import de.knowwe.core.kdom.objects.TermReference;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.parsing.Sections;
import de.knowwe.core.kdom.sectionFinder.SectionFinder;
import de.knowwe.core.kdom.sectionFinder.SectionFinderResult;
import de.knowwe.core.report.Message;
import de.knowwe.ontology.turtlePimped.compile.NodeProvider;
import de.knowwe.ontology.turtlePimped.compile.StatementProvider;
import de.knowwe.ontology.turtlePimped.compile.StatementProviderResult;
import de.knowwe.rdf2go.Rdf2GoCore;

public class Object extends AbstractType implements NodeProvider<Object>, StatementProvider<Object> {

	public Object() {
		this.setSectionFinder(new SectionFinder() {

			@Override
			public List<SectionFinderResult> lookForSections(String text, Section<?> father, Type type) {
				return SectionFinderResult.createResultList(Strings.splitUnquoted(text, ",", true,
						TurtleMarkup.TURTLE_QUOTES));
			}
		});
		this.addChildType(new BlankNode());
		this.addChildType(new BlankNodeID());
		this.addChildType(TurtleCollection.getInstance());
		this.addChildType(new TurtleLiteralType());
		this.addChildType(new TurtleLongURI());
		this.addChildType(new TurtleURI());
	}

	@Override
	public StatementProviderResult getStatements(Section<Object> section, Rdf2GoCore core, Article article) {

		StatementProviderResult result = new StatementProviderResult();
		boolean termError = false;
		/*
		 * Handle OBJECT
		 */
		Node object = section.get().getNode(section, core);
		Section<TurtleURI> turtleURITermObject = Sections.findChildOfType(section, TurtleURI.class);
		if (turtleURITermObject != null) {
			boolean isDefined = checkTurtleURIDefinition(article, turtleURITermObject);
			if (!isDefined) {
				// error message is already rendered by term reference renderer
				// we do not insert statement in this case
				object = null;
				termError = true;
			}
		}
		if (object == null && !termError) {
			result.addMessage(new Message(de.knowwe.core.report.Message.Type.ERROR,
					"object node was null for: " + section.getText()));
		}

		/*
		 * Handle PREDICATE
		 */
		Section<PredicateSentence> predSentenceSection = Sections.findAncestorOfType(section,
				PredicateSentence.class);
		Section<Predicate> predicateSection = Sections.findChildOfType(predSentenceSection,
				Predicate.class);
		
		URI predicate = predicateSection.get().getURI(predicateSection, core);

		// check term definition
		Section<TurtleURI> turtleURITerm = Sections.findSuccessor(predicateSection, TurtleURI.class);
		if(turtleURITerm != null) {
			boolean isDefined = checkTurtleURIDefinition(article, turtleURITerm);
			if (!isDefined) {
				// error message is already rendered by term reference renderer
				// we do not insert statment in this case
				predicate = null;
				termError = true;
			}
		}
		if (predicate == null && !termError) {
			result.addMessage(new Message(de.knowwe.core.report.Message.Type.ERROR,
					"predicate URI was null for: " + predicateSection.getText()));
		}

		/*
		 * Handle SUBJECT
		 */
		Resource subject = null;
		// the subject can either be a normal turtle sentence subject
		// OR a blank node
		Section<BlankNode> blankNodeSection = Sections.findAncestorOfType(predSentenceSection,
				BlankNode.class);
		if (blankNodeSection != null) {
			subject = blankNodeSection.get().getResource(blankNodeSection, core);
			if (subject == null) {
				result.addMessage(new Message(de.knowwe.core.report.Message.Type.ERROR,
						"subject resource was null for: " + blankNodeSection.getText()));
			}
		}
		else {
			Section<TurtleSentence> sentence = Sections.findAncestorOfType(predSentenceSection,
					TurtleSentence.class);
			Section<Subject> subjectSection = Sections.findSuccessor(sentence,
					Subject.class);
			subject = subjectSection.get().getResource(subjectSection, core);

			// check term definition
			Section<TurtleURI> turtleURITermSubject = Sections.findChildOfType(subjectSection,
					TurtleURI.class);
			if (turtleURITermSubject != null) {
				boolean isDefined = checkTurtleURIDefinition(article, turtleURITermSubject);
				if (!isDefined) {
					// error message is already rendered by term reference
					// renderer
					// we do not insert statement in this case
					subject = null;
					termError = true;
				}
			}

			if (subject == null && !termError) {
				result.addMessage(new Message(de.knowwe.core.report.Message.Type.ERROR,
						"subject resource was null for: " + subjectSection.getText()));
			}
		}




		// create statement
		if (object != null && predicate != null && subject != null) {
			result.addStatement(core.createStatement(subject, predicate, object));
		}
		return result;
	}

	private boolean checkTurtleURIDefinition(Article article, Section<TurtleURI> turtleURITerm) {
		TerminologyManager terminologyManager = Environment.getInstance().getTerminologyManager(article);
		Section<TermReference> term = Sections.findSuccessor(turtleURITerm, TermReference.class);
		return terminologyManager.isDefinedTerm(term.get().getTermIdentifier(term));
	}

	@Override
	@SuppressWarnings({
			"rawtypes", "unchecked" })
	public Node getNode(Section<Object> section, Rdf2GoCore core) {
		// there should be exactly one NodeProvider child (while potentially
		// many successors)
		Section<NodeProvider> nodeProviderChild = Sections.findChildOfType(section,
				NodeProvider.class);
		if (nodeProviderChild != null) {

			return nodeProviderChild.get().getNode(nodeProviderChild, core);
		}
		return null;
	}

}