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

/**
 * @author W Melhem
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class PluginSearchUIOperation
	extends PluginSearchOperation
	implements IRunnableWithProgress {

	private static final String KEY_TASKNAME = "SearchMonitorDialog.taskName";

	public PluginSearchUIOperation(
		PluginSearchInput input,
		IPluginSearchResultCollector collector) {
		super(input, collector);
	}

	public void run(IProgressMonitor monitor) {
		try {
			IWorkspaceRunnable workspaceRunnable = new IWorkspaceRunnable() {
				public void run(IProgressMonitor pm) throws CoreException {
					execute(pm, PDEPlugin.getResourceString(KEY_TASKNAME));
					// CoreException and OperationCanceledException are propagated
				}
			};
			WorkbenchPlugin.getPluginWorkspace().run(
				workspaceRunnable,
				monitor);
		} catch (CoreException e) {
		} catch (OperationCanceledException e) {
		}
	}
}