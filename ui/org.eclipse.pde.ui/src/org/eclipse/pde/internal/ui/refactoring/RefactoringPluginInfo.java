package org.eclipse.pde.internal.ui.refactoring;

import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;

public class RefactoringPluginInfo extends RefactoringInfo {
	
	private boolean fRenameProject;
	
	public boolean isRenameProject() {
		return fRenameProject;
	}

	public void setRenameProject(boolean renameProject) {
		fRenameProject = renameProject;
	}
	
	public String getCurrentValue() {
		IPluginModelBase base = getBase();
		if (base == null)
			return null;
		BundleDescription desc = base.getBundleDescription();
		if (desc != null)
			return desc.getSymbolicName();
		IPluginBase pb = base.getPluginBase();
		return pb.getId();
	}

	public IPluginModelBase getBase() {
		return (fSelection instanceof IPluginModelBase) ? (IPluginModelBase)fSelection : null; 
	}

}
