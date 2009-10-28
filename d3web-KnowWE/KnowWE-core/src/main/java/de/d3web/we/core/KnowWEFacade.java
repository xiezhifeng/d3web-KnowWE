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

package de.d3web.we.core;

import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.apache.commons.fileupload.FileItem;

import de.d3web.we.action.GlobalReplaceAction;
import de.d3web.we.action.KnowWEAction;
import de.d3web.we.action.KnowWEObjectTypeActivationAction;
import de.d3web.we.action.KnowWEObjectTypeBrowserAction;
import de.d3web.we.action.ParseWebOfflineRenderer;
import de.d3web.we.action.ReplaceKDOMNodeAction;
import de.d3web.we.action.SetQuickEditFlagAction;
import de.d3web.we.action.TagHandlingAction;
import de.d3web.we.action.UpdateTableKDOMNodes;
import de.d3web.we.action.WordBasedRenameFinding;
import de.d3web.we.action.WordBasedRenamingAction;
import de.d3web.we.kdom.KnowWEArticle;
import de.d3web.we.module.KnowWEModule;
import de.d3web.we.upload.UploadManager;


/**
 * @author Jochen
 * 
 * This Facade offers Methods to perform user driven actions, coming by as a
 * (AJAX-) request through (for example) some instance of KnowWEActionDispatcher
 * (e.g. JspwikiActionDispatcher) Each method returns a String shown in the
 * user-view of the webapp. In the KnowWEParameterMap all
 * HTTP-Request-Attributes are stores to give the necessary informations to the
 * specific action.
 * 
 * 
 */
public class KnowWEFacade {

	/**
	 * holds all possible Actions, that can be performed by the KnowWEEngine
	 */
	private Map<Class<? extends KnowWEAction>, KnowWEAction> actionMap = new HashMap<Class<? extends KnowWEAction>, KnowWEAction>();

	/**
	 * Singleton instance
	 */
	private static KnowWEFacade instance;

	/**
	 * lazy singleton instance factory
	 * 
	 * @return
	 */
	public static synchronized KnowWEFacade getInstance() {
		if (instance == null) {
			instance = new KnowWEFacade();
		}

		return instance;
	}

	/**
	 * prevent cloning
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	private KnowWEFacade() {
		initActions();
	}

	/**
	 * initialises all supported actions dont forget to register your new
	 * written actions here!
	 */
	/**
	 * 
	 */
	private void initActions() {
        actionMap.put(SetQuickEditFlagAction.class, new SetQuickEditFlagAction());

		actionMap.put(WordBasedRenamingAction.class, new WordBasedRenamingAction());
		actionMap.put(GlobalReplaceAction.class, new GlobalReplaceAction());

		actionMap.put(ReplaceKDOMNodeAction.class, new ReplaceKDOMNodeAction());
		actionMap.put(UpdateTableKDOMNodes.class, new UpdateTableKDOMNodes());
		actionMap.put(KnowWEObjectTypeBrowserAction.class, new KnowWEObjectTypeBrowserAction());
		actionMap.put(KnowWEObjectTypeActivationAction.class, new KnowWEObjectTypeActivationAction());
		
		actionMap.put(TagHandlingAction.class, new TagHandlingAction());
		
		List<KnowWEModule> modules = KnowWEEnvironment.getInstance()
				.getModules();
		for (KnowWEModule knowWEModule : modules) {
			knowWEModule.addAction(actionMap);
		}
	}

	/**
	 * Used for Tests.
	 * 
	 * @param map
	 * @return
	 */
	public String replaceKDOMNode(KnowWEParameterMap map) {
		return this.actionMap.get(ReplaceKDOMNodeAction.class).perform(map);
	}
	
	/**
	 * 
	 * AutoCompletion for editing in the KnowWE-Wiki. Returns a list of
	 * propositions for continuing the users writing. Result is further
	 * processed and inserted by several javascript functions.
	 * 
	 * @param parameterMap
	 * @return propositions for autocompletion
	 */
	public String autoCompletion(KnowWEParameterMap parameterMap) {
		return performAction("CodeCompletionRenderer", parameterMap);
	}

	/**
	 * (re-)parses the whole web/wiki
	 * 
	 * @param parameterMap
	 * @return
	 */
	public String parseAll(KnowWEParameterMap parameterMap) {

		ResourceBundle bundle = ResourceBundle.getBundle("KnowWE_config");

		if (bundle.getString("knowwe2wiki.parseAllFunction").equals("true")) {

			if (parameterMap.getWikiContext().userIsAdmin()) {
				KnowWEAction action = actionMap
						.get(ParseWebOfflineRenderer.class);
				return action.perform(parameterMap);
			}

			return "<p class=\"info box\">"
					+ KnowWEEnvironment.getInstance().getKwikiBundle(parameterMap.getRequest()).getString("KnowWE.login.error.admin")
					+ "</p>";
		}

		KnowWEAction action = actionMap.get(ParseWebOfflineRenderer.class);
		return action.perform(parameterMap);
	}

