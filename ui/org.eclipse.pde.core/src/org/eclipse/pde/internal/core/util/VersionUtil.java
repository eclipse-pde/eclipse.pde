/*******************************************************************************
 *  Copyright (c) 2006, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.util;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.pde.core.plugin.IMatchRules;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.osgi.framework.Version;

public class VersionUtil {

	public static IStatus validateVersion(String versionString) {
		try {
			if (versionString != null)
				new Version(versionString.trim());
		} catch (IllegalArgumentException e) {
			return new Status(IStatus.ERROR, PDECore.PLUGIN_ID, IStatus.ERROR, PDECoreMessages.BundleErrorReporter_InvalidFormatInBundleVersion, e);
		}
		return Status.OK_STATUS;
	}

	public static IStatus validateVersionRange(String versionRangeString) {
		try {
			new VersionRange(versionRangeString);
		} catch (IllegalArgumentException e) {
			return new Status(IStatus.ERROR, PDECore.PLUGIN_ID, IStatus.ERROR, PDECoreMessages.BundleErrorReporter_invalidVersionRangeFormat, e);
		}
		return Status.OK_STATUS;
	}

	public static boolean compare(String id1, String version1, String id2, String version2, int match) {
		if (!(id1.equals(id2)))
			return false;
		try {
			Version v1 = Version.parseVersion(version1);
			Version v2 = Version.parseVersion(version2);

			switch (match) {
				case IMatchRules.NONE :
				case IMatchRules.COMPATIBLE :
					return isCompatibleWith(v1, v2);
				case IMatchRules.EQUIVALENT :
					return isEquivalentTo(v1, v2);
				case IMatchRules.PERFECT :
					return v1.equals(v2);
				case IMatchRules.GREATER_OR_EQUAL :
					return isGreaterOrEqualTo(v1, v2);
			}
		} catch (RuntimeException e) {
		}
		return version1.equals(version2);
	}

	/**
	 * Returns true if the given version number is an empty version as
	 * defined by {@link Version}. Used in cases where it would be
	 * inappropriate to parse the actual version number.
	 * 
	 * @param version version string to check
	 * @return true if empty version
	 */
	public static boolean isEmptyVersion(String version) {
		if (version == null)
			return true;
		version = version.trim();
		return version.length() == 0 || version.equals(Version.emptyVersion.toString());
	}

	public static boolean isCompatibleWith(Version v1, Version v2) {
		if (v1.getMajor() != v2.getMajor())
			return false;
		if (v1.getMinor() > v2.getMinor())
			return true;
		if (v1.getMinor() < v2.getMinor())
			return false;
		if (v1.getMicro() > v2.getMicro())
			return true;
		if (v1.getMicro() < v2.getMicro())
			return false;
		return v1.getQualifier().compareTo(v2.getQualifier()) >= 0;
	}

	public static boolean isEquivalentTo(Version v1, Version v2) {
		if (v1.getMajor() != v2.getMajor() || v1.getMinor() != v2.getMinor())
			return false;
		if (v1.getMicro() > v2.getMicro())
			return true;
		if (v1.getMicro() < v2.getMicro())
			return false;
		return v1.getQualifier().compareTo(v2.getQualifier()) >= 0;
	}

	public static boolean isGreaterOrEqualTo(Version v1, Version v2) {
		if (v1.getMajor() > v2.getMajor())
			return true;
		if (v1.getMajor() == v2.getMajor()) {
			if (v1.getMinor() > v2.getMinor())
				return true;
			if (v1.getMinor() == v2.getMinor()) {
				if (v1.getMicro() > v2.getMicro())
					return true;
				if (v1.getMicro() == v2.getMicro())
					return v1.getQualifier().compareTo(v2.getQualifier()) >= 0;
			}
		}
		return false;
	}

	public static int compareMacroMinorMicro(Version v1, Version v2) {
		int result = v1.getMajor() - v2.getMajor();
		if (result != 0)
			return result;

		result = v1.getMinor() - v2.getMinor();
		if (result != 0)
			return result;

		result = v1.getMicro() - v2.getMicro();
		return result;
	}

	public static String computeInitialPluginVersion(String version) {
		if (version != null && VersionUtil.validateVersion(version).isOK()) {
			Version pvi = Version.parseVersion(version);
			return pvi.getMajor() + "." + pvi.getMinor() //$NON-NLS-1$
					+ "." + pvi.getMicro(); //$NON-NLS-1$
		}

		return version;
	}

}
