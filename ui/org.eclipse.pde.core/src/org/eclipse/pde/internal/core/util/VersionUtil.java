/*******************************************************************************
 *  Copyright (c) 2006, 2024 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Hannes Wellmann - Rework PluginRegistry API, replace Equinox resolver's VersionRange and introduce VersionMatchRule enum
 *******************************************************************************/
package org.eclipse.pde.internal.core.util;

import java.util.Comparator;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.core.plugin.IMatchRules;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.VersionMatchRule;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

public class VersionUtil {

	private VersionUtil() { // static use only
	}

	public static IStatus validateVersion(String versionString) {
		try {
			if (versionString != null) {
				new Version(versionString.trim());
			}
		} catch (IllegalArgumentException e) {
			return Status.error(UtilMessages.BundleErrorReporter_InvalidFormatInBundleVersion, e);
		}
		return Status.OK_STATUS;
	}

	public static IStatus validateVersionRange(String versionRangeString) {
		try {
			new VersionRange(versionRangeString);
		} catch (IllegalArgumentException e) {
			return Status.error(UtilMessages.BundleErrorReporter_invalidVersionRangeFormat, e);
		}
		return Status.OK_STATUS;
	}

	public static final Comparator<IPluginModelBase> BY_DESCENDING_PLUGIN_VERSION = Comparator
			.comparing(VersionUtil::getVersion).reversed();

	public static Version getVersion(IPluginModelBase model) {
		String version = model.getPluginBase().getVersion();
		try {
			return Version.parseVersion(version);
		} catch (IllegalArgumentException e) {
		}
		return Version.emptyVersion;
	}

	public static VersionMatchRule matchRuleFromLiteral(int literal) {
		return switch (literal) {
		case IMatchRules.EQUIVALENT -> VersionMatchRule.EQUIVALENT;
		case IMatchRules.COMPATIBLE, IMatchRules.NONE -> VersionMatchRule.COMPATIBLE;
		case IMatchRules.PERFECT -> VersionMatchRule.PERFECT;
		case IMatchRules.GREATER_OR_EQUAL -> VersionMatchRule.GREATER_OR_EQUAL;
		default -> throw new IllegalArgumentException("Unsupported match rule literal: " + literal); //$NON-NLS-1$
		};
	}

	public static boolean compare(String version1, String version2, int match) {
		try {
			Version v1 = Version.parseVersion(version1);
			Version v2 = Version.parseVersion(version2);
			return matchRuleFromLiteral(match).matches(v1, v2);
		} catch (RuntimeException e) { // ignore
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
		if (version == null) {
			return true;
		}
		version = version.trim();
		return version.length() == 0 || version.equals(Version.emptyVersion.toString());
	}

	public static int compareMacroMinorMicro(Version v1, Version v2) {
		int result = v1.getMajor() - v2.getMajor();
		if (result != 0) {
			return result;
		}
		result = v1.getMinor() - v2.getMinor();
		if (result != 0) {
			return result;
		}
		result = v1.getMicro() - v2.getMicro();
		return result;
	}

	public static String computeInitialPluginVersion(String version) {
		if (version != null && VersionUtil.validateVersion(version).isOK()) {
			Version pvi = Version.parseVersion(version);
			return pvi.getMajor() + "." + pvi.getMinor() + "." + pvi.getMicro(); //$NON-NLS-1$//$NON-NLS-2$
		}
		return version;
	}

}
