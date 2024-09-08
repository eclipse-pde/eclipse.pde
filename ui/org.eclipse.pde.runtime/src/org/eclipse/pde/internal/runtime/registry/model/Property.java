/*******************************************************************************
 * Copyright (c) 2008, 2019 IBM Corporation and others.
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
 *     Wolfgang Schell <ws@jetztgrad.net> - bug 260055
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.registry.model;

import org.osgi.framework.Constants;

public class Property extends ModelObject implements Comparable<Property> {
	public static final String PREFIX_SERVICE = "service."; //$NON-NLS-1$
	public static final String PREFIX_COMPONENT = "component."; //$NON-NLS-1$

	private String name;
	private String value;

	public Property() {
		// empty
	}

	// TODO should we merge this with Attribute somehow?
	public Property(String name, String value) {
		this.name = name;
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * Compare properties for order. Returns a negative integer, zero, or a
	 * positive integer as the first argument is less than, equal to, or greater
	 * than the second.
	 *
	 * <p>
	 * The {@link Property}s are compared according to the following rules:
	 * </p>
	 * <ul>
	 * <li>objectClass is always less than everything else</li>
	 * <li>properties with names starting with "service." are considered "less"
	 * than other properties.</li>
	 * <li>regular properties are considered "more" than other properties</li>
	 * </ul>
	 *
	 * <p>
	 * When sorting an array of properties with the following code:
	 * </p>
	 *
	 * <pre>
	 * Property[] properties = ...;
	 * Arrays.sort(properties, PropertyComparator.INSTANCE);
	 * </pre>
	 *
	 * the result is something like this:
	 * <ul>
	 * <li>objectClass</li>
	 * <li>service.id</li>
	 * <li>service.id</li>
	 * </ul>
	 *
	 * @param other
	 *            other property to be compared against
	 *
	 * @return a negative integer, zero, or a positive integer as the first
	 *         argument is less than, equal to, or greater than the second.
	 */
	@Override
	public int compareTo(Property other) {
		String name0 = getName();
		String name1 = other.getName();

		if (Constants.OBJECTCLASS.equals(name0)) {
			return -1;
		}

		if (Constants.OBJECTCLASS.equals(name1)) {
			return 1;
		}

		if (name0.startsWith(PREFIX_COMPONENT) && name1.startsWith(PREFIX_COMPONENT)) {
			// both are service properties
			// simply compare them
			return name0.compareTo(name1);
		}

		if (name0.startsWith(PREFIX_COMPONENT)) {
			return -1;
		}

		if (name1.startsWith(PREFIX_COMPONENT)) {
			return 1;
		}

		if (name0.startsWith(PREFIX_SERVICE) && name1.startsWith(PREFIX_SERVICE)) {
			// both are service properties
			// simply compare them
			return name0.compareTo(name1);
		}

		if (name0.startsWith(PREFIX_SERVICE)) {
			return -1;
		}

		if (name1.startsWith(PREFIX_SERVICE)) {
			return 1;
		}

		// simply compare strings
		return name0.compareTo(name1);
	}

}
