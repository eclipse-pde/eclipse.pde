/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.plugin;

import org.eclipse.pde.ui.IFragmentFieldData;

public class FragmentFieldData extends AbstractFieldData implements IFragmentFieldData {

	private String fPluginId;
	private String fPluginVersion;
	private int fMatch;

	@Override
	public String getPluginId() {
		return fPluginId;
	}

	@Override
	public String getPluginVersion() {
		return fPluginVersion;
	}

	@Override
	public int getMatch() {
		return fMatch;
	}

	public void setPluginId(String id) {
		fPluginId = id;
	}

	public void setPluginVersion(String version) {
		fPluginVersion = version;
	}

	public void setMatch(int match) {
		fMatch = match;
	}
}
