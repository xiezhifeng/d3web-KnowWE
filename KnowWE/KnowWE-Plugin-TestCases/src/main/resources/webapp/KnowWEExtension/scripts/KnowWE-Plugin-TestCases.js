var TestCasePlayer = {};

TestCasePlayer.init = function() {
	TestCasePlayer.setLastSelected();
	jq$(".type_TestCasePlayer").find(".wikitable").find("th").unbind('click').click(TestCasePlayer.registerClickableColumnHeaders);
}


TestCasePlayer.toggleFindings = function(id, action) {
	jq$('#' + id).find('.wikitable').find('th').filter('[type="finding"]').each(function() {
		var collapse = action == "collapse";
		var th = jq$(this);
		if (collapse && !th.hasClass("collapsedcolumn")) {
			TestCasePlayer.collapseColumn(th);
		} else if (!collapse && th.hasClass("collapsedcolumn")) {
			TestCasePlayer.expandColumn(th);
		}
	});
}
	
TestCasePlayer.registerClickableColumnHeaders = function() {

	TestCasePlayer.toggelColumnStatus(jq$(this));
}

TestCasePlayer.toggelColumnStatus = function(th) {

	var isCollapsed = th.hasClass("collapsedcolumn");
	
	if (isCollapsed) {
		TestCasePlayer.expandColumn(th, true);
	} else {
		TestCasePlayer.collapseColumn(th, true);
	}
	
}

TestCasePlayer.getCollapseStatus = function(th) {
	var collapsed = "";
	th.siblings().each(function() {
		if (jq$(this).hasClass("collapsedcolumn")) {
			collapsed += jq$(this).attr("column") + "#";
		}
	});
	return collapsed;
}

TestCasePlayer.writeCollapseStatus = function(th, collapsed) {
	var id = th.parents(".TestCasePlayerContent").attr("id");
	var testCase = jq$("#selector" + id).find('[selected="selected"]').attr("value");
	if (!testCase) testCase = jq$("#selector" + id).val();
	testCase = TestCasePlayer.encodeCookieValue(testCase);
	document.cookie = "columnstatus_" + id + "_" + testCase + "=" + collapsed;
}

TestCasePlayer.collapseColumn = function(th, animated) {
	if (th.find("input").length > 0) return;

	var column = th.attr("column");
	
	var tds = th.parents(".wikitable").first().find('[column="' + column + '"]');
	
	var collapsed = TestCasePlayer.getCollapseStatus(th);
	collapsed += column;
	TestCasePlayer.writeCollapseStatus(th, collapsed);
	
	if (tds.length < 12 && animated) {	
		// for performance reasons
		tds.addClass("collapsedcolumn", 150, "linear");
	} else {
		tds.addClass("collapsedcolumn");
	}
	th.attr("title", "Expand " + th.text());
	tds.filter("td").each(function() {
		jq$(this).attr("title", jq$(this).text());
	});
	
}

TestCasePlayer.expandColumn = function(th, animated) {
	if (th.find("input").length > 0) return;

	var column = th.attr("column");
	
	var tds = th.parents(".wikitable").first().find('[column="' + column + '"]');

	if (tds.length < 12 && animated) {			
		tds.removeClass("collapsedcolumn", 150, "linear");
	} else {
		tds.removeClass("collapsedcolumn");
	}
	th.attr("title", "Collapse");
	tds.filter("td").each(function() {
		jq$(this).removeAttr("title");
	});
	
	var collapsed = TestCasePlayer.getCollapseStatus(th);
	
	TestCasePlayer.writeCollapseStatus(th, collapsed);
	
	
}

TestCasePlayer.send = function(sessionid, casedate, name, topic) {
			
            var params = {
        		action : 'ExecuteCasesAction',
       			KWiki_Topic : topic,
       			id : sessionid,
        		date : casedate,
        		testCaseName : name
    		}
            
            var options = {
                url : KNOWWE.core.util.getURL(params),
                response : {
                	action : 'none',
                	fn : function(){
			        	try {
			               	TestCasePlayer.update();
			        	}
			        	catch (e) { /*ignore*/ }
			        	KNOWWE.core.util.updateProcessingState(-1);
                	},
                    onError : function () {
			        	KNOWWE.core.util.updateProcessingState(-1);
			        	if (this.status == null) return;
        	switch (this.status) {
        	  case 0:
        		// server not running, do nothing.
        		break;
        	  case 409:
          	    alert("The section has changed since you " 
          	    		+ "loaded this page. Please reload the page.");
        	    break;
        	  default:
        	    alert("Error " + this.status + ". Please reload the page.");
        	    break;
        	}                	
                    }
                }
            }
        	KNOWWE.core.util.updateProcessingState(1);
            new _KA( options ).send();         
        }

TestCasePlayer.lastSelected = new Object();

