package org.eclipse.pde.internal.core.search;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.core.ModelEntry;

/**
 * @author W Melhem
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class PluginSearchOperation {
	private PluginSearchInput input;
	private IPluginSearchResultCollector collector;
	
	public PluginSearchOperation(PluginSearchInput input, IPluginSearchResultCollector collector) {
		this.input = input;
		this.collector = collector;
		collector.setOperation(this);
	}
	
	public void execute(IProgressMonitor monitor) {
		ModelEntry[] entries = input.getSearchScope().getMatchingEntries();
		collector.searchStarted();
		monitor.beginTask("Searching...", entries.length);
		
		for (int i = 0; i < entries.length; i++) {
			IPluginModelBase candidate = entries[i].getActiveModel();
			visit(candidate);
			monitor.worked(1);
		}
		monitor.done();
	}
	
	private void visit(IPluginModelBase model) {
		IPluginObject match = findMatch(model);
		if (match != null) {
			collector.accept(match);
		}
	}
	
	private IPluginObject findMatch(IPluginModelBase model) {
		return null;
	}
	
	public String getPluralLabel() {
		return "";
	}
	
	public String getSingularLabel() {
		return "";
	}
	
}
