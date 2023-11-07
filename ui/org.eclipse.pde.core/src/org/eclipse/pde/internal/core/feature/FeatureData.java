/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 322975
 *******************************************************************************/
package org.eclipse.pde.internal.core.feature;

import java.io.File;
import java.io.PrintWriter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.XMLPrintHandler;
import org.eclipse.pde.internal.core.ifeature.IFeatureData;
import org.w3c.dom.Node;

public class FeatureData extends IdentifiableObject implements IFeatureData {
	private static final long serialVersionUID = 1L;
	private String os;
	private String ws;
	private String nl;
	private String arch;
	private String filter;

	public FeatureData() {
	}

	@Override
	protected void reset() {
		super.reset();
		os = null;
		ws = null;
		nl = null;
		arch = null;
	}

	@Override
	public boolean exists() {
		String location = getModel().getInstallLocation();
		if (location.startsWith("file:")) { //$NON-NLS-1$
			location = location.substring(5);
		}
		File file = new File(location + File.separator + getId());
		return file.exists();
	}

	@Override
	protected void parse(Node node) {
		super.parse(node);
		os = getNodeAttribute(node, "os"); //$NON-NLS-1$
		ws = getNodeAttribute(node, "ws"); //$NON-NLS-1$
		nl = getNodeAttribute(node, "nl"); //$NON-NLS-1$
		arch = getNodeAttribute(node, "arch"); //$NON-NLS-1$
		filter = getNodeAttribute(node, "filter"); //$NON-NLS-1$
	}

	protected void writeAttributes(String indent2, PrintWriter writer) {
		writeAttribute("id", getId(), indent2, writer); //$NON-NLS-1$
		writeAttribute("os", getOS(), indent2, writer); //$NON-NLS-1$
		writeAttribute("ws", getWS(), indent2, writer); //$NON-NLS-1$
		writeAttribute("nl", getNL(), indent2, writer); //$NON-NLS-1$
		writeAttribute("arch", getArch(), indent2, writer); //$NON-NLS-1$
		writeAttribute("filter", getFilter(), indent2, writer); //$NON-NLS-1$
	}

	private void writeAttribute(String attribute, String value, String indent2, PrintWriter writer) {
		if (value != null && value.length() > 0) {
			writer.println();
			writer.print(indent2);
			writer.print(attribute);
			writer.print("=\""); //$NON-NLS-1$
			writer.print(XMLPrintHandler.encode(value));
			writer.print("\""); //$NON-NLS-1$
		}
	}

	@Override
	public void write(String indent, PrintWriter writer) {
		writer.print(indent + "<data"); //$NON-NLS-1$
		String indent2 = indent + Feature.INDENT + Feature.INDENT;
		writeAttributes(indent2, writer);
		writer.println("/>"); //$NON-NLS-1$
		//writer.println(indent + "</data>");
	}

	/**
	 * Gets the os.
	 * @return Returns a String
	 */
	@Override
	public String getOS() {
		return os;
	}

	/**
	 * Sets the os.
	 * @param os The os to set
	 */
	@Override
	public void setOS(String os) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.os;
		this.os = os;
		firePropertyChanged(P_OS, oldValue, os);
	}

	/**
	 * Gets the ws.
	 * @return Returns a String
	 */
	@Override
	public String getWS() {
		return ws;
	}

	/**
	 * Sets the ws.
	 * @param ws The ws to set
	 */
	@Override
	public void setWS(String ws) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.ws;
		this.ws = ws;
		firePropertyChanged(P_WS, oldValue, ws);
	}

	/**
	 * Gets the nl.
	 * @return Returns a String
	 */
	@Override
	public String getNL() {
		return nl;
	}

	/**
	 * Sets the nl.
	 * @param nl The nl to set
	 */
	@Override
	public void setNL(String nl) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.nl;
		this.nl = nl;
		firePropertyChanged(P_NL, oldValue, nl);
	}

	/**
	 * Gets the arch.
	 * @return Returns a String
	 */
	@Override
	public String getArch() {
		return arch;
	}

	/**
	 * Sets the arch.
	 * @param arch The arch to set
	 */
	@Override
	public void setArch(String arch) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.arch;
		this.arch = arch;
		firePropertyChanged(P_ARCH, oldValue, arch);
	}

	/**
	 * Get the LDAP filter
	 * @return the filter or null
	 */
	@Override
	public String getFilter() {
		return filter;
	}

	/** Set the LDAP filter
	 * @param filter The filter to set
	 */
	@Override
	public void setFilter(String filter) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.filter;
		this.filter = filter;
		firePropertyChanged(P_FILTER, oldValue, filter);
	}

	@Override
	public void restoreProperty(String name, Object oldValue, Object newValue) throws CoreException {
		switch (name) {
		case P_OS:
			setOS((String) newValue);
			break;
		case P_WS:
			setWS((String) newValue);
			break;
		case P_NL:
			setNL((String) newValue);
			break;
		case P_ARCH:
			setArch((String) newValue);
			break;
		default:
			super.restoreProperty(name, oldValue, newValue);
			break;
		}
	}

	@Override
	public String getLabel() {
		return getId();
	}

	@Override
	public String toString() {
		return getLabel();
	}
}
