/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.provisioner.update;

public class UpdateSiteProvisionerEntry implements IUpdateSiteProvisionerEntry {

	private String installLocation;
	private String siteLocation;

	public UpdateSiteProvisionerEntry(String installLocation, String siteLocation) {
		this.installLocation = installLocation;
		this.siteLocation = siteLocation;
	}

	public String getInstallLocation() {
		return installLocation;
	}

	public String getSiteLocation() {
		return siteLocation;
	}

}
