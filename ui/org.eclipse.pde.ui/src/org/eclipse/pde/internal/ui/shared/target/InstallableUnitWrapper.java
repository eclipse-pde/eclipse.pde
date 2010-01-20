package org.eclipse.pde.internal.ui.shared.target;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.pde.internal.core.target.IUBundleContainer;
import org.eclipse.pde.internal.core.target.provisional.ITargetDefinition;
import org.eclipse.pde.internal.core.target.provisional.NameVersionDescriptor;

/**
 * Wraps the installable unit descriptions provided by IUBundleContainer.  If a resolved target is available
 * this class can calculate and the cache the actual installable unit from the target.  An installable unit
 * can provide better information for the label providers than the installable unit description can.  Has a 
 * pointer to its parent container so the wrapper can be be interacted with in tables and trees
 * 
 * @see NameVersionDescriptor
 * @see IInstallableUnit
 * @see TargetLocationsGroup
 * @see IUBundleContainer
 * 
 * @since 3.6
 */
public class InstallableUnitWrapper {

	private NameVersionDescriptor fDescription;
	// TODO Is caching this in the object actually beneficial?
	private IInstallableUnit fUnit;
	private ITargetDefinition fTarget;
	private IUBundleContainer fContainer;

	public InstallableUnitWrapper(NameVersionDescriptor description, IUBundleContainer container, ITargetDefinition target) {
		fDescription = description;
		fTarget = target;
		fContainer = container;
	}

	public IUBundleContainer getContainer() {
		return fContainer;
	}

	public Object getBestUnit() {
		// If we cached the unit, return it
		if (fUnit != null) {
			return fUnit;
		}
		// If the target is resolved, try to get the actual unit
		if (fTarget.isResolved()) {
			fUnit = fTarget.getResolvedUnit(fDescription);
			if (fUnit != null) {
				return fUnit;
			}
		}
		// Otherwise just return the description object
		return fDescription;
	}

}
