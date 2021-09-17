package org.eclipse.pde.spy.model;

import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.e4.tools.emf.ui.common.IExtensionLookup;

@SuppressWarnings("restriction")
public class RuntimeExtensionLookup implements IExtensionLookup {

	@Override
	public IExtension[] findExtensions(String extensionPointId, boolean liveModel) {
		IExtensionRegistry registry = RegistryFactory.getRegistry();
		return registry.getExtensionPoint(extensionPointId).getExtensions();
	}

}
