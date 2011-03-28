if (!FlowEditor){
	FlowEditor = {};
}

function FlowEditor(articleIDs){
	
	// kbinfo initialization
	KBInfo._updateCache($('ajaxKBInfo'));
	KBInfo._updateCache($('articleKBInfo'));
	KBInfo._updateCache($('referredKBInfo'));
	

	// initialize wiki tree tool
	new ObjectTree('objectTree', null, articleIDs);

	theFlowchart = null;
	
}

FlowEditor.prototype.showEditor = function(){
	theFlowchart = Flowchart.createFromXML('contents', $('flowchartSource'));
	theFlowchart.setVisible(true);
	$('properties.editName').value = theFlowchart.name || theFlowchart.id;
	$('properties.editWidth').value = theFlowchart.width;
	$('properties.editHeight').value = theFlowchart.height;
	$('properties.autostart').checked = theFlowchart.autostart;
	$('properties.editName').onchange = this.updateProperties;
	$('properties.editWidth').onchange = this.updateProperties;
	$('properties.editHeight').onchange = this.updateProperties;
	$('properties.autostart').onchange = this.updateProperties;
	
	$('saveClose').observe('click', function(){this.saveFlowchart(true);}.bind(this));
	$('save').observe('click', function(){this.saveFlowchart(false);}.bind(this));
	$('refresh').observe('click', this.revert);
	
	$('close').observe('click', this.closeEditor);
	$('delete').observe('click', this.deleteFlowchart);
	
	$('arrowLeftIncrease').observe('click', function(){FlowEditor.increaseSize('left');});
	$('arrowRightIncrease').observe('click', function(){FlowEditor.increaseSize('right');});
	$('arrowTopIncrease').observe('click', function(){FlowEditor.increaseSize('top');});
	$('arrowBottomIncrease').observe('click', function(){FlowEditor.increaseSize('bottom');});
	
	$('arrowLeftDecrease').observe('click', function(){FlowEditor.decreaseSize('left');});
	$('arrowRightDecrease').observe('click', function(){FlowEditor.decreaseSize('right');});
	$('arrowTopDecrease').observe('click', function(){FlowEditor.decreaseSize('top');});
	$('arrowBottomDecrease').observe('click', function(){FlowEditor.decreaseSize('bottom');});
		
	theFlowchart.getContentPane().observe('mousedown', function(event) {FlowEditor.massSelectDown(event);})
	theFlowchart.getContentPane().observe('mouseup', function(event) {FlowEditor.massSelectUp(event);})
	theFlowchart.getContentPane().observe('mousemove', function(event) {FlowEditor.massSelectMove(event);})
	
	var dragOptions = { ghosting: true, revert: true, reverteffect: ObjectTree.revertEffect};

	new Draggable('decision_prototype', dragOptions);
	new Draggable('start_prototype', dragOptions);
	new Draggable('exit_prototype', dragOptions);
	new Draggable('comment_prototype', dragOptions);
	new Draggable('snapshot_prototype', dragOptions);
	
	$('decision_prototype').createNode = function(flowchart, left, top) { FlowEditor.createActionNode(flowchart, left, top, {action: { markup: 'KnOffice', expression: ''}}); };
	$('start_prototype').createNode = function(flowchart, left, top) { FlowEditor.createActionNode(flowchart, left, top, {start: 'Start'}); };
	$('exit_prototype').createNode = function(flowchart, left, top) { FlowEditor.createActionNode(flowchart, left, top, {exit: 'Exit'}); };
	$('comment_prototype').createNode = function(flowchart, left, top) { FlowEditor.createActionNode(flowchart, left, top, {comment: 'Comment'}); };
	$('snapshot_prototype').createNode = function(flowchart, left, top) { FlowEditor.createActionNode(flowchart, left, top, {snapshot: 'Snapshot'}); };
	
	
	
}

