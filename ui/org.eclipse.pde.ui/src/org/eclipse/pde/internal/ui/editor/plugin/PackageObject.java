/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 *  Workplace Client Technology - Bundle Developer Kit
 *
 * (C) Copyright IBM Corp. 2003,2004.
 *
 * The source code for this program is not published or other-
 * wise divested of its trade secrets, irrespective of what has
 * been deposited with the U. S. Copyright Office.
 */
package org.eclipse.pde.internal.ui.editor.plugin;


/**
 * @author Sherry Loats (xiaotong@us.ibm.com)
 *
 */
public class PackageObject {
	private String name = null;
	private String version = null;
	
	
	/**
	 * @param name
	 * @param version
	 */
	public PackageObject(String name, String version) {
		super();
		this.name = name;
		this.version = version;
	}

	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return Returns the version.
	 */
	public String getVersion() {
		return version;
	}
	/**
	 * @param version The version to set.
	 */
	public void setVersion(String version) {
		this.version = version;
	}
	
	public String toString()
	{
		if (version != null)
			return (name+" ("+version+")"); //$NON-NLS-1$ //$NON-NLS-2$
		return name;
	}
}
