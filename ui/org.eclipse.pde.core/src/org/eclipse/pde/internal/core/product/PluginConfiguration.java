/*******************************************************************************
 * Copyright (c) 2008, 2009 Code 9 Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Code 9 Corporation - initial API and implementation
 *     Bartosz Michalik <bartosz.michalik@gmail.com> - bug 240737
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 265931     
 *******************************************************************************/
package org.eclipse.pde.internal.core.product;

import java.io.PrintWriter;
import org.eclipse.pde.internal.core.iproduct.IPluginConfiguration;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class PluginConfiguration extends ProductObject implements IPluginConfiguration {

	private static final long serialVersionUID = -3549668957352554876L;
	private boolean fAutoStart;
	private int fStartLevel;
	private String fId;

	/**
	 * Only for parsing usage
	 * @param model
	 */
	PluginConfiguration(IProductModel model) {
		super(model);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProductObject#parse(org.w3c.dom.Node)
	 */
	public void parse(Node node) {
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			Element element = (Element) node;
			fId = element.getAttribute("id"); //$NON-NLS-1$
			fAutoStart = Boolean.valueOf(element.getAttribute(P_AUTO_START)).booleanValue();
			fStartLevel = Integer.parseInt(element.getAttribute(P_START_LEVEL));
		}

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IWritable#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
		writer.print(indent + "<plugin id=\"" + fId + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		writer.print(" autoStart=\"" + fAutoStart + "\""); //$NON-NLS-1$//$NON-NLS-2$
		writer.print(" startLevel=\"" + fStartLevel + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println(" />"); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IPluginConfiguration#getId()
	 */
	public String getId() {
		return fId;
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
