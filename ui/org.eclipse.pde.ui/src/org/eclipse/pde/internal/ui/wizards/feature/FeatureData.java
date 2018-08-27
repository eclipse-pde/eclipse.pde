/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
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

package org.eclipse.pde.internal.ui.wizards.feature;

import org.eclipse.jdt.ui.PreferenceConstants;

public class FeatureData {

	public String id;
	public String name;
	public String version;
	public String provider;
	public String library;
	public boolean isPatch;
	public String featureToPatchId;
	public String featureToPatchVersion;

	public FeatureData() {
		library = null;
		isPatch = false;
	}

	public boolean hasCustomHandler() {
		return library != null && library.length() > 0;
	}

	public String getSourceFolderName() {
		return PreferenceConstants.getPreferenceStore().getString(PreferenceConstants.SRCBIN_SRCNAME);
	}

	public String getJavaBuildFolderName() {
		return PreferenceConstants.getPreferenceStore().getString(PreferenceConstants.SRCBIN_BINNAME);
	}
}
