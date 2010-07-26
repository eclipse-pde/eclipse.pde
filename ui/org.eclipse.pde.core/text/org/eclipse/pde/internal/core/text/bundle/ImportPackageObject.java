/*******************************************************************************
 *  Copyright (c) 2005, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text.bundle;

import java.io.PrintWriter;

import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.bundle.BundlePluginBase;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

public class ImportPackageObject extends PackageObject {

	private static final long serialVersionUID = 1L;

	private static String getVersion(ExportPackageDescription desc) {
		String version = desc.getVersion().toString();
		if (!version.equals(Version.emptyVersion.toString()))
			return desc.getVersion().toString();
		return null;
	}

	public ImportPackageObject(ManifestHeader header, ManifestElement element, String versionAttribute) {
		super(header, element, versionAttribute);
	}

	public ImportPackageObject(ManifestHeader header, ExportPackageDescription desc, String versionAttribute) {
		super(header, desc.getName(), getVersion(desc), versionAttribute);
	}

	public ImportPackageObject(ManifestHeader header, String id, String version, String versionAttribute) {
		super(header, id, version, versionAttribute);
	}

	public boolean isOptional() {
		int manifestVersion = BundlePluginBase.getBundleManifestVersion(getHeader().getBundle());
		if (manifestVersion > 1)
			return Constants.RESOLUTION_OPTIONAL.equals(getDirective(Constants.RESOLUTION_DIRECTIVE));
		return "true".equals(getAttribute(ICoreConstants.OPTIONAL_ATTRIBUTE)); //$NON-NLS-1$
	}

	public void setOptional(boolean optional) {
		boolean old = isOptional();
		int manifestVersion = BundlePluginBase.getBundleManifestVersion(getHeader().getBundle());
		if (optional) {
			if (manifestVersion > 1)
				setDirective(Constants.RESOLUTION_DIRECTIVE, Constants.RESOLUTION_OPTIONAL);
			else
				setAttribute(ICoreConstants.OPTIONAL_ATTRIBUTE, "true"); //$NON-NLS-1$
		} else {
			setDirective(Constants.RESOLUTION_DIRECTIVE, null);
			setAttribute(ICoreConstants.OPTIONAL_ATTRIBUTE, null);
		}
		fHeader.update();
		firePropertyChanged(this, Constants.RESOLUTION_DIRECTIVE, Boolean.toString(old), Boolean.toString(optional));
	}

	/**
	 * @param model
	 * @param header
	 * @param versionAttribute
	 */
	public void reconnect(IBundleModel model, ImportPackageHeader header, String versionAttribute) {
		super.reconnect(model, header, versionAttribute);
		// No transient fields
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.bundle.BundleObject#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
		// Used for text transfers for copy, cut, paste operations
		writer.write(write());
	}

	public void restoreProperty(String propertyName, Object oldValue, Object newValue) {
		if (Constants.RESOLUTION_DIRECTIVE.equalsIgnoreCase(propertyName)) {
			setOptional(new Boolean(newValue.toString()).booleanValue());
		} else if (fVersionAttribute != null && fVersionAttribute.equalsIgnoreCase(propertyName)) {
			setVersion(newValue.toString());
		}
	}

}
