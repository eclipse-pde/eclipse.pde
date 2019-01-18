/*******************************************************************************
 * Copyright (c) 2003, 2012 IBM Corporation and others.
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
package org.eclipse.pde.internal.core.bundle;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.ManifestHeader;
import org.eclipse.pde.internal.core.util.HeaderMap;
import org.osgi.framework.Constants;

public class Bundle extends BundleObject implements IBundle {
	private static final long serialVersionUID = 1L;
	private final Map<String, IManifestHeader> fDocumentHeaders = new HeaderMap<>();

	@Override
	public void setHeader(String key, String value) {
		if (value == null) {
			// Do a remove
			IManifestHeader header = fDocumentHeaders.remove(key);
			if (header != null) {
				getModel().fireModelObjectChanged(header, key, header.getValue(), null);
			}
		} else {
			// Edit an existing header value or create a new header object
			IManifestHeader header = fDocumentHeaders.get(key);
			if (header == null) {
				header = getModel().getFactory().createHeader(key, value);
				fDocumentHeaders.put(key, header);
				getModel().fireModelObjectChanged(header, key, null, value);
			} else {
				String old = header.getValue();
				header.setValue(value);
				getModel().fireModelObjectChanged(header, key, old, value);
			}
		}
	}

	@Override
	public String getHeader(String key) {
		ManifestHeader header = (ManifestHeader) fDocumentHeaders.get(key);
		return (header != null) ? header.getValue() : null;
	}

	/**
	 * Load a map of String key value pairs into the list of known manifest headers.  Any
	 * headers previously loaded will be cleared. Empty value strings will create empty headers.
	 * Null values will be ignored.
	 *
	 * @param headers map<String, String> of manifest key and values
	 */
	public void load(Map<?, ?> headers) {
		fDocumentHeaders.clear();
		Iterator<?> iter = headers.keySet().iterator();
		while (iter.hasNext()) {
			String key = iter.next().toString();
			if (headers.get(key) != null) {
				String value = headers.get(key).toString();
				IManifestHeader header = getModel().getFactory().createHeader(key.toString(), value);
				header.update(); // Format the headers, unknown if this step is necessary for new header objects
				fDocumentHeaders.put(key.toString(), header);
			}
		}
	}

	@Override
	public String getLocalization() {
		String localization = getHeader(Constants.BUNDLE_LOCALIZATION);
		return localization != null ? localization : Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME;
	}

	@Override
	public void renameHeader(String key, String newKey) {
		ManifestHeader header = (ManifestHeader) getManifestHeader(key);
		if (header != null) {
			header.setName(newKey);
			fDocumentHeaders.put(newKey, fDocumentHeaders.remove(key));
		}
		getModel().fireModelObjectChanged(header, newKey, key, newKey);
	}

	@Override
	public IManifestHeader getManifestHeader(String key) {
		return fDocumentHeaders.get(key);
	}

	@Override
	public Map<String, IManifestHeader> getManifestHeaders() {
		HeaderMap<String, IManifestHeader> copy = new HeaderMap<>();
		copy.putAll(fDocumentHeaders);
		return copy;
	}

	/**
	 * @return a map containing all key/value pairs of manifest headers as strings, values may be empty strings, but not <code>null</code>
	 */
	protected Map<String, String> getHeaders() {
		Map<String, String> result = new HashMap<>(fDocumentHeaders.values().size());
		for (IManifestHeader header : fDocumentHeaders.values()) {
			if (header.getValue() != null) {
				result.put(header.getKey(), header.getValue());
			}
		}
		return result;
	}
}