TestCasePlayer.setLastSelected = function() {
	jq$('.type_TestCasePlayer').find(".ReRenderSectionMarker").each(function() {
		var id = jq$(this).children().first().attr('id');
		var selected = jq$('#selector' + id).val();
		TestCasePlayer.lastSelected[id] = selected;
	});
}

TestCasePlayer.change = function(key_sessionid, selectedvalue) {
 			var topic = KNOWWE.helper.gup('page');
			document.cookie = key_sessionid +"=" + TestCasePlayer.encodeCookieValue(selectedvalue);
            //KNOWWE.helper.observer.notify('update');
           	TestCasePlayer.update();
}

TestCasePlayer.addCookie = function(cookievalue)  {
			var topic = KNOWWE.helper.gup('page');
			document.cookie = "additionalQuestions"+ TestCasePlayer.encodeCookieValue(topic) +"=" + TestCasePlayer.encodeCookieValue(cookievalue);
           	//KNOWWE.helper.observer.notify('update');
           	TestCasePlayer.update();
}

TestCasePlayer.encodeCookieValue = function(cookievalue) {
			var temp = escape(cookievalue);
			temp = temp.replace('@', '%40');
			temp = temp.replace('+', '%2B');
			return temp;
}

TestCasePlayer.update = function() {
	var scrollInfos = new Object();
	jq$('.type_TestCasePlayer').find(".ReRenderSectionMarker").each(function() {
		var id = jq$(this).children().first().attr('id');
		var selected = jq$('#selector' + id).val();
		var scrollInfo = new Object();
        if (selected != TestCasePlayer.lastSelected[id]) {
        	TestCasePlayer.lastSelected[id] = selected;
        } else {
			var tableDiv = jq$("#" + id).find('.' + "wikitable").parent();
			scrollInfo.left = tableDiv.scrollLeft();
			scrollInfo.width = tableDiv[0].scrollWidth;
			scrollInfo.restoreScroll = true;
        }
    	scrollInfos[id] = scrollInfo;
	});
	
    var fn = function() {
    	for (var id in scrollInfos) {
    		var scrollInfo = scrollInfos[id];
    		if (scrollInfo.restoreScroll) {
    			var tableDiv = jq$("#" + id).find('.' + "wikitable").parent();
		        var scrollWidthAfter = tableDiv[0].scrollWidth;
		        if (scrollInfo.width < scrollWidthAfter) {
		        	scrollInfo.left += scrollWidthAfter - scrollInfo.width;
		        }       	
		        tableDiv.scrollLeft(scrollInfo.left);
    		}
	        
    	}
    	jq$(".type_TestCasePlayer").find(".wikitable").find("th").unbind('click').click(TestCasePlayer.registerClickableColumnHeaders);
    }
	KNOWWE.helper.observer.notify('update', fn);
}

if (!KNOWWE.plugin.testCases) KNOWWE.plugin.testCases = {};
if (!KNOWWE.plugin.testCases.testCaseTable) KNOWWE.plugin.testCases.testCaseTable = {};

jq$(document).ready(function() {
	// Prepare for instant table editor with custom auto-complete 
	KNOWWE.plugin.testCases.testCaseTable.editTool = KNOWWE.plugin.tableEditTool.create(
			function (callback, prefix, spreadsheet, row, col) {
				var ajaxFun, ajaxPrefix = prefix;
				var otherItems = [];
				// prepare object name
				var colName = spreadsheet.getCellTextTrimmed(0, col);
				// prepare ajax
				if (row == 0) {
					ajaxFun = AutoComplete.sendD3webValueObjectCompletionAction;
					if (prefix.match(/^\s*"?(t(i(m(e)?)?)?)?"?\s*$/i))
						otherItems.push({
							insertText: "Time",
							replaceLength: prefix.length,
							description: "Column for entering the reasoning time of the specific row."
						});
					if (prefix.match(/^\s*"?(c(h(e(c(k(s)?)?)?)?)?)?"?\s*$/i))
						otherItems.push({
							insertText: "Checks", 
							replaceLength: prefix.length,
							description: "Column for entering some conditions. The condition must be true after executing the row. Otherwise the test case fails."
						});
				}
				else if (colName === 'Time') {
					ajaxFun = function(callback, prefix) {callback([]);};
				}
				else if (colName === 'Checks') {
					ajaxFun = AutoComplete.sendD3webConditionCompletionAction;
				}
				else {
					colName = AutoComplete.unquoteTermIdentifier(colName);
					if (AutoComplete.termRequiresQuotes(colName)) colName = '"' +colName + '"';
					ajaxFun = AutoComplete.sendD3webActionCompletionAction;
					ajaxPrefix = colName + ' = '+prefix;
				}
				ajaxFun(function(byAjax) {
					AutoComplete.unquoteTermIdentifiers(byAjax);
					callback(otherItems.concat(byAjax));
				}, ajaxPrefix);
			});
	
	// init test case player
	TestCasePlayer.init();
});


