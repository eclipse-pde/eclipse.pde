/*******************************************************************************
 * Copyright (c) 2024, 2024 Hannes Wellmann and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Hannes Wellmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core.plugin;

import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

/**
 * Defines the rules that are used to control when two compared versions match.
 *
 * @since 3.19
 */
public enum VersionMatchRule {
	/**
	 * A rule that matches if a version is perfectly
	 * {@link Version#equals(Object) equal} to a reference.
	 */
	PERFECT("perfect") { //$NON-NLS-1$
		@Override
		public VersionRange rangeFor(Version reference) {
			return new VersionRange(VersionRange.LEFT_CLOSED, reference, reference, VersionRange.RIGHT_CLOSED);
		}

		@Override
		public boolean matches(Version version, Version reference) {
			return reference.equals(version);
		}
	},
	/**
	 * A rule that matches if a version is <em>equivalent</em> to a reference,
	 * which is the case if that version has the same {@link Version#getMajor()
	 * major} and {@link Version#getMinor() minor} component and apart from that
	 * is greater than or equal to the reference.
	 */
	EQUIVALENT("equivalent") { //$NON-NLS-1$
		@Override
		public VersionRange rangeFor(Version reference) {
			Version upperBound = new Version(reference.getMajor(), reference.getMinor() + 1, 0);
			return new VersionRange(VersionRange.LEFT_CLOSED, reference, upperBound, VersionRange.RIGHT_OPEN);
		}

		@Override
		public boolean matches(Version version, Version reference) {
			return version.getMajor() == reference.getMajor() && version.getMinor() == reference.getMinor()
					&& version.compareTo(reference) >= 0;
		}
	},
	/**
	 * A rule that matches if a version is <em>compatible</em> to a reference,
	 * which is the case if that version has the same {@link Version#getMajor()
	 * major} component and apart from that is greater than or equal to the
	 * reference.
	 */
	COMPATIBLE("compatible") { //$NON-NLS-1$
		@Override
		public VersionRange rangeFor(Version reference) {
			Version upperBound = new Version(reference.getMajor() + 1, 0, 0);
			return new VersionRange(VersionRange.LEFT_CLOSED, reference, upperBound, VersionRange.RIGHT_OPEN);
		}

		@Override
		public boolean matches(Version version, Version reference) {
			return version.getMajor() == reference.getMajor() && version.compareTo(reference) >= 0;
		}
	},
	/**
	 * A rule that matches if a version is greater than or equal to a reference.
	 */
	GREATER_OR_EQUAL("greaterOrEqual") { //$NON-NLS-1$
		@Override
		public VersionRange rangeFor(Version reference) {
			return new VersionRange(VersionRange.LEFT_CLOSED, reference, null, VersionRange.RIGHT_OPEN);
		}

		@Override
		public boolean matches(Version version, Version reference) {
			return version.compareTo(reference) >= 0;
		}
	};

	private final String name;

	VersionMatchRule(String name) {
		this.name = name;
	}

	public abstract VersionRange rangeFor(Version version);

	public abstract boolean matches(Version version, Version reference);

	@Override
	public String toString() {
		return name;
	}
}