// increase the size of the flowchart via arrows at the top, bottom, left or right
FlowEditor.increaseSize = function(direction) {
	var height = parseInt(theFlowchart.height);
	var width = parseInt(theFlowchart.width);

	if (direction === 'bottom') {
		theFlowchart.setSize(width, height + 100);
	} else if (direction === 'top') {
		theFlowchart.setSize(width, height + 100);
		for (var i = 0; i < theFlowchart.nodes.length; i++) {
			theFlowchart.nodes[i].moveBy(0, 100);
		}
	} else if (direction === 'right') {
		theFlowchart.setSize(width + 100, height);
	} else if (direction === 'left') {
		theFlowchart.setSize(width + 100, height);
		for (var i = 0; i < theFlowchart.nodes.length; i++) {
			theFlowchart.nodes[i].moveBy(100, 0);
		}
	}
	FlowEditor.rearrangeArrows()
}

FlowEditor.decreaseSize = function(direction) {
	var height = parseInt(theFlowchart.height);
	var width = parseInt(theFlowchart.width);

	var max = theFlowchart.getMaxObjects();
	var min = theFlowchart.getMinObjects();
	var dif, change;
	
	if (direction === 'bottom') {
		dif = height - max[1];
		change = Math.min(100, dif);
		theFlowchart.setSize(width, (height - change));
	} else if (direction === 'right') {
		dif = height - max[0];
		change = Math.min(100, dif);
		theFlowchart.setSize((width - change), height);
	} else if (direction === 'left') {
		dif = min[0] - 40;
		change = Math.min(100, dif);
		for ( var i = 0; i < theFlowchart.nodes.length; i++) {
			theFlowchart.nodes[i].moveBy(-change, 0);
		}
		theFlowchart.setSize((width - change), height);

	} else if (direction === 'top') {
		dif = min[1] - 40;
		change = Math.min(100, dif);
		for ( var i = 0; i < theFlowchart.nodes.length; i++) {
			theFlowchart.nodes[i].moveBy(0, -change);
		}
		theFlowchart.setSize(width, (height - change));
	}
	FlowEditor.rearrangeArrows()

}

FlowEditor.rearrangeArrows = function() {
	var leftContainer = $('arrowLeftIncrease').parentNode.parentNode;
	var rightContainer = $('arrowRightIncrease').parentNode.parentNode;
	var upContainer = $('arrowTopIncrease').parentNode.parentNode;
	var downContainer = $('arrowBottomIncrease').parentNode.parentNode;
	
	var width = theFlowchart.width;
	var height = theFlowchart.height;
	var xMid = theFlowchart.width / 2;
	var yMid = theFlowchart.height / 2;
	
	leftContainer.style.left = '1px';
	leftContainer.style.top = (yMid - 50) + 'px';
	rightContainer.style.left = (width - 100) + 'px';
	rightContainer.style.top = (yMid -50) + 'px';
	upContainer.style.left = (xMid - 50) + 'px';
	upContainer.style.top = '5px;'
	downContainer.style.left = (xMid - 50) + 'px';
	downContainer.style.top = (height - 100) + 'px';
}


FlowEditor.massSelectDown = function(event) {
	FlowEditor.SelectX = event.layerX;
	FlowEditor.SelectY = event.layerY;
	FlowEditor.moveStarted = true;


	event.stop();
}

FlowEditor.massSelectMove = function(event) {
	var selectTool = $('select_tool');
	if (selectTool) {
		selectTool.parentNode.removeChild(selectTool);
	}
	if (FlowEditor.moveStarted) {
		var startX = FlowEditor.SelectX;
		var startY = FlowEditor.SelectY;
		var endX = event.layerX;
		var endY = event.layerY;

		var lineDOM = SelectTool.createSelectionBox(startX, startY, endX, endY, 1, 'grey', 0, 1000);
		theFlowchart.getContentPane().appendChild(lineDOM);
		
		
		var newSelection = [];
		for ( var i = 0; i < theFlowchart.nodes.length; i++) {
			if (theFlowchart.nodes[i].intersects(startX, startY, endX, endY)) {
				newSelection.push(theFlowchart.nodes[i]);
			}
		}
		var current = theFlowchart.selection;
		var currentSelection = current[0];
		if ((currentSelection && !currentSelection.guardEditor) || newSelection.size() !== 0) {
			theFlowchart.setSelection(newSelection, false, false);
		} 
	}
}

