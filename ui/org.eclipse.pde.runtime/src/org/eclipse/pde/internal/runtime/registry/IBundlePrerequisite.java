package org.eclipse.pde.internal.runtime.registry;

import org.eclipse.osgi.util.ManifestElement;

public interface IBundlePrerequisite {
	public ManifestElement getPrerequisite();
	public boolean isExported();
	public String getLabel();
}
