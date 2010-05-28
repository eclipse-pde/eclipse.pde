/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.feature;

import java.io.File;
import java.io.PrintWriter;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.ifeature.IFeatureData;
import org.w3c.dom.Node;

public class FeatureData extends IdentifiableObject implements IFeatureData {
	private static final long serialVersionUID = 1L;
	private String os;
	private String ws;
	private String nl;
	private String arch;
	private String filter;
	private long downloadSize;
	private long installSize;

	public FeatureData() {
	}

	protected void reset() {
		super.reset();
		os = null;
		ws = null;
		nl = null;
		arch = null;
		downloadSize = 0;
		installSize = 0;
	}

	public boolean exists() {
		String location = getModel().getInstallLocation();
		if (location.startsWith("file:")) //$NON-NLS-1$
			location = location.substring(5);
		File file = new File(location + File.separator + getId());
		return file.exists();
	}

	protected void parse(Node node) {
		super.parse(node);
		os = getNodeAttribute(node, "os"); //$NON-NLS-1$
		ws = getNodeAttribute(node, "ws"); //$NON-NLS-1$
		nl = getNodeAttribute(node, "nl"); //$NON-NLS-1$
		arch = getNodeAttribute(node, "arch"); //$NON-NLS-1$
		filter = getNodeAttribute(node, "filter"); //$NON-NLS-1$
		downloadSize = getIntegerAttribute(node, "download-size"); //$NON-NLS-1$
		installSize = getIntegerAttribute(node, "install-size"); //$NON-NLS-1$
	}

	protected void writeAttributes(String indent2, PrintWriter writer) {
		writeAttribute("id", getId(), indent2, writer); //$NON-NLS-1$
		writeAttribute("os", getOS(), indent2, writer); //$NON-NLS-1$
		writeAttribute("ws", getWS(), indent2, writer); //$NON-NLS-1$
		writeAttribute("nl", getNL(), indent2, writer); //$NON-NLS-1$
		writeAttribute("arch", getArch(), indent2, writer); //$NON-NLS-1$
		writeAttribute("filter", getFilter(), indent2, writer); //$NON-NLS-1$
		writer.println();
		writer.print(indent2 + "download-size=\"" + getDownloadSize() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println();
		writer.print(indent2 + "install-size=\"" + getInstallSize() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private void writeAttribute(String attribute, String value, String indent2, PrintWriter writer) {
		if (value != null && value.length() > 0) {
			writer.println();
			writer.print(indent2);
			writer.print(attribute);
			writer.print("=\""); //$NON-NLS-1$
			writer.print(value);
			writer.print("\""); //$NON-NLS-1$
		}
	}

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
	public String getOS() {
		return os;
	}

	/**
	 * Sets the os.
	 * @param os The os to set
	 */
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
	public String getWS() {
		return ws;
	}

	/**
	 * Sets the ws.
	 * @param ws The ws to set
	 */
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
	public String getNL() {
		return nl;
	}

	/**
	 * Sets the nl.
	 * @param nl The nl to set
	 */
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
	public String getArch() {
		return arch;
	}

	/**
	 * Sets the arch.
	 * @param arch The arch to set
	 */
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
	public String getFilter() {
		return filter;
	}

	/** Set the LDAP filter
	 * @param filter The filter to set
	 */
	public void setFilter(String filter) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.filter;
		this.filter = filter;
		firePropertyChanged(P_FILTER, oldValue, filter);
	}

	/**
	 * Gets the downloadSize.
	 * @return Returns a int
	 */
	public long getDownloadSize() {
		return downloadSize;
	}

	/**
	 * Sets the downloadSize.
	 * @param downloadSize The downloadSize to set
	 */
	public void setDownloadSize(long downloadSize) throws CoreException {
		ensureModelEditable();
		Object oldValue = new Long(this.downloadSize);
		this.downloadSize = downloadSize;
		firePropertyChanged(P_DOWNLOAD_SIZE, oldValue, new Long(downloadSize));
	}

	/**
	 * Gets the installSize.
	 * @return Returns a int
	 */
	public long getInstallSize() {
		return installSize;
	}

	/**
	 * Sets the installSize.
	 * @param installSize The installSize to set
	 */
	public void setInstallSize(long installSize) throws CoreException {
		ensureModelEditable();
		Object oldValue = new Long(this.installSize);
		this.installSize = installSize;
		firePropertyChanged(P_INSTALL_SIZE, oldValue, new Long(installSize));
	}

	public void restoreProperty(String name, Object oldValue, Object newValue) throws CoreException {
		if (name.equals(P_OS)) {
			setOS((String) newValue);
		} else if (name.equals(P_WS)) {
			setWS((String) newValue);
		} else if (name.equals(P_NL)) {
			setNL((String) newValue);
		} else if (name.equals(P_ARCH)) {
			setArch((String) newValue);
		} else if (name.equals(P_DOWNLOAD_SIZE)) {
			setDownloadSize(newValue != null ? ((Integer) newValue).intValue() : 0);
		} else if (name.equals(P_INSTALL_SIZE)) {
			setInstallSize(newValue != null ? ((Integer) newValue).intValue() : 0);
		} else
			super.restoreProperty(name, oldValue, newValue);
	}

	public String getLabel() {
		return getId();
	}

	public String toString() {
		return getLabel();
	}
}