FlowEditor.massSelectUp = function(event) {
	var startX = FlowEditor.SelectX;
	var startY = FlowEditor.SelectY;
	var endX = event.layerX;
	var endY = event.layerY;
	
	
	var newSelection = [];
	for ( var i = 0; i < theFlowchart.nodes.length; i++) {
		if (theFlowchart.nodes[i].intersects(startX, startY, endX, endY)) {
			newSelection.push(theFlowchart.nodes[i]);
		}
	}
	var current = theFlowchart.selection;
	var currentSelection = current[0];
	if ((currentSelection && !currentSelection.guardEditor) || newSelection.size() !== 0) {
		theFlowchart.setSelection(newSelection, false, false);
	} 
	FlowEditor.moveStarted = false;
	var selectTool = $('select_tool');
	if (selectTool) {
		selectTool.parentNode.removeChild(selectTool);
	}
}

FlowEditor.createActionNode = function(flowchart, left, top, nodeModel) {
	nodeModel.position = {left: left, top: top};
	var node = new Node(flowchart, nodeModel);
	node.select();
	node.edit();
};

FlowEditor.prototype.updateProperties = function(){
	theFlowchart.name = $('properties.editName').value;
	theFlowchart.setSize($('properties.editWidth').value, $('properties.editHeight').value);
	theFlowchart.autostart = $('properties.autostart').checked;
}

FlowEditor.prototype.revert = function(){
	var result = confirm('Do you really want to revert your changes?');
	if (result) {
		window.location.reload();
	}
}

FlowEditor.prototype.closeEditor = function(){
	window.close();
}

FlowEditor.prototype.deleteFlowchart = function() {
	var result = confirm('Do you really want to delete the flowchart?');
	if (result) {
		this._saveFlowchartText('', true);
	}
}

FlowEditor.prototype.saveFlowchart = function(closeOnSuccess) {
	theFlowchart.setSelection(null, false, false);
	var xml = theFlowchart.toXML(true); // include preview for us
	this._saveFlowchartText(xml, closeOnSuccess);
}

// Overrides empty implementation in flowchart.js
Flowchart.prototype.createDroppables = function(dom, contentPane, trashPane) {
	// initialize trash to delete nodes and rules
	Droppables.add(trashPane, { 
		accept: ['Node', 'Rule'],
		hoverclass: 'trash_hover',
		onDrop: function(draggable, droppable, event) {
			if (draggable.__node) draggable.__node.destroy();
			if (draggable.__rule) draggable.__rule.destroy();
		}
	});

	// initialite drag from trees to the pane
	Droppables.add(dom, { 
		accept: ['NodePrototype'],
		hoverclass: 'FlowchartGroup_hover',
		onDrop: function(draggable, droppable, event) {
			var p1 = draggable.cumulativeOffset();
			var p2 = contentPane.cumulativeOffset();
			var x = p1.left - p2.left;
			var y = p1.top  - p2.top;
			draggable.createNode(dom.__flowchart, x, y); // dom.__flowchart is defined above
		}
	});
}



FlowEditor.prototype._saveFlowchartText = function(xml, closeOnSuccess) {
	var url = "KnowWE.jsp";
	new Ajax.Request(url, {
		method: 'post',
		parameters: {
			action: 'SaveFlowchartAction',
			KWiki_Topic: topic,			// article
			TargetNamespace: nodeID,	// KDOM nodeID
			KWikitext: xml				// content
		},
		onSuccess: function(transport) {
			if (window.opener) window.opener.location.reload();
			if (closeOnSuccess) window.close();
		},
		onFailure: function() {
			CCMessage.warn(
				'AJAX Error', 
				'Changes could not be saved.');
		},
		onException: function(transport, exception) {
			CCMessage.warn(
				'AJAX Error, Saving most likely failed.',
				exception
				);
		}
	}); 		

	
}





