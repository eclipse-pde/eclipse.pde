/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Common Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.pde.internal.ui.wizards.feature;

import org.eclipse.jdt.ui.PreferenceConstants;

public class FeatureData {

	String id;
	String name;
	String version;
	String provider;
	String library;
	boolean hasCustomHandler;

	public FeatureData() {
		library = null;
		hasCustomHandler = false;
	}

	public boolean hasCustomHandler() {
		return hasCustomHandler;
	}

	public String getSourceFolderName() {
		return PreferenceConstants.getPreferenceStore().getString(
				PreferenceConstants.SRCBIN_SRCNAME);
	}

	public String getJavaBuildFolderName() {
		return PreferenceConstants.getPreferenceStore().getString(
				PreferenceConstants.SRCBIN_BINNAME);
	}
}