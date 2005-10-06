/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.model.bundle;

import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.osgi.framework.BundleException;

public class LazyStartHeader extends ManifestHeader {

	private static final long serialVersionUID = 1L;
	private boolean fLazy;
	private String fRemaining;

	public LazyStartHeader(String name, String value, IBundle bundle, String lineDelimiter) {
		super(name, value, bundle, lineDelimiter);
		processValue();
	}
	
	private void processValue() {
		try {
			ManifestElement[] elements = ManifestElement.parseHeader(fName, fValue);
			if (elements.length > 0) {
				fLazy = "true".equals(elements[0].getValue()); //$NON-NLS-1$
				int index = fValue.indexOf(';');
				if (index != -1)
					fRemaining = fValue.substring(index);
			}
		} catch (BundleException e) {
		}
	}

	public boolean isLazyStart() {
		return fLazy;
	}
	
	public void setLazyStart(boolean lazy) {
		fLazy = lazy;
		String old = fValue;
		updateValue();
		firePropertyChanged(this, fName, old, fValue);
	}
	
	public void updateValue() {
		fValue = Boolean.toString(fLazy);
		if (fRemaining != null)
			fValue += fRemaining;
	}
	
}
