package org.eclipse.pde.internal.core.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.w3c.dom.Node;
import java.io.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.ui.model.ifeature.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.core.resources.IResource;

public class FeatureData
	extends IdentifiableObject
	implements IFeatureData {
	private String os;
	private String ws;
	private String nl;
	private String arch;
	private int downloadSize;
	private int installSize;

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
		if (location.startsWith("file:"))
		   location = location.substring(5);
		File file = new File(location + File.separator+getId());
		return file.exists();
	}

	protected void parse(Node node) {
		super.parse(node);
		os = getNodeAttribute(node, "os");
		ws = getNodeAttribute(node, "ws");
		nl = getNodeAttribute(node, "nl");
		arch = getNodeAttribute(node, "arch");
		downloadSize = getIntegerAttribute(node, "download-size");
		installSize = getIntegerAttribute(node, "install-size");
	}
	protected void writeAttributes(String indent2, PrintWriter writer) {
		if (getId() != null) {
			writer.println();
			writer.print(indent2 + "id=\"" + getId() + "\"");
		}
		if (getOS() != null) {
			writer.println();
			writer.print(indent2 + "os=\"" + getOS() + "\"");
		}
		if (getWS() != null) {
			writer.println();
			writer.print(indent2 + "ws=\"" + getWS() + "\"");
		}
		if (getNL() != null) {
			writer.println();
			writer.print(indent2 + "nl=\"" + getNL() + "\"");
		}
		if (getArch() != null) {
			writer.println();
			writer.print(indent2 + "arch=\"" + getArch() + "\"");
		}
		writer.println();
		writer.print(indent2 + "download-size=\"" + getDownloadSize() + "\"");
		writer.println();
		writer.print(indent2 + "install-size=\"" + getInstallSize() + "\"");
	}

	public void write(String indent, PrintWriter writer) {
		writer.print(indent + "<data");
		String indent2 = indent + Feature.INDENT + Feature.INDENT;
		writeAttributes(indent2, writer);
		writer.println(">");
		writer.println(indent + "</data>");
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
	 * Gets the downloadSize.
	 * @return Returns a int
	 */
	public int getDownloadSize() {
		return downloadSize;
	}

	/**
	 * Sets the downloadSize.
	 * @param downloadSize The downloadSize to set
	 */
	public void setDownloadSize(int downloadSize) throws CoreException {
		ensureModelEditable();
		Object oldValue = new Integer(this.downloadSize);
		this.downloadSize = downloadSize;
		firePropertyChanged(P_DOWNLOAD_SIZE, oldValue, new Integer(downloadSize));
	}

	/**
	 * Gets the installSize.
	 * @return Returns a int
	 */
	public int getInstallSize() {
		return installSize;
	}

	/**
	 * Sets the installSize.
	 * @param installSize The installSize to set
	 */
	public void setInstallSize(int installSize) throws CoreException {
		ensureModelEditable();
		Object oldValue = new Integer(this.installSize);
		this.installSize = installSize;
		firePropertyChanged(P_DOWNLOAD_SIZE, oldValue, new Integer(installSize));
	}

	public String getLabel() {
		return getId();
	}
	
	public String toString() {
		return getLabel();
	}
}