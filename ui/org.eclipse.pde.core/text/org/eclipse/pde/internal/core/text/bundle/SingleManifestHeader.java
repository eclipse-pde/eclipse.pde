/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text.bundle;

import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.osgi.framework.BundleException;

public class SingleManifestHeader extends ManifestHeader {

	private static final long serialVersionUID = 1L;

	private PDEManifestElement fElement;

	public SingleManifestHeader(String name, String value, IBundle bundle, String lineDelimiter) {
		super(name, value, bundle, lineDelimiter);
	}

	protected void processValue(String value) {
		try {
			ManifestElement[] elements = ManifestElement.parseHeader(getName(), value);
			if (elements.length > 0)
				fElement = new PDEManifestElement(this, elements[0]);
		} catch (BundleException e) {
		}
		if (fElement == null)
			fElement = new PDEManifestElement(this, ""); //$NON-NLS-1$
		fValue = value;
	}

	public void setAttribute(String key, String value) {
		fElement.setAttribute(key, value);
		update();
	}

	public void setDirective(String key, String value) {
		fElement.setDirective(key, value);
		update();
	}

	public void setMainComponent(String value) {
		if (value == null)
			fElement.setValueComponents((String[]) null);
		else
			fElement.setValueComponents(new String[] {value});
		update();
	}

	public String getAttribute(String key) {
		return fElement.getAttribute(key);
	}

	public String getDirective(String key) {
		return fElement.getDirective(key);
	}

	public String getMainComponent() {
		return fElement.getValue();
	}

	public void update() {
		// single headers will fire a change by default
		update(true);
	}

	public void update(boolean notify) {
		String old = fValue;
		fValue = fElement.write();
		if (notify)
			fBundle.getModel().fireModelObjectChanged(this, fName, old, fValue);
	}

}