	/**
	 * Allows to perform actions by Class
	 * 
	 * @param parameterMap
	 * @return
	 */
	public KnowWEAction getAction(Class<? extends KnowWEAction> clazz) {
		return actionMap.get(clazz);
	}

	/**
	 * Allows to perform actions by Classname
	 * 
	 * @param parameterMap
	 * @return
	 */
	public KnowWEAction getAction(String name) {
		Set<Class<? extends KnowWEAction>> classSet = actionMap.keySet();
		for (Class<? extends KnowWEAction> class1 : classSet) {
			if (class1.getName().equals(name)) {
				return actionMap.get(class1);
			}
		}
		return null;
	}

	public String performAction(String action, KnowWEParameterMap parameterMap) {
		if (action == null) {
			action = parameterMap.get("action");
			if (action == null) {
				throw new IllegalArgumentException("no action specified");
			}
		}
		
		//Hotfix: search in ActionMap
		KnowWEAction actionInstance = findInActionMap(action); 
		if(actionInstance != null) return actionInstance.perform(parameterMap);
		try {
			// check is action is fully qualified class name
			if (!action.contains(".")) {
				// if not, use d3web default package
				action = "de.d3web.we.action." + action;
			}
			Class clazz = Class.forName(action);
			actionInstance = actionMap.get(clazz);
			if (actionInstance == null) {
				Logger.getLogger(this.getClass().getName()).warning(
						"Action/Render " + action + " wasn't registered => default instanciation");
				actionInstance = (KnowWEAction)clazz.newInstance();
			} 
			return actionInstance.perform(parameterMap);
		} 
		catch (ClassNotFoundException e) {
			String msg = "action's class not found: "+action;
			Logger.getLogger(this.getClass().getName()).severe(msg);
			throw new IllegalArgumentException(msg, e);
		}
		catch (InstantiationException e) {
			String msg = "action cannot be instanciated: "+action;
			Logger.getLogger(this.getClass().getName()).severe(msg);
			throw new IllegalArgumentException(msg, e);
		} 
		catch (IllegalAccessException e) {
			String msg = "action constructor cannot be accessed: "+action;
			Logger.getLogger(this.getClass().getName()).severe(msg);
			throw new IllegalArgumentException(msg, e);
		}
		catch (ClassCastException e) {
			String msg = "action is not an implementation of KnowWEAction: "+action;
			Logger.getLogger(this.getClass().getName()).severe(msg);
			throw new IllegalArgumentException(msg, e);
		}
	}

	private KnowWEAction findInActionMap(String action) {
		for(Entry<Class<? extends KnowWEAction>, KnowWEAction> entry : this.actionMap.entrySet()) {
			if(entry.getValue().getClass().getName().contains(action)) {
				return entry.getValue();
			}
		}
		return null;
	}

	public String performAction(KnowWEParameterMap parameterMap) {
		return this.performAction(null, parameterMap);
	}

	public String getNodeData(String web, String topic, String nodeID) {
		return KnowWEEnvironment.getInstance().getNodeData(web, topic, nodeID);
	}

	public String uploadFiles(Collection<FileItem> fileItems) {
		return UploadManager.getInstance().manageUpload(fileItems);
	}

	/**
	 * Generates XCL from a Tirex Input of a given Page.
	 * TODO: Hey this class is gone... Where is it =(
	 * 
	 * @param parameterMap
	 * @return
	 */
	public String tirexToXCL(KnowWEParameterMap parameterMap) {
		String className = "TirexToXCLRenderer";
		Class clazz;
		try {
			this.getClass();
			clazz = Class.forName(className);
			KnowWEAction action = actionMap.get(clazz);
			return action.perform(parameterMap);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "Class not Found: " + className;
	}

	/**
	 * This returns a dump of the current Ontology
	 * @return
	 */
	public void writeOwl(OutputStream stream){
	    SemanticCore.getInstance().writeDump(stream);
	}
	
	/**
	 * This is for the JUnit Test of the Renaming Tool.
	 */
	public Map<KnowWEArticle, Collection<WordBasedRenameFinding>> renamingToolTest(
			KnowWEParameterMap map) {
		return ((WordBasedRenamingAction) this.actionMap
				.get(WordBasedRenamingAction.class)).scanForFindings(map
				.getWeb(), map.get(KnowWEAttributes.TARGET), map.get(
				KnowWEAttributes.CONTEXT_PREVIOUS).length(), null);
	}

}
