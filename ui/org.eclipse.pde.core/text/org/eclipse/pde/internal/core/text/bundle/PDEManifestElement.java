/*******************************************************************************
 *  Copyright (c) 2003, 2017 IBM Corporation and others.
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
 *******************************************************************************/

package org.eclipse.pde.internal.core.text.bundle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.internal.core.bundle.BundleObject;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.osgi.framework.BundleException;

public class PDEManifestElement extends BundleObject {

	private static final long serialVersionUID = 1L;

	protected String[] fValueComponents;
	protected TreeMap<String, Serializable> fAttributes;
	protected TreeMap<String, Serializable> fDirectives;

	protected transient ManifestHeader fHeader;

	public PDEManifestElement(ManifestHeader header, String value) {
		setHeader(header);
		setValue(value);
		setModel(fHeader.getBundle().getModel());
	}

	protected PDEManifestElement(ManifestHeader header, ManifestElement manifestElement) {
		setHeader(header);
		init(manifestElement);
		setModel(fHeader.getBundle().getModel());
	}

	public String[] getValueComponents() {
		return fValueComponents;
	}

	protected void setValueComponents(String[] valueComponents) {
		fValueComponents = valueComponents;
	}

	public String[] getAttributes(String key) {
		return getTableValues(fAttributes, key);
	}

	public String getAttribute(String key) {
		return getTableValue(fAttributes, key);
	}

	public Set<String> getKeys() {
		return getTableKeys(fAttributes);
	}

	public void addAttribute(String key, String value) {
		fAttributes = addTableValue(fAttributes, key, value);
	}

	public void setAttribute(String key, String value) {
		fAttributes = setTableValue(fAttributes, key, value);
	}

	public String getDirective(String key) {
		return getTableValue(fDirectives, key);
	}

	public String[] getDirectives(String key) {
		return getTableValues(fDirectives, key);
	}

	public Set<String> getDirectiveKeys() {
		return getTableKeys(fDirectives);
	}

	public void addDirective(String key, String value) {
		fDirectives = addTableValue(fDirectives, key, value);
	}

	public void setDirective(String key, String value) {
		fDirectives = setTableValue(fDirectives, key, value);
	}

	private String getTableValue(TreeMap<String, Serializable> table, String key) {
		if (table == null) {
			return null;
		}
		Object result = table.get(key);
		if (result == null) {
			return null;
		}
		if (result instanceof String) {
			return (String) result;
		}

		ArrayList<?> valueList = (ArrayList<?>) result;
		//return the last value
		return (String) valueList.get(valueList.size() - 1);
	}

	private String[] getTableValues(TreeMap<String, Serializable> table, String key) {
		if (table == null) {
			return null;
		}
		Object result = table.get(key);
		if (result == null) {
			return null;
		}
		if (result instanceof String) {
			return new String[] {(String) result};
		}
		ArrayList<?> valueList = (ArrayList<?>) result;
		return valueList.toArray(new String[valueList.size()]);
	}

	private Set<String> getTableKeys(TreeMap<String, Serializable> table) {
		if (table == null) {
			return null;
		}
		return table.keySet();
	}

	@SuppressWarnings("unchecked")
	private TreeMap<String, Serializable> addTableValue(TreeMap<String, Serializable> table, String key, String value) {
		if (table == null) {
			table = new TreeMap<>();
		}
		Object curValue = table.get(key);
		if (curValue != null) {
			ArrayList<Object> newList;
			// create a list to contain multiple values
			if (curValue instanceof ArrayList) {
				newList = (ArrayList<Object>) curValue;
			} else {
				newList = new ArrayList<>(5);
				newList.add(curValue);
			}
			newList.add(value);
			table.put(key, newList);
		} else {
			table.put(key, value);
		}
		return table;
	}

	private TreeMap<String, Serializable> setTableValue(TreeMap<String, Serializable> table, String key, String value) {
		if (table == null) {
			table = new TreeMap<>();
		}
		if (value == null || value.trim().length() == 0) {
			table.remove(key);
		} else {
			table.put(key, value);
		}
		return table;
	}

