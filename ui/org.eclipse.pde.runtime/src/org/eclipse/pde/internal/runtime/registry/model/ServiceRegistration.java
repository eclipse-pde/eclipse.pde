/*******************************************************************************
 *  Copyright (c) 2008, 2019 IBM Corporation and others.
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
 *     Wolfgang Schell <ws@jetztgrad.net> - bug 259348, 260055
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.registry.model;

import java.util.HashSet;
import java.util.Set;

public class ServiceRegistration extends ModelObject implements Comparable<ServiceRegistration> {

	private long id;
	private String bundle;
	private long[] usingBundles = new long[0];
	private ServiceName name;
	private Property[] properties = new Property[0];

	public void setId(long id) {
		this.id = id;
	}

	public void setBundle(String bundle) {
		this.bundle = bundle;
	}

	public void setUsingBundles(long[] usingBundles) {
		if (usingBundles == null) {
			throw new IllegalArgumentException();
		}

		this.usingBundles = usingBundles;
	}

	public void setName(ServiceName name) {
		if (name == null) {
			throw new IllegalArgumentException();
		}

		this.name = name;
	}

	public void setProperties(Property[] properties) {
		if (properties == null) {
			throw new IllegalArgumentException();
		}

		this.properties = properties;
	}

	public long getId() {
		return id;
	}

	public ServiceName getName() {
		return name;
	}

	public String getBundle() {
		return bundle;
	}

	public long[] getUsingBundleIds() {
		return usingBundles;
	}

	public Bundle[] getUsingBundles() {
		if (usingBundles.length == 0 || model == null) {
			return new Bundle[0];
		}

		Set<Bundle> bundles = new HashSet<>();
		for (long usingBundle : usingBundles) {
			Bundle bundle = model.getBundle(Long.valueOf(usingBundle));
			if (bundle != null){
				bundles.add(bundle);
			}
		}
		return bundles.toArray(new Bundle[bundles.size()]);
	}

	public Property[] getProperties() {
		return properties;
	}

	public Property getProperty(String name) {
		for (Property property : properties) {
			if (name.equals(property.getName())) {
				return property;
			}
		}
		return null;
	}

	public static String toString(Object value) {
		if (value == null) {
			return ""; //$NON-NLS-1$
		} else if (value instanceof CharSequence) {
			CharSequence charSequence = (CharSequence) value;
			return charSequence.toString();
		} else if (value instanceof Object[]) {
			StringBuilder buff = new StringBuilder();
			appendString(buff, value);

			return buff.toString();
		} else {
			return value.toString();
		}
	}

	public static void appendString(StringBuilder buff, Object value) {
		if (value == null) {
			// ignore
		} else if (value instanceof Object[]) {
			Object[] objects = (Object[]) value;
			buff.append("["); //$NON-NLS-1$
			for (int o = 0; o < objects.length; o++) {
				Object object = objects[o];
				if (o > 0) {
					buff.append(", "); //$NON-NLS-1$
				}
				appendString(buff, object);
			}
			buff.append("]"); //$NON-NLS-1$
		} else {
			buff.append(value.toString());
		}
	}

	@Override
	public boolean equals(Object obj) {
		return (obj instanceof ServiceRegistration) && (id == (((ServiceRegistration) obj).id));
	}

	@Override
	public int hashCode() {
		return (int) id;
	}

	@Override
	public int compareTo(ServiceRegistration other) {
		return name.compareTo(other.getName());
	}
}
