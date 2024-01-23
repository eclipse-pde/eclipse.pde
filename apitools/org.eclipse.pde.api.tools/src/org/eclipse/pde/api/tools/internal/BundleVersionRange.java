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

import org.eclipse.pde.api.tools.internal.provisional.IVersionRange;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

/**
 * Implementation of a required component description based on OSGi bundles.
 *
 * @since 1.0.0
 */
public class BundleVersionRange implements IVersionRange {

	private final VersionRange fRange;

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
		Version right = fRange.getRight();
		return right != null ? right.toString() : null;
	}

	@Override
	public String getMinimumVersion() {
		return fRange.getLeft().toString();
	}

	@Override
	public boolean isIncludeMaximum() {
		return fRange.getRightType() == VersionRange.RIGHT_CLOSED;
	}

	@Override
	public boolean isIncludeMinimum() {
		return fRange.getLeftType() == VersionRange.LEFT_CLOSED;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof BundleVersionRange range) {
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
		return fRange.includes(new Version(version));
	}

}
