/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.descriptors;

import org.eclipse.pde.api.tools.internal.provisional.descriptors.IComponentDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;

/**
 * Base implementation of {@link IComponentDescriptor}
 *
 * @since 1.0.1
 *
 * @noextend This class is not intended to be sub-classed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class ComponentDescriptorImpl extends NamedElementDescriptorImpl implements IComponentDescriptor {

	private String componentid = null;
	private String version = null;

	/**
	 * Constructor
	 *
	 * @param componentid
	 */
	public ComponentDescriptorImpl(String componentid, String version) {
		super(componentid);
		this.componentid = componentid;
		this.version = version;

	}

	@Override
	public int getElementType() {
		return COMPONENT;
	}

	@Override
	public int hashCode() {
		int hc = 0;
		if (version != null) {
			hc = version.hashCode();
		}
		return this.componentid.hashCode() + hc;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof IComponentDescriptor) {
			if (this.componentid.equals(((IComponentDescriptor) obj).getId())) {
				if (this.version == null) {
					return ((IComponentDescriptor) obj).getVersion() == null;
				} else {
					return this.version.equals(((IComponentDescriptor) obj).getVersion());
				}
			}
		}
		return false;
	}

	@Override
	public String getId() {
		return this.componentid;
	}

	@Override
	public IElementDescriptor[] getPath() {
		return null;
	}

	@Override
	public String toString() {
		return this.componentid;
	}

	@Override
	public String getVersion() {
		return version;
	}

}
