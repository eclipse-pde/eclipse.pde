package org.eclipse.pde.internal.ui.search;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.pde.internal.core.search.IPluginSearchResultCollector;
import org.eclipse.pde.internal.core.search.PluginSearchInput;
import org.eclipse.pde.internal.core.search.PluginSearchOperation;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.internal.WorkbenchPlugin;


public class PluginSearchUIOperation
	extends PluginSearchOperation
	implements IRunnableWithProgress {

	private static final String KEY_TASKNAME = "SearchMonitorDialog.taskName";
	private static final String KEY_MATCH = "Search.singleMatch";
	private static final String KEY_MATCHES = "Search.multipleMatches";
	public PluginSearchUIOperation(
		PluginSearchInput input,
		IPluginSearchResultCollector collector) {
		super(input, collector);
	}

	public void run(IProgressMonitor monitor) {
		try {
			IWorkspaceRunnable workspaceRunnable = new IWorkspaceRunnable() {
				public void run(IProgressMonitor pm) throws CoreException {
					String taskName = PDEPlugin.getResourceString(KEY_TASKNAME);
					pm.setTaskName(taskName);
					execute(pm, taskName);
				}
			};
			WorkbenchPlugin.getPluginWorkspace().run(
				workspaceRunnable,
				monitor);
		} catch (CoreException e) {
		} catch (OperationCanceledException e) {
		}
	}
	
	public String getPluralLabel() {
		return input.getSearchString() + " - {0} " + PDEPlugin.getResourceString(KEY_MATCHES);
	}

	public String getSingularLabel() {
		return input.getSearchString() + " - 1 " + PDEPlugin.getResourceString(KEY_MATCH);
	}
	

}