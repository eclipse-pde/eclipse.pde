package org.eclipse.pde.internal.ui.model.build;

import org.eclipse.pde.core.build.*;

public class BuildModelFactory implements IBuildModelFactory {
	
	private IBuildModel fModel;
	
	public BuildModelFactory(IBuildModel model) {
		fModel = model;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.build.IBuildModelFactory#createEntry(java.lang.String)
	 */
	public IBuildEntry createEntry(String name) {
		BuildEntry entry = new BuildEntry();
		entry.setName(name);
		entry.setModel(fModel);
		return entry;
	}
}
