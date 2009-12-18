package org.eclipse.pde.internal.ui.shared.target;

import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.pde.internal.core.target.IUBundleContainer;
import org.eclipse.pde.internal.core.target.provisional.ITargetDefinition;

/**
 * Wraps the installable unit descriptions provided by IUBundleContainer.  If a resolved target is available
 * this class can calculate and the cache the actual installable unit from the target.  An installable unit
 * can provide better information for the label providers than the installable unit description can.
 * 
 * @see InstallableUnitDescription
 * @see IInstallableUnit
 * @see TargetLocationsGroup
 * @see IUBundleContainer
 * 
 * @since 3.6
 */
public class InstallableUnitWrapper {

	private InstallableUnitDescription fDescription;
	// TODO Is caching this in the object actually beneficial?
	private IInstallableUnit fUnit;
	private ITargetDefinition fTarget;

	public InstallableUnitWrapper(InstallableUnitDescription description, ITargetDefinition target) {
		fDescription = description;
		fTarget = target;
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
