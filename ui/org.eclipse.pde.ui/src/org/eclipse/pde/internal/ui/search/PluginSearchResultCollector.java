package org.eclipse.pde.internal.ui.search;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.ISourceObject;
import org.eclipse.pde.core.plugin.IFragment;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.core.search.IPluginSearchResultCollector;
import org.eclipse.pde.internal.core.search.PluginSearchOperation;
import org.eclipse.search.ui.ISearchResultView;
import org.eclipse.search.ui.SearchUI;

/**
 * @author W Melhem
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class PluginSearchResultCollector
	implements IPluginSearchResultCollector {
		
		private PluginSearchUIOperation operation;
		private ISearchResultView resultView;
		private static IPluginObject currentMatch;
		
	public static IPluginObject getCurrentMatch() {
		return currentMatch;
	}

	public void setOperation(PluginSearchOperation operation) {
		this.operation = (PluginSearchUIOperation) operation;
	}

	public PluginSearchOperation getOperation() {
		return operation;
	}

	public void accept(IPluginObject match) {
		try {
			String description = "";
			if (match instanceof IPluginExtension) {
				description = "extension";
			} else if (match instanceof IPluginExtensionPoint) {
				description = "extension point";
			} else if (match instanceof IFragment) {
				description = "fragment";
			} else {
				description = "plug-in";
			}
			
			currentMatch = match;
			
			IMarker marker = null;
			IResource resource = match.getModel().getUnderlyingResource();
			if (resource != null) {
				marker = resource.createMarker(SearchUI.SEARCH_MARKER);
				if (match instanceof ISourceObject) {
					marker.setAttribute(IMarker.LINE_NUMBER, ((ISourceObject)match).getStartLine());
				}
			}
					
			resultView.addMatch(
				description,
				match,
				resource,
				marker);
		} catch (CoreException e) {
		}
	}

	public void done() {
		resultView.searchFinished();
	}

	public void searchStarted() {
		resultView = SearchUI.getSearchResultView();
		resultView.searchStarted(
			new PluginSearchActionGroupFactory(),
			operation.getSingularLabel(),
			operation.getPluralLabel(),
			null,
			"org.eclipse.pde.internal.ui.search.SearchPage",
			new PluginSearchLabelProvider(),
			new PluginSearchGoToAction(),
			new PluginGroupByKeyComputer(),
			operation);
	}

}
