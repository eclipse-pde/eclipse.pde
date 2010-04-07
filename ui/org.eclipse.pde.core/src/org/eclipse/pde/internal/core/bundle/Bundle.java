/*******************************************************************************
 * Copyright (c) 2003, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.bundle;

import java.util.Iterator;
import java.util.Map;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.ManifestHeader;
import org.eclipse.pde.internal.core.util.HeaderMap;
import org.osgi.framework.Constants;

public class Bundle extends BundleObject implements IBundle {
	private static final long serialVersionUID = 1L;
	private Map fProperties;

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.ibundle.IBundle#setHeader(java.lang.String, java.lang.String)
	 */
	public void setHeader(String key, String value) {
		if (fProperties == null)
			fProperties = new HeaderMap();//TreeMap(new HeaderComparator());
		Object oldValue = fProperties.get(key);
		// an empty string removes the header whereas a non-zero length string with spaces is used to generate an empty header
		if (value == null || value.length() == 0)
			fProperties.remove(key);
		else
			fProperties.put(key, value);
		getModel().fireModelObjectChanged(this, key, oldValue, value);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.ibundle.IBundle#getHeader(java.lang.String)
	 */
	public String getHeader(String key) {
		if (fProperties == null) {
			return null;
		}
		return (String) fProperties.get(key);
	}

	public void load(Map properties) {
		// Passed dictionary is read-only
		fProperties = new HeaderMap();//TreeMap(new HeaderComparator());
		Iterator it = properties.keySet().iterator();
		while (it.hasNext()) {
			Object o = it.next();
			fProperties.put(o, properties.get(o));
		}
	}

	public String getLocalization() {
		String localization = getHeader(Constants.BUNDLE_LOCALIZATION);
		return localization != null ? localization : Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME;
	}

	public void setLocalization(String localization) {
		setHeader(Constants.BUNDLE_LOCALIZATION, localization);
	}

	public void renameHeader(String key, String newKey) {
		if (fProperties == null)
			fProperties = new HeaderMap();//TreeMap(new HeaderComparator());
		if (fProperties.get(key) != null) {
			fProperties.put(newKey, fProperties.remove(key));
		}
	}

	public IManifestHeader getManifestHeader(String key) {
		return new ManifestHeader(key, getHeader(key), this, System.getProperty("line.separator")); //$NON-NLS-1$
	}

	protected Map getHeaders() {
		return fProperties;
	}
}