//Flowchart event handlers
//register select click events for flowchart
//CCEvents.addClassListener('click', 'FlowchartGroup', 
//	function(event) {
//		this.__flowchart.setSelection(null);
//	}
//);
CCEvents.addClassListener('keydown', 'FlowchartGroup', 
	function(event) {
		this.__flowchart.handleKeyEvent(event);
	}
);

Flowchart.prototype.handleKeyEvent = function(event) {
	var isHandled = false;
	var ctrlKey = event.ctrlKey;
	switch (event.keyCode) {
		case 65: // [ctrl] + a
			if (ctrlKey) {
				this.setSelection(this.nodes, false);
				//this.setSelection(this.rules, true);
				this.focus();
				isHandled = true;
			}
			break;
		case 88: // [ctrl] + x
			if (ctrlKey) {
				this.copySelectionToClipboard();
				this.trashSelection();
				isHandled = true;
			}
			break;
		case 67: // [ctrl] + c
			if (ctrlKey) {
				this.copySelectionToClipboard();
				isHandled = true;
			}
			break;
		case 86: // [ctrl] + v
			if (ctrlKey) {
				this.pasteFromClipboard();
				isHandled = true;
			}
			break;
		case Event.KEY_LEFT:
			isHandled = this.moveSelection(-10, 0);
			break;
		case Event.KEY_RIGHT:
			isHandled = this.moveSelection(+10, 0);
			break;
		case Event.KEY_UP:
			isHandled = this.moveSelection(0, -10);
			break;
		case Event.KEY_DOWN:
			isHandled = this.moveSelection(0, +10);
			break;
		case Event.KEY_DELETE:
		case Event.KEY_BACKSPACE:
			this.trashSelection();
			isHandled = true;
			break;
	}
	// only changes nodes using keys
	if (!isHandled) event.nextHandler();
}

Flowchart.prototype.copySelectionToClipboard = function() {
	var sel = this.selection.clone();
	// add all rinterconnecting rules to clipboard as well
	for (var i=0; i<this.rules.length; i++) {
		var rule = this.rules[i];
		if (sel.contains(rule)) continue;
		if (sel.contains(rule.getSourceNode()) || sel.contains(rule.getTargetNode())) {
			sel.push(rule);
		}
	}
	var result = '';
	for (var i=0; i<sel.length; i++) {
		result += sel[i].toXML();
	}
	CCClipboard.toClipboard(result);
	this.focus();
}

Flowchart.prototype.pasteFromClipboard = function() {
	//alert("paste not implemented yet:\n\n"+CCClipboard.fromClipboard());
	var xmlDom = CCClipboard.fromClipboard().parseXML();
	this.addFromXML(xmlDom, 20, 20);
}

Flowchart.prototype.trashSelection = function() {
	var sel = this.selection.clone();
	for (var i=0; i<sel.length; i++) {
		var item = sel[i];
		item.destroy();
	}
	this.focus();
}

Flowchart.prototype.moveSelection = function(dx, dy) {
	var isHandled = false;
	for (var i=0; i<this.selection.length; i++) {
		var item = this.selection[i];
		if (item.moveBy) {
			item.moveBy(dx, dy);
			isHandled = true;
		}
	}
	this.focus();
	return isHandled;
}


Flowchart.prototype.getHSnaps = function() {
	var result = [];
	for (var i=0; i<this.nodes.length; i++) {
		result.push(this.nodes[i].x + this.nodes[i].getWidth()/2);
	}
	return result;
}

Flowchart.prototype.getVSnaps = function() {
	var result = [];
	for (var i=0; i<this.nodes.length; i++) {
		result.push(this.nodes[i].y + this.nodes[i].getHeight()/2);
	}
	return result;
}


