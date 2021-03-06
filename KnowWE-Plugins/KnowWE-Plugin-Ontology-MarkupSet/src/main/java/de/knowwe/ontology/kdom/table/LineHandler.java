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
package de.knowwe.ontology.kdom.table;

import java.util.LinkedList;
import java.util.List;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;

import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.parsing.Sections;
import de.knowwe.core.report.CompilerMessage;
import de.knowwe.core.report.Message;
import de.knowwe.core.report.Messages;
import de.knowwe.kdom.defaultMarkup.DefaultMarkupType;
import de.knowwe.kdom.table.TableCellContent;
import de.knowwe.kdom.table.TableLine;
import de.knowwe.kdom.table.TableUtils;
import de.knowwe.ontology.compile.OntologyCompileScript;
import de.knowwe.ontology.compile.OntologyCompiler;
import de.knowwe.ontology.turtle.Object;
import de.knowwe.ontology.turtle.Predicate;
import de.knowwe.ontology.turtle.compile.NodeProvider;
import de.knowwe.ontology.turtle.compile.StatementProviderResult;
import de.knowwe.rdf2go.Rdf2GoCore;

/**
 * @author Sebastian Furth (denkbares GmbH)
 * @created 27.04.15
 */
public class LineHandler extends OntologyCompileScript<TableLine> {

    @Override
    public void compile(OntologyCompiler compiler, Section<TableLine> section) throws CompilerMessage {

        if (TableUtils.isHeaderRow(section)) {
            Messages.clearMessages(compiler, section, getClass());
            return;
        }

        Rdf2GoCore core = compiler.getRdf2GoCore();
        List<Statement> statements = new LinkedList<>();
        Section<NodeProvider> subjectReference = findSubject(section);
        if (subjectReference == null) {
            // obviously no subject in this line, could be an empty table line
            return;
        }
		@SuppressWarnings("unchecked")
		Value subjectNode = subjectReference.get().getNode(subjectReference, compiler);
		List<Section<Object>> objects = findObjects(section);
        for (Section<Object> objectReference : objects) {
            if (Sections.ancestor(objectReference, TableCellContent.class) != null) {
                Section<Predicate> propertyReference = TableUtils.getColumnHeader(objectReference, Predicate.class);
				if (propertyReference != null) {
					URI propertyUri = (URI) propertyReference.get().getNode(propertyReference, compiler);
					Value objectNode = objectReference.get().getNode(objectReference, compiler);
					statements.add(core.createStatement((Resource) subjectNode, propertyUri, objectNode));
				}
            } else {
				// TODO: clarify whenever this case can make sense...!?
                final StatementProviderResult statementProviderResult = objectReference.get().getStatements(objectReference, compiler);
                statements.addAll(statementProviderResult.getStatements());
            }
        }

		/*
		Additionally handle header cell definition;
		A subject is type of the class specified in the first column header
		*/
		Message typeAnnotationMissing = null;
		Section<TableCellContent> cell = Sections.ancestor(subjectReference, TableCellContent.class);
		Section<TableCellContent> rowHeaderCell = TableUtils.getColumnHeader(cell);
		Section<OntologyTableMarkup.BasicURIType> colHeaderConcept = Sections.successor(rowHeaderCell, OntologyTableMarkup.BasicURIType.class);
		if(colHeaderConcept != null) {
			Section<NodeProvider> nodeProviderSection = Sections.$(colHeaderConcept).successor(NodeProvider.class).getFirst();
			@SuppressWarnings("unchecked")
			Value headerClassResource = nodeProviderSection.get().getNode(nodeProviderSection, compiler);
			Sections<DefaultMarkupType> markup = Sections.$(section).ancestor(DefaultMarkupType.class);
			String typeRelationAnnotationValue = DefaultMarkupType.getAnnotation(markup.getFirst(), OntologyTableMarkup.ANNOTATION_TYPE_RELATION);
			if(typeRelationAnnotationValue != null) {
				org.openrdf.model.URI propertyUri = compiler.getRdf2GoCore().createURI(typeRelationAnnotationValue);
				statements.add(core.createStatement(new URIImpl(subjectNode.stringValue()), propertyUri, headerClassResource));
			} else {
				typeAnnotationMissing = new Message(Message.Type.ERROR, "If subject concepts should be defined as instance of the class given in the first column header, a type-relation has to be defined via the typeRelation-typeRelationAnnotationValue. Otherwise, leave the first cell header blank.");
			}

		}

		core.addStatements(section, statements);

		/*
		Error message handling after committing statements
		 */
		if(typeAnnotationMissing != null) {
			throw new CompilerMessage(typeAnnotationMissing);
		}

    }

    private List<Section<Object>> findObjects(Section<TableLine> section) {
        return Sections.successors(section, Object.class);
    }

    private Section<NodeProvider> findSubject(Section<TableLine> section) {
        final Section<TableCellContent> firstCell = Sections.successor(section, TableCellContent.class);
        return Sections.successor(firstCell, NodeProvider.class);
    }

    @Override
    public void destroy(OntologyCompiler compiler, Section<TableLine> section) {
        Rdf2GoCore core = compiler.getRdf2GoCore();
        core.removeStatements(section);
    }
}
