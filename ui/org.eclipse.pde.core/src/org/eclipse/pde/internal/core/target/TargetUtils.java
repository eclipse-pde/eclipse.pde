package org.eclipse.pde.internal.core.target;

import java.net.*;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.target.provisional.ITargetDefinition;

/**
 * Collection of static convenience methods for working with targets.  Strictly for internal use.
 * 
 * TODO Combine with {@link TargetPlatformHelper} and other target classes inside that package
 * 
 * @since 3.6
 */
public class TargetUtils {

	/**
	 * Collects the set of URL plug-in paths representing the content of the target definition.  The
	 * target must be both resolved and provisioned or this method will return an empty array.
	 * 
	 * @param definition definition to get plug-ins from
	 * @return list of URL locations of plug-ins, possibly empty
	 */
	public static URL[] getPluginPaths(ITargetDefinition definition) {
		if (!definition.isProvisioned()) {
			return new URL[0];
		}

		// Create models for the provisioned bundles
		BundleInfo[] bundles = definition.getProvisionedBundles();
		List bundleLocations = new ArrayList(bundles.length);
		for (int i = 0; i < bundles.length; i++) {
			try {
				URI location = bundles[i].getLocation();
				if (location != null) {
					bundleLocations.add(location.toURL());
				}
			} catch (MalformedURLException e) {
				// Ignore invalid urls, UI should see and handle them
			}
		}
		return (URL[]) bundleLocations.toArray(new URL[bundleLocations.size()]);
	}

}