/**
 * Flowchart.removeFromSelection
 * Removes the specified Node(s) or Rule(s) from the current selection. 
 * This method signals the deselected elements to be de-highlighted.
 * 
 * @param {Node | Rule | [Node, ..., Rule, ...]} nodeOrRuleOrArray 
 */
Flowchart.prototype.removeFromSelection = function(nodeOrRuleOrArray) {
	// create new and defined selection array 
	// for the items to be selected
	var items;
	if (nodeOrRuleOrArray == null) {
		items = [];
	}
	else if (DiaFluxUtils.isArray(nodeOrRuleOrArray)) {
		items = nodeOrRuleOrArray;
	}
	else {
		items = [nodeOrRuleOrArray];
	}
	
	for (var i=0; i<items.length; i++) {
		items[i].setSelectionVisible(false);
		this.selection.remove(items[i]);
	}
}


//TODO remove??
Flowchart.prototype.selectAt = function(x, y, addToSelection) {
	for (var i=0; i<this.nodes.length; i++) {
		if (this.nodes[i].intersects(x, y, x, y)) {
			this.setSelection(this.nodes[i], addToSelection);
			return;
		}
	}
	for (var i=0; i<this.rules.length; i++) {
		if (this.nodes[i].intersects(x-3, y-3, x+3, y+3)) {
			this.setSelection(this.nodes[i], addToSelection);
			return;
		}
	}
	if (!addToSelection) {
		setSelection([], false);
	}
}

Flowchart.prototype.toXML = function(includePreview) {
	var xml = '<flowchart' +
			' fcid="'+this.id+'"' +
			(this.name ? ' name="'+this.name.escapeXML()+'"' : '') +
			(this.icon ?' icon="'+this.icon+'"' : '')  +
			' width="'+this.width+'"' +
			' height="'+this.height+'"' +
			' autostart="'+this.autostart+'"' +
			' idCounter="'+this.idCounter+'">\n\n';
	
	xml += '\t<!-- nodes of the flowchart -->\n';
	for (var i=0; i<this.nodes.length; i++) {
		this.nodes[i].stopEdit();
		xml += this.nodes[i].toXML() + '\n';
	}
	
	xml += '\n';
	xml += '\t<!-- rules of the flowchart -->\n';
	for (var i=0; i<this.rules.length; i++) {
		xml += this.rules[i].toXML() + '\n';
	}

	if (this.dom && includePreview) {
		xml += '\t<preview mimetype="text/html">\n' +
				'\t\t<![CDATA[\n' +
				this.toPreviewHTML(this.dom.select('.Flowchart')[0]) + '\n' +
				'\t\t]]>\n' +
				'\t</preview>\n';
	}

	xml += '</flowchart>'
	return xml;
}

Flowchart.prototype.toPreviewHTML = function(node) { 
	//return node.innerHTML;
	if (node.nodeName == '#text') return node.data;
	if (node.style.display == 'none') return '';
	if (node.style.visibility == 'hidden') return '';
	var size = Element.getDimensions(node);
	var html = '<' + node.nodeName; 
	if (node.className)	html += ' class="' + node.className + '"';
	if (node.id) html += ' id="' + node.id + '"';
	if (node.src){
		var srcVal = node.src;

		if (node.src.startsWith('http://')){
		
			
			var path = self.location.href;
			var pos = path.indexOf('.jsp');
			path = path.substring(0,pos);
			
			pos = path.lastIndexOf('/') + 1;
			path = path.substring(0,pos);
		
			srcVal = node.src.substring(path.length, srcVal.length);
		}
		
		html += ' src="' + srcVal + '"';
	}
	// for nodes we have a problem: padding is not taken nto consideration
	// therefore allow width+padding not to be more than parent's width - 2

	if (node.parentNode.hasClassName('Node') && (node.hasClassName('flowchart') || node.hasClassName('action') || node.hasClassName('question') ||  node.hasClassName('decision'))) {
		size.width = Element.getWidth(node.parentNode)-13;
		size.height += 2; 
	}
	if (node.parentNode.hasClassName('Node') && (node.hasClassName('start') || node.hasClassName('exit') || node.hasClassName('comment') || node.hasClassName('snapshot'))) {
		size.width = Element.getWidth(node.parentNode)-13;
		size.height += 2; 
			
	}
	// for nodes we want to have the fixed size in addition to the style
	var attributes = ['position', 'display', 'visibility', 'left', 'right', 'top', 'bottom', 'overflow'];
	var style = ''
	for (var i=0; i<attributes.length; i++) {
		var value = node.style[attributes[i]];
		if (value) style += attributes[i] + ': ' + value + ';';
	}
	html += ' style="' + style + ' width: ' + size.width + 'px; height: ' + size.height + 'px;"' + '>';
	var childs = node.childNodes;
	for (var i=0; i<childs.length; i++) {
		html += this.toPreviewHTML(childs[i]);
	}
	html += '</' + node.nodeName + '>';
	return html;
}

