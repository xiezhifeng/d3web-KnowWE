/**
 * The KNOWWE global namespace object. If KNOWWE is already defined, the
 * existing KNOWWE object will not be overwritten so that defined namespaces are
 * preserved.
 */
if (typeof KNOWWE == "undefined" || !KNOWWE) {
	var KNOWWE = {};
}

var toSelect;
/**
 * The KNOWWE.plugin global namespace object. If KNOWWE.plugin is already
 * defined, the existing KNOWWE.plugin object will not be overwritten so that
 * defined namespaces are preserved.
 */
if (typeof KNOWWE.plugin == "undefined" || !KNOWWE.plugin) {
	KNOWWE.plugin = function() {
		return {}
	}
}

/**
 * The KNOWWE.plugin.ontology global namespace object. If KNOWWE.plugin.ontology
 * is already defined, the existing KNOWWE.plugin.ontology object will not be
 * overwritten so that defined namespaces are preserved.
 */
KNOWWE.plugin.ontology = function() {
	return {
		commitOntology : function(sectionID) {
			new _KA({
				url : KNOWWE.core.util.getURL({
					action : 'CommitOntologyAction',
					SectionID : sectionID
				}),
				fn : function() {
					KNOWWE.core.util.hideProcessingIndicator();
					location.reload();
				}
			}).send();
			KNOWWE.core.util.showProcessingIndicator();
		},
		expandLazyReference : function(sectionID, newReferenceText, rerenderID) {
			var params = {
				action : 'ReplaceKDOMNodeAction',
				TargetNamespace : sectionID,
				KWikitext : newReferenceText,
			};
			var options = {
				url : KNOWWE.core.util.getURL(params),
				response : {
					fn : function() {
						// todo: rerender markup block
						location.reload();
					}
				}
			}
			new _KA(options).send();
		}
	}
}();


KNOWWE.plugin.sparql.editTool = {};
jq$.extend(KNOWWE.plugin.sparql.editTool, KNOWWE.plugin.defaultEditTool);

KNOWWE.plugin.sparql.editTool.generateButtons = function(id) {
	KNOWWE.plugin.sparql.editTool.format = function() {
		KNOWWE.core.plugin.formatterAjax(id, "SparqlFormatAction");
	};
	return _EC.elements.getSaveCancelDeleteButtons(id,
		["<a class='action format' onclick='KNOWWE.plugin.sparql.editTool.format(\"" + id + "\")'>" +
		"Format</a>"]);
};

KNOWWE.plugin.turtle = {};
KNOWWE.plugin.turtle.editTool = {};
jq$.extend(KNOWWE.plugin.turtle.editTool, KNOWWE.plugin.defaultEditTool);

KNOWWE.plugin.turtle.editTool.generateButtons = function(id) {
	KNOWWE.plugin.turtle.editTool.format = function() {
		KNOWWE.core.plugin.formatterAjax(id, "TurtleFormatAction");
	};
	return _EC.elements.getSaveCancelDeleteButtons(id,
		["<a class='action format' onclick='KNOWWE.plugin.turtle.editTool.format(\"" + id + "\")'>" +
		"Format</a>"]);
};