	public void setValue(String value) {
		if (value == null) {
			setValueComponents(new String[0]);
			return;
		}
		try {
			ManifestElement[] elements = ManifestElement.parseHeader(fHeader.fName, value);
			if (elements != null && elements.length > 0) {
				init(elements[0]);
			}
		} catch (BundleException e) {
		}
	}

	private void init(ManifestElement manifestElement) {
		setValueComponents(manifestElement.getValueComponents());
		Enumeration<?> attKeys = manifestElement.getKeys();
		if (attKeys != null) {
			while (attKeys.hasMoreElements()) {
				String attKey = (String) attKeys.nextElement();
				String[] values = ManifestElement.getArrayFromList(manifestElement.getAttribute(attKey));
				//empty string in attribute, go with default behavior of attribute
				if (values == null) {
					continue;
				}
				for (String value : values) {
					addAttribute(attKey, value);
				}
			}
		}
		Enumeration<?> dirKeys = manifestElement.getDirectiveKeys();
		if (dirKeys != null) {
			while (dirKeys.hasMoreElements()) {
				String dirKey = (String) dirKeys.nextElement();
				String[] values = ManifestElement.getArrayFromList(manifestElement.getDirective(dirKey));
				for (String value : values) {
					addDirective(dirKey, value);
				}
			}
		}
	}

	public String write() {
		StringBuilder sb = new StringBuilder(getValue());
		appendValuesToBuffer(sb, fAttributes);
		appendValuesToBuffer(sb, fDirectives);
		return sb.toString();
	}

	public String getValue() {
		StringBuilder sb = new StringBuilder();
		if (fValueComponents == null) {
			return ""; //$NON-NLS-1$
		}
		for (int i = 0; i < fValueComponents.length; i++) {
			if (i != 0) {
				sb.append("; "); //$NON-NLS-1$
			}
			sb.append(fValueComponents[i]);
		}
		return sb.toString();
	}

	protected void appendValuesToBuffer(StringBuilder sb, TreeMap<String, Serializable> table) {
		if (table == null) {
			return;
		}
		for (Entry<String, Serializable> entry : table.entrySet()) {
			String dkey = entry.getKey();
			Object value = entry.getValue();
			if (value == null) {
				continue;
			}
			sb.append(";"); //$NON-NLS-1$
			sb.append(dkey);
			sb.append(table.equals(fDirectives) ? ":=" : "="); //$NON-NLS-1$ //$NON-NLS-2$

			if (value instanceof String) {
				boolean wrap = shouldWrap(value.toString());
				if (wrap) {
					sb.append("\""); //$NON-NLS-1$
				}
				sb.append(value);
				if (wrap) {
					sb.append("\""); //$NON-NLS-1$
				}
			} else if (value instanceof ArrayList) {
				ArrayList<?> values = (ArrayList<?>) value;
				boolean wrap = (values.size() > 1 || (values.size() == 1 && shouldWrap(values.get(0).toString())));
				if (wrap) {
					sb.append("\""); //$NON-NLS-1$
				}
				for (int i = 0; i < values.size(); i++) {
					if (i != 0) {
						sb.append(","); //$NON-NLS-1$
					}
					sb.append(values.get(i));
				}
				if (wrap) {
					sb.append("\""); //$NON-NLS-1$
				}
			}
		}
	}

	private boolean shouldWrap(String value) {
		return value.indexOf(' ') != -1 || value.indexOf(',') != -1 || value.indexOf('.') != -1 || value.indexOf('[') != -1 || value.indexOf('(') != -1;
	}

	public ManifestHeader getHeader() {
		return fHeader;
	}

	public void setHeader(ManifestHeader header) {
		fHeader = header;
	}

	/**
	 * @param model
	 * @param header
	 */
	public void reconnect(IBundleModel model, ManifestHeader header) {
		super.reconnect(model);
		// Transient Field:  Header
		fHeader = header;
	}

}
