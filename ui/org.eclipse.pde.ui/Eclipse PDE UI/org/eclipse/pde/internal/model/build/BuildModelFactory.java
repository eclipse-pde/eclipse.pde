package org.eclipse.pde.internal.model.build;

import org.eclipse.pde.internal.base.model.build.*;
import org.eclipse.pde.internal.base.model.plugin.*;

public class BuildModelFactory implements IBuildModelFactory {
	private IBuildModel model;

public BuildModelFactory(IBuildModel model) {
	this.model = model;
}
public IBuildEntry createEntry(String name) {
	BuildEntry entry = new BuildEntry(name);
	entry.setModel(model);
	return entry;
}
}
