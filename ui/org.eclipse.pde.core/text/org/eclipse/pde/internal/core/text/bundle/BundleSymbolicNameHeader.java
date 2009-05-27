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

import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.osgi.framework.Constants;

public class BundleSymbolicNameHeader extends SingleManifestHeader {

	private static final long serialVersionUID = 1L;

	public BundleSymbolicNameHeader(String name, String value, IBundle bundle, String lineDelimiter) {
		super(name, value, bundle, lineDelimiter);
	}

	public void setId(String id) {
		setMainComponent(id);
	}

	public String getId() {
		return getMainComponent();
	}

	public void setSingleton(boolean singleton) {
		if (getManifestVersion() > 1) {
			setDirective(Constants.SINGLETON_DIRECTIVE, singleton ? Boolean.toString(true) : null);
			if (getAttribute(Constants.SINGLETON_DIRECTIVE) != null)
				setAttribute(Constants.SINGLETON_DIRECTIVE, null);
		} else {
			setAttribute(Constants.SINGLETON_DIRECTIVE, singleton ? Boolean.toString(true) : null);
			if (getDirective(Constants.SINGLETON_DIRECTIVE) != null)
				setDirective(Constants.SINGLETON_DIRECTIVE, null);
		}
	}

	public boolean isSingleton() {
		String value = getManifestVersion() > 1 ? getDirective(Constants.SINGLETON_DIRECTIVE) : getAttribute(Constants.SINGLETON_DIRECTIVE);
		return "true".equals(value); //$NON-NLS-1$
	}

	public void fixUnsupportedDirective() {
		String value = getDirective(Constants.SINGLETON_DIRECTIVE);
		if (value != null) {
			setAttribute(Constants.SINGLETON_DIRECTIVE, value);
			setDirective(Constants.SINGLETON_DIRECTIVE, null);
		}
	}

}
