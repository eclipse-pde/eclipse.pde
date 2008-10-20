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

	private Long id;
	private String bundle;
	private Long[] usingBundles;
	private String[] classes;

	public ServiceRegistration(RegistryModel model, Long id, String bundle, Long[] usingBundles, String[] classes) {
		super(model);
		this.id = id;
		this.bundle = bundle;
		this.usingBundles = usingBundles;
		this.classes = classes;
	}

	public Long getId() {
		return id;
	}

	public String[] getClasses() {
		return classes;
	}

	public String getBundle() {
		return bundle;
	}

	public Long[] getUsingBundles() {
		return usingBundles;
	}

	public boolean equals(Object obj) {
		return (obj instanceof ServiceRegistration) && (id.equals(((ServiceRegistration) obj).id));
	}

	public int hashCode() {
		return id.intValue();
	}
}
