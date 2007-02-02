/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.site;

import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.pde.internal.build.IBuildPropertiesConstants;
import org.eclipse.pde.internal.build.IPDEBuildConstants;
import org.eclipse.update.core.*;
import org.osgi.framework.Version;

public class ReachablePlugin implements Comparable {
	private static final Version GENERIC_VERSION = new Version(IPDEBuildConstants.GENERIC_VERSION_NUMBER);
	private static final Version MAX_VERSION = new Version(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
	public static final VersionRange WIDEST_RANGE = new VersionRange(new Version(0, 0, 0), true, MAX_VERSION, true);

	private String id;
	private VersionRange range;

	public ReachablePlugin(String id, VersionRange range) {
		this.id = id;
		this.range = range;
	}

	public ReachablePlugin(IPluginEntry entry) {
		id = entry.getVersionedIdentifier().getIdentifier();
		Version version = new Version(entry.getVersionedIdentifier().getVersion().toString());
		if (version.equals(GENERIC_VERSION)) {
			range = WIDEST_RANGE;
		} else if (version.getQualifier().equals(IBuildPropertiesConstants.PROPERTY_QUALIFIER)) {
			if (version.getMicro() == 0) {
				range = new VersionRange(new Version(version.getMajor(), version.getMinor(), 0), true, new Version(version.getMajor(), version.getMinor() + 1, 0), false);
			} else {
				range = new VersionRange(new Version(version.getMajor(), version.getMinor(), version.getMicro()), true, new Version(version.getMajor(), version.getMinor(), version.getMicro() + 1), false);
			}
		} else {
			range = new VersionRange(version, true, version, true);
		}
	}

	public ReachablePlugin(IImport existingImport) {
		id = existingImport.getVersionedIdentifier().getIdentifier();
		range = constructRange(new Version(existingImport.getVersionedIdentifier().toString()), existingImport.getRule());
	}

	private VersionRange constructRange(Version initialValue, int ruleCode) {
		switch (ruleCode) {
			case IUpdateConstants.RULE_NONE :
			case IUpdateConstants.RULE_EQUIVALENT : //[1.0.0, 1.1.0)
				return new VersionRange(initialValue, true, new Version(initialValue.getMajor(), initialValue.getMinor() + 1, 0), false);

			case IUpdateConstants.RULE_PERFECT : //[1.0.0, 1.0.0]
				return new VersionRange(initialValue, true, initialValue, true);

			case IUpdateConstants.RULE_COMPATIBLE : //[1.1.0, 2.0.0) 
				return new VersionRange(initialValue, true, new Version(initialValue.getMajor() + 1, 0, 0), false);

			case IUpdateConstants.RULE_GREATER_OR_EQUAL ://[1.0.0, 999.999.999)
				return new VersionRange(initialValue, true, new Version(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE), true);
			default :
				return null;
		}

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
			return substract(toCompare.range.getMaximum(), toCompare.range.getMinimum()).compareTo(substract(range.getMaximum(), range.getMinimum()));
		}
		return -1;
	}

	private Version substract(Version v1, Version v2) {
		return new Version(v1.getMajor() - v2.getMajor(), v1.getMinor() - v2.getMinor(), v1.getMicro() - v2.getMicro());
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
