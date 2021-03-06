/*
 * Copyright (C) 2014 denkbares GmbH, Germany
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

/**
 * The KNOWWE global namespace object. If KNOWWE is already defined, the
 * existing KNOWWE object will not be overwritten so that defined namespaces are
 * preserved.
 */
if (typeof KNOWWE == "undefined" || !KNOWWE) {
    var KNOWWE = {};
}

/**
 * The KNOWWE.core global namespace object. If KNOWWE.core is already defined,
 * the existing KNOWWE.core object will not be overwritten so that defined
 * namespaces are preserved.
 */
if (typeof KNOWWE.plugin == "undefined" || !KNOWWE.plugin) {
    KNOWWE.plugin = {};
}


/**
 * Namespace: KNOWWE.core.plugin.instantedit The KNOWWE instant edit namespace.
 */
KNOWWE.plugin.instantEdit = function() {
 
    function enabledWarning() {
        if (_IE.enabled) {
            alert("You can only edit the page once at a time.")
        }
    }
    
    function bindUnloadFunctions(id) {
        window.onbeforeunload = (function() {
            var toolsUnloadCondition = _IE.toolNameSpace[id].unloadCondition;
            if (toolsUnloadCondition != null && !toolsUnloadCondition(id)) {
                return "edit.areyousure".localize();
            }
        }).bind(this);

        window.onunload = (function() {
            _IE.disable(id, false, null);
        }).bind(this);
	}

    return {

        toolNameSpace: {},

        enabled: false,
        
        enable: function(id, toolNameSpace) {

            if (_IE.enabled) {
                enabledWarning();
                return;
            }
			if (KNOWWE.helper.gup('version')) {
				alert("Unable to edit while restoring versions.");
				return;
			}

            _EC.showAjaxLoader(id);
            
            _EC.mode = _IE;

            _IE.toolNameSpace[id] = toolNameSpace;

            var params = {
                action: 'InstantEditEnableAction',
                KdomNodeId: id,
                mode: toolNameSpace.mode
            };

            var options = {
                url: KNOWWE.core.util.getURL(params),
                response: {
                    action: 'none',
                    fn: function() {
                        if (_IE.mutualExclusion && _IE.enabled) {
                            enabledWarning();
                            return;
                        }
                        _IE.enabled = true;

						var json = JSON.parse(this.responseText);
						var locked = json.locked;
						var html = toolNameSpace.generateHTML(id);
						if (toolNameSpace.getChangeNoteField) {
							html += toolNameSpace.getChangeNoteField();
						} else{
							html += _IE.getChangeNoteField();
						}
                        html += toolNameSpace.generateButtons(id);
                        html = _EC.wrapHTML(id, locked, html);

                        KNOWWE.core.util.replace(html);

                        toolNameSpace.postProcessHTML(id);
                        bindUnloadFunctions(id);
                        _EC.hideTools();

						var save = toolNameSpace.save ? toolNameSpace.save : _IE.save;
						var cancel = toolNameSpace.cancel ? toolNameSpace.cancel : _IE.cancel;

                        _EC.registerSaveCancelEvents(jq$('#' + id), save, cancel, id);
                    },
					onError: _EC.onErrorBehavior
				}
            };
            new _KA(options).send();

            _EC.hideAjaxLoader();
        },


        disable: function(id, reload, fn) {
        	if (!fn) fn = false; // prevents JS error in KnowWE helper
            var params = {
                action: 'InstantEditDisableAction',
                KdomNodeId: id
            };

            var options = {
                url: KNOWWE.core.util.getURL(params),
                async: false,
                response: {
                    action: 'none',
                    onError: _EC.onErrorBehavior,
                    fn: fn
                }
            };
            new _KA(options).send();

            if (reload) {
				KNOWWE.core.util.reloadPage();
			}
		},
        
        /**
		 * Save the changes to the article.
		 * 
		 * @param id is the id of the DOM element
		 * @param newWikiText is the optional new text for the section with the given id
		 */
        save: function(id, newWikiText) {

            if (newWikiText == null) {
                newWikiText = _IE.toolNameSpace[id].generateWikiText(id);
            }
            
            var params = {
                action: 'InstantEditSaveAction',
                KdomNodeId: id,
                KWikiChangeNote: _EC.mode.getChangeNote(id)
            };

            _EC.sendChanges(newWikiText, params, function(id) {
                _IE.disable(id, true, null);
            });
        },

        /**
		 * Adds a new article with the given articleText. The title of the new
		 * article is given by the current tools function getNewArticleTitle();
		 * 
		 * @param id is the id of the source DOM element
		 * @param title is the optional title of the added article
		 * @param newWikiText is the optional new text of the new article
         * @param redirect boolean, whether the user should be redirected to the newly created page after saving.
		 * @param fn the callback called after saving...
		 */
        add: function(id, title, newWikiText, redirect, fn) {

            _EC.showAjaxLoader(id);

            if (newWikiText == null) {
                newWikiText = _IE.toolNameSpace[id].generateWikiText(id);
            }
            if (title == null) {
                title = _IE.toolNameSpace[id].getNewArticleTitle(id);
            }

            var params = {
                action: 'InstantEditAddArticleAction',
                KdomNodeId: id,
                KWiki_Topic: title
            };

            _EC.sendChanges(newWikiText, params, function(id) {
                if (fn) fn();
                _IE.disable(id, !redirect, null);
                if (redirect) {
                    window.location.search = "?page=" + title;
                }
            });

        },


        /**
		 * Cancel the instant edit action. Restore the original text.
		 * 
		 * @param id is the id of the DOM element
		 */
        cancel: function(id) {
        	_IE.toolNameSpace[id].unloadCondition = null;
            _IE.disable(id, true, null);
        },

        deleteSection: function(id) {
            var del = confirm("Do you really want to delete this content?");
            if (del) {
                _IE.save(id, "");
            }
        },
        
        deleteArticle : function(id) {
        	var del = confirm("Do you really want to delete this content?");
        	if (del) {
        		var title = _IE.toolNameSpace[id].getCurrentArticleTitle(id); 	
        		_IE.add(id, title, "");
        	}
        }, 

        disableDefaultEditTool: function() {
            jq$('div.InstantEditTool').css("display", "none");
        },

        enableDefaultEditTool: function() {
            jq$('div.InstantEditTool').css("display", null);
        }, 
        
        getSaveCancelDeleteButtons: function(id, additionalButtonArray) {        
            var array = [];
            array.push(_EC.elements.getSaveButton("_IE.save('" + id + "')"));
            array.push(_EC.elements.getCancelButton("_IE.cancel('" + id + "')"));
            array.push("&nbsp;");
            array.push(_EC.elements.getDeleteSectionButton("_IE.deleteSection('" + id + "')"));
            
            // add additional buttons
            if (additionalButtonArray) {
                array.push("       ");
                array = array.concat(additionalButtonArray);
            }
            return array;
        },
        
        getButtonsTable: function(buttons) {
            var table = "";
            for (var i = 0; i < buttons.length; i++) {
                table += "<p>" + buttons[i] + "</p>\n";
            }
            return "<div class='editbuttons'>" + table + "</div>";
        },
        
        getChangeNoteField: function(){
			return "<div class='changenotearea'><span>Change note: </span><input class='changenote' type='text' maxLength=100 size=100></div>";
		},
        
        getChangeNote: function(id) {
			return jq$('#' + id + ' .changenote').val();
        }
        
    }
}();



/**
 * Alias for some to reduce typing.
 */
var _IE = KNOWWE.plugin.instantEdit;

jq$(document).ready(function() {
	_EC.executeIfPrivileged(_IE.enableDefaultEditTool, _IE.disableDefaultEditTool);
});

