package org.eclipse.pde.internal.core.target;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.pde.core.target.TargetBundle;

/**
 * Target bundle representing a problem with content in a target. Uses
 * the status codes found on {@link TargetBundle}.
 */
public class InvalidTargetBundle extends TargetBundle {

	private IStatus fStatus;

	/**
	 * Creates a new target bundle with the given status and additional bundle information
	 *  
	 * @param bundleInfo bundle info object containing information about the target content if available (symbolic name, version, location)
	 * @param status status describing the problem with this content
	 */
	public InvalidTargetBundle(BundleInfo bundleInfo, IStatus status) {
		fInfo = bundleInfo;
		fStatus = status;
	}

	public IStatus getStatus() {
		return fStatus;
	}

}
