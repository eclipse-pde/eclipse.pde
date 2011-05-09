/*******************************************************************************
 *  Copyright (c) 2007, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.site;

import org.eclipse.equinox.p2.publisher.eclipse.FeatureEntry;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.pde.internal.build.Utils;
import org.osgi.framework.Version;

/**
 * ReachablePlugin's are sorted first by id, then by the width of the version range.
 * With equal range width, R1 < R2 if R1.range.getMinimum() < R2.range.getMaximum()
 */
public class ReachablePlugin implements Comparable {
	public static final VersionRange WIDEST_RANGE = VersionRange.emptyRange;
	public static final VersionRange NARROWEST_RANGE = new VersionRange(Version.emptyVersion, true, Version.emptyVersion, false);

	private final String id;
	private final VersionRange range;

	public ReachablePlugin(String id, VersionRange range) {
		this.id = id;
		this.range = range;
	}

	public ReachablePlugin(FeatureEntry entry) {
		id = entry.getId();
		range = Utils.createVersionRange(entry);
	}

	public String getId() {
		return id;
	}

	public VersionRange getRange() {
		return range;
	}

	public int compareTo(Object o) {
		if (o instanceof ReachablePlugin) {
			ReachablePlugin toCompare = (ReachablePlugin) o;
			int result = id.compareTo(toCompare.id);
			if (result != 0)
				return result;
			//We want the object with the widest version range to sort first
			result = substract(toCompare.range.getMaximum(), toCompare.range.getMinimum()).compareTo(substract(range.getMaximum(), range.getMinimum()));
			if (result != 0)
				return result;
			if (range.getIncludeMaximum() && !toCompare.range.getIncludeMaximum())
				return -1;
			if (!range.getIncludeMaximum() && toCompare.range.getIncludeMaximum())
				return 1;
			if (this.equals(o))
				return 0;
			result = range.getMinimum().compareTo(toCompare.range.getMaximum());
			if (result != 0)
				return result;
			//Give up
			return -1;
		}
		return -1;
	}

	private Version substract(Version v1, Version v2) { //v1 - v2 where v1 is always greater or equals to v2
		int major, minor, micro = 0;
		int carry = 0;
		if (v1.getMicro() < v2.getMicro()) {
			micro = Integer.MAX_VALUE - v2.getMicro() + v1.getMicro();
			carry = 1;
		} else {
			micro = v1.getMicro() - v2.getMicro();
			carry = 0;
		}
		if (v1.getMinor() < v2.getMinor() + carry) {
			minor = Integer.MAX_VALUE - (v2.getMinor() + carry) + v1.getMinor();
			carry = 1;
		} else {
			minor = v1.getMinor() - (v2.getMinor() + carry);
			carry = 0;
		}
		if (v1.getMajor() < v2.getMajor() + carry) {
			major = Integer.MAX_VALUE - (v2.getMajor() + carry) + v1.getMajor();
			carry = 1;
		} else {
			major = v1.getMajor() - (v2.getMajor() + carry);
			carry = 0;
		}
		return new Version(major, minor, micro);
	}

	public boolean equals(Object obj) {
		if (obj instanceof ReachablePlugin) {
			ReachablePlugin toCompare = (ReachablePlugin) obj;
			if (!id.equals(toCompare.id))
				return false;
			if (range.getIncludeMinimum() != toCompare.range.getIncludeMinimum())
				return false;
			if (range.getIncludeMaximum() != toCompare.range.getIncludeMaximum())
				return false;
			return range.getMinimum().equals(toCompare.range.getMinimum()) && range.getMaximum().equals(toCompare.range.getMaximum());
		}
		return false;
	}

	public int hashCode() {
		return id.hashCode() + range.hashCode() * 17;
	}

	public String toString() {
		return id + ' ' + range.toString();
	}
}
