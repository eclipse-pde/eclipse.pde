/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.registry.model;

public class ServiceRegistration extends ModelObject {

	private long id;
	private String bundle;
	private long[] usingBundles = new long[0];
	private String[] classes = new String[0];

	public void setId(long id) {
		this.id = id;
	}

	public void setBundle(String bundle) {
		this.bundle = bundle;
	}

	public void setUsingBundles(long[] usingBundles) {
		if (usingBundles == null)
			throw new IllegalArgumentException();

		this.usingBundles = usingBundles;
	}

	public void setClasses(String[] classes) {
		if (usingBundles == null)
			throw new IllegalArgumentException();

		this.classes = classes;
	}

	public long getId() {
		return id;
	}

	public String[] getClasses() {
		return classes;
	}

	public String getBundle() {
		return bundle;
	}

	public long[] getUsingBundles() {
		return usingBundles;
	}

	public boolean equals(Object obj) {
		return (obj instanceof ServiceRegistration) && (id == (((ServiceRegistration) obj).id));
	}

	public int hashCode() {
		return (int) id;
	}
}
