package org.eclipse.pde.internal.core.bnd;

import aQute.bnd.osgi.PluginsContainer;

public class PDEPluginsContainer extends PluginsContainer {

	public PDEPluginsContainer() {
		add(TargetRepository.getTargetRepository());
	}

}
