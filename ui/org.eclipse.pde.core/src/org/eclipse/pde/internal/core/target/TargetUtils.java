package org.eclipse.pde.internal.core.target;

import java.io.File;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.equinox.p2.core.*;
import org.eclipse.pde.internal.core.PDECore;
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
	 * Path to the local directory where the local bundle pool is stored for targets that
	 * must download artifacts.
	 */
	private static final String BUNDLE_POOL = PDECore.getDefault().getStateLocation().append(".bundle_pool").toOSString(); //$NON-NLS-1$

	/**
	 * p2 data area for all targets
	 */
	private static final String AGENT_LOCATION = PDECore.getDefault().getStateLocation().append("p2").toOSString(); //$NON-NLS-1$

	/**
	 * Name of the profile for the current active target.  There is only one profile for targets in the agent,
	 * in the future we may create separate profiles for each provisioned target
	 */
	private static final String TARGET_PROFILE = "TARGET_PROFILE"; //$NON-NLS-1$

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

	/**
	 * Return the provisioning agent to be used with the given target definition.  May return 
	 * <code>null</code> if the provisioning agent service is unavailable.  Callers should call
	 * {@link IProvisioningAgent#stop()} when they are done working with the agent.
	 * 
	 * @param definition 
	 * @return provisioning agent or <code>null</code>
	 */
	public static IProvisioningAgent getProvisioningAgent(ITargetDefinition definition) {
		IProvisioningAgentProvider provider = (IProvisioningAgentProvider) PDECore.getDefault().acquireService(IProvisioningAgentProvider.SERVICE_NAME);
		if (provider == null) {
			return null;
		}
		try {
			return provider.createAgent(new File(AGENT_LOCATION).toURI());
		} catch (ProvisionException e) {
			return null;
		}
	}

	public static String getProfileID(ITargetDefinition definition) {
		return TARGET_PROFILE;
	}

	public static String getSharedBundlePool(ITargetDefinition definition) {
		return BUNDLE_POOL;
	}

}
