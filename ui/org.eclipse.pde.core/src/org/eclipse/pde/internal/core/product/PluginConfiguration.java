/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Bartosz Michalik (bartosz.michalik@gmail.com)
 *******************************************************************************/
package org.eclipse.pde.internal.core.product;

import java.io.PrintWriter;
import java.util.*;
import org.eclipse.pde.internal.core.iproduct.IPluginConfiguration;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.w3c.dom.*;

public class PluginConfiguration extends ProductObject implements IPluginConfiguration {

	private static final long serialVersionUID = -3549668957352554876L;
	private boolean fAutoStart;
	private int fStartLevel;
	private String fId;
	private Map fPropertiesMap;

	/**
	 * Only for parsing usage
	 * @param model
	 */
	PluginConfiguration(IProductModel model) {
		super(model);
		fPropertiesMap = new HashMap();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProductObject#parse(org.w3c.dom.Node)
	 */
	public void parse(Node node) {
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			Element element = (Element) node;
			fId = element.getAttribute("id"); //$NON-NLS-1$
			fAutoStart = Boolean.getBoolean((element.getAttribute(P_AUTO_START)));
			fStartLevel = Integer.parseInt(element.getAttribute(P_START_LEVEL));
			NodeList children = node.getChildNodes();
			int length = children.getLength();
			for (int i = 0; i < length; ++i) {
				Node item = children.item(i);
				if (item.getNodeType() == Node.ELEMENT_NODE) {
					element = (Element) item;
					String key = element.getAttribute("key"); //$NON-NLS-1$
					String value = element.getAttribute("value"); //$NON-NLS-1$
					fPropertiesMap.put(key, value);
				}
			}
		}

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IWritable#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
		writer.print(indent + "<plugin id=\"" + fId + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		writer.print(" autoStart=\"" + fAutoStart + "\""); //$NON-NLS-1$//$NON-NLS-2$
		writer.print(" startLevel=\"" + fStartLevel + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		if (fPropertiesMap.isEmpty())
			writer.println(" />"); //$NON-NLS-1$
		else {
			writer.println(" >"); //$NON-NLS-1$
			Iterator i = fPropertiesMap.keySet().iterator();
			while (i.hasNext()) {
				String key = (String) i.next();
				writer.println(indent + "<property key=\"" + key + " value=\"" + fPropertiesMap.get(key) + "\" />"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			writer.println("</plugin>"); //$NON-NLS-1$
		}

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IPluginConfiguration#addProperty(java.lang.String, java.lang.String)
	 */
	public void addProperty(String key, String value) throws IllegalArgumentException {
		if (key == null || key.equals("")) //$NON-NLS-1$
			throw new IllegalArgumentException("key cannot empty"); //$NON-NLS-1$
		if (value == null || value.equals("")) //$NON-NLS-1$
			throw new IllegalArgumentException("value cannot empty"); //$NON-NLS-1$
		String oldValue = (String) fPropertiesMap.get(key);
		fPropertiesMap.put(key, value);
		if (isEditable() && !value.equals(oldValue))
			firePropertyChanged(key, oldValue, value);

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IPluginConfiguration#getId()
	 */
	public String getId() {
		return fId;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IPluginConfiguration#getProperty(java.lang.String)
	 */
	public String getProperty(String key) {
		return (String) fPropertiesMap.get(key);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IPluginConfiguration#getStartLevel()
	 */
	public int getStartLevel() {
		return fStartLevel;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IPluginConfiguration#isAutoStart()
	 */
	public boolean isAutoStart() {
		return fAutoStart;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IPluginConfiguration#removeProperty(java.lang.String)
	 */
	public void removeProperty(String key) {
		String oldValue = (String) fPropertiesMap.get(key);
		fPropertiesMap.remove(key);
		if (isEditable() && oldValue != null)
			firePropertyChanged(key, oldValue, null);

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IPluginConfiguration#setAutoStart(boolean)
	 */
	public void setAutoStart(boolean autostart) {
		boolean oldValue = fAutoStart;
		fAutoStart = autostart;
		if (isEditable() && oldValue != fAutoStart)
			firePropertyChanged(P_AUTO_START, new Boolean(oldValue), new Boolean(fAutoStart));

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IPluginConfiguration#setStartLevel(java.lang.String)
	 */
	public void setStartLevel(int startLevel) {
		int oldValue = fStartLevel;
		fStartLevel = startLevel;
		if (isEditable() && oldValue != fStartLevel)
			firePropertyChanged(P_START_LEVEL, new Integer(oldValue), new Integer(fStartLevel));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProductPluginConfiguration#setId(java.lang.String)
	 */
	public void setId(String id) {
		fId = id;
	}

}