var SelectTool = {}

SelectTool.createSelectionBox = function(x1, y1, x2, y2, pixelSize, pixelColor, spacing, maxDots) {
	var temp;
	if (x2 < x1) {
		temp = x1;
		x1 = x2;
		x2 = temp;
	}
	
	if (y2 < y1) {
		temp = y1;
		y1 = y2;
		y2 = temp;
	}
	
	var cx = x2 - x1;
	var cy = y2 - y1;

	var dotCountX = cx / (spacing + pixelSize);
	var dotCountY = cy / (spacing + pixelSize);
	if (maxDots && dotCountX > maxDots) dotCountX = maxDots;
	if (maxDots && dotCountY > maxDots) dotCountY = maxDots;
	var dx = cx / dotCountX;
	var dy = cy / dotCountY;

	var x = x1;
	var y = y1;
	var dotsHTML = '';
	for (var i = 0; i < dotCountX; i++) {
		dotsHTML += '<div style="position:absolute; overflow:hidden; ' +
		'left:' + Math.ceil(x-pixelSize/2) + 'px; ' +
		'top:' + y + 'px; ' +
		'width:' + pixelSize + 'px; ' +
		'height:' + pixelSize + 'px; ' +
		'background-color: ' + pixelColor + ';"></div>';
		
		x += dx;
	}
	x = x1;
	for (var i = 0; i < dotCountX; i++) {
		dotsHTML += '<div style="position:absolute; overflow:hidden; ' +
		'left:' + Math.ceil(x-pixelSize/2) + 'px; ' +
		'top:' + y2 + 'px; ' +
		'width:' + pixelSize + 'px; ' +
		'height:' + pixelSize + 'px; ' +
		'background-color: ' + pixelColor + ';"></div>';
		
		x += dx;
	}
	
	for (var i = 0; i < dotCountY; i++) {
		dotsHTML += '<div style="position:absolute; overflow:hidden; ' +
		'left:' + x1 + 'px; ' +
		'top:' + Math.ceil(y-pixelSize/2) + 'px; ' +
		'width:' + pixelSize + 'px; ' +
		'height:' + pixelSize + 'px; ' +
		'background-color: ' + pixelColor + ';"></div>';
		
		y += dy;
	}
	y = y1;
	
	for (var i = 0; i < dotCountY; i++) {
		dotsHTML += '<div style="position:absolute; overflow:hidden; ' +
		'left:' + x2 + 'px; ' +
		'top:' + Math.ceil(y-pixelSize/2) + 'px; ' +
		'width:' + pixelSize + 'px; ' +
		'height:' + pixelSize + 'px; ' +
		'background-color: ' + pixelColor + ';"></div>';
		
		y += dy;
	}
	
	
	var div = Builder.node('div', {
		id: 'select_tool',
		style: 'position:absolute; overflow:visible; ' +
		 		'top: 0px; left: 0px; width:1px; height:1px;'
	});
	div.innerHTML = dotsHTML;
	return div;
}



