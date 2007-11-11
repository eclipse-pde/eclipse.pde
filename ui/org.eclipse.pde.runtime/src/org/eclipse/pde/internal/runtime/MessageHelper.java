/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.runtime;

import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.ImportPackageSpecification;
import org.eclipse.osgi.service.resolver.VersionConstraint;
import org.eclipse.osgi.util.NLS;

public class MessageHelper {
	public static String getResolutionFailureMessage(VersionConstraint unsatisfied) {
		if (unsatisfied.isResolved())
			throw new IllegalArgumentException();
		if (unsatisfied instanceof ImportPackageSpecification)
			return NLS.bind(PDERuntimeMessages.MessageHelper_missing_imported_package, toString(unsatisfied));
		else if (unsatisfied instanceof BundleSpecification) {
			if (((BundleSpecification) unsatisfied).isOptional())
				return NLS.bind(PDERuntimeMessages.MessageHelper_missing_optional_required_bundle, toString(unsatisfied));
			return NLS.bind(PDERuntimeMessages.MessageHelper_missing_required_bundle, toString(unsatisfied));
		} else
			return NLS.bind(PDERuntimeMessages.MessageHelper_missing_host, toString(unsatisfied));
	}

	private static String toString(VersionConstraint constraint) {
		org.eclipse.osgi.service.resolver.VersionRange versionRange = constraint.getVersionRange();
		if (versionRange == null)
			return constraint.getName();
		return constraint.getName() + '_' + versionRange;
	}

}
