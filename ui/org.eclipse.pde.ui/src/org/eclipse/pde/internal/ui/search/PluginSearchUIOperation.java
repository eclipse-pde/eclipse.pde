package org.eclipse.pde.internal.ui.search;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.pde.internal.core.search.IPluginSearchResultCollector;
import org.eclipse.pde.internal.core.search.PluginSearchInput;
import org.eclipse.pde.internal.core.search.PluginSearchOperation;
import org.eclipse.pde.internal.ui.PDEPlugin;

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

	public void run(IProgressMonitor monitor)
		throws InvocationTargetException, InterruptedException {
		execute(monitor, PDEPlugin.getResourceString(KEY_TASKNAME));
	}
}