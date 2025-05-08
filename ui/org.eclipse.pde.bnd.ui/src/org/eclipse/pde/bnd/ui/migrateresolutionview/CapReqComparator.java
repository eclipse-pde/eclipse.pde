/*******************************************************************************
 * Copyright (c) 2015, 2023 bndtools project and others.
 *
* This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Neil Bartlett <njbartlett@gmail.com> - initial API and implementation
 *     BJ Hargrave <bj@hargrave.dev> - ongoing enhancements
 *     Peter Kriens <peter.kriens@aqute.biz> - ongoing enhancements
*******************************************************************************/
package bndtools.tasks;

import java.util.Comparator;

import org.bndtools.core.ui.resource.R5LabelFormatter;
import org.osgi.framework.Version;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

import aQute.bnd.osgi.resource.ResourceUtils;

public class CapReqComparator implements Comparator<Object> {

	@Override
	public int compare(Object o1, Object o2) {
		if (o1 instanceof Requirement)
			return compareReqToObj((Requirement) o1, o2);

		if (o1 instanceof RequirementWrapper)
			return compareReqToObj(((RequirementWrapper) o1).requirement, o2);

		if (o1 instanceof Capability)
			return compareCapToObj((Capability) o1, o2);

		return 0;
	}

	private int compareReqToObj(Requirement r1, Object o2) {
		if (o2 instanceof Requirement)
			return compareReqToReq(r1, (Requirement) o2);

		if (o2 instanceof RequirementWrapper)
			return compareReqToReq(r1, ((RequirementWrapper) o2).requirement);

		// requirements sort before other things
		return -1;
	}

	private int compareCapToObj(Capability c1, Object o2) {
		if (o2 instanceof Capability)
			return compareCapToCap(c1, (Capability) o2);

		// capabilities sort after other things
		return 1;
	}

	private int compareCapToCap(Capability c1, Capability c2) {
		// Compare namespaces
		String ns1 = c1.getNamespace();
		String ns2 = c2.getNamespace();
		int nsDiff = ns1.compareTo(ns2);
		if (nsDiff != 0)
			return nsDiff;

		// Compare the main attribute
		String attribName = R5LabelFormatter.getMainAttributeName(ns1);
		Object attrib1 = c1.getAttributes()
			.get(attribName);
		Object attrib2 = c2.getAttributes()
			.get(attribName);

		if (attrib1 != null && attrib2 != null) {
			int attribDiff = attrib1.toString()
				.compareTo(attrib2.toString());
			if (attribDiff != 0)
				return attribDiff;
		}

		// Compare the versions
		String versionAttribName = R5LabelFormatter.getVersionAttributeName(ns1);
		if (versionAttribName == null)
			return 0;
		Version v1 = (Version) c1.getAttributes()
			.get(versionAttribName);
		if (v1 == null)
			v1 = Version.emptyVersion;
		Version v2 = (Version) c2.getAttributes()
			.get(versionAttribName);
		if (v2 == null)
			v2 = Version.emptyVersion;
		return v1.compareTo(v2);
	}

	private int compareReqToReq(Requirement r1, Requirement r2) {
		return ResourceUtils.REQUIREMENT_COMPARATOR.compare(r1, r2);
	}
}
