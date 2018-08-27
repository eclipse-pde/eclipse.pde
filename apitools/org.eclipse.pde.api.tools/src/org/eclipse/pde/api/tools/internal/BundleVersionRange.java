/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal;

import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.pde.api.tools.internal.provisional.IVersionRange;
import org.osgi.framework.Version;

/**
 * Implementation of a required component description based on OSGi bundles.
 *
 * @since 1.0.0
 */
public class BundleVersionRange implements IVersionRange {

	private VersionRange fRange;

	/**
	 * Constructs a new version range based on the given required bundle version
	 * interval.
	 *
	 * @param versionInterval string representing mathematical interval
	 *            describing range of compatible versions
	 */
	public BundleVersionRange(String versionInterval) {
		fRange = new VersionRange(versionInterval);
	}

	/**
	 * Constructs a new version range based on the given range.
	 *
	 * @param range version range
	 */
	public BundleVersionRange(VersionRange range) {
		fRange = range;
	}

	@Override
	public String getMaximumVersion() {
		return fRange.getMaximum().toString();
	}

	@Override
	public String getMinimumVersion() {
		return fRange.getMinimum().toString();
	}

	@Override
	public boolean isIncludeMaximum() {
		return fRange.getIncludeMaximum();
	}

	@Override
	public boolean isIncludeMinimum() {
		return fRange.getIncludeMinimum();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof BundleVersionRange) {
			BundleVersionRange range = (BundleVersionRange) obj;
			return fRange.equals(range.fRange);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return fRange.hashCode();
	}

	@Override
	public String toString() {
		return fRange.toString();
	}

	@Override
	public boolean isIncluded(String version) {
		return fRange.isIncluded(new Version(version));
	}

}
