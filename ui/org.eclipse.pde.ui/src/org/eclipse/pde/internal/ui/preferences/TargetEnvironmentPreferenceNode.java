/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.preferences;

import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.preference.IPreferencePage;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.graphics.Image;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class TargetEnvironmentPreferenceNode implements IPreferenceNode {
	TargetEnvironmentPreferencePage page;

	/**
	 * @see org.eclipse.jface.preference.IPreferenceNode#add(org.eclipse.jface.preference.IPreferenceNode)
	 */
	public void add(IPreferenceNode node) {
	}

	/**
	 * @see org.eclipse.jface.preference.IPreferenceNode#createPage()
	 */
	public void createPage() {
		page = new TargetEnvironmentPreferencePage();
	}

	/**
	 * @see org.eclipse.jface.preference.IPreferenceNode#disposeResources()
	 */
	public void disposeResources() {
		if (page!=null) page.dispose();
		page = null;
	}

	/**
	 * @see org.eclipse.jface.preference.IPreferenceNode#findSubNode(java.lang.String)
	 */
	public IPreferenceNode findSubNode(String id) {
		return null;
	}

	/**
	 * @see org.eclipse.jface.preference.IPreferenceNode#getId()
	 */
	public String getId() {
		return "org.eclipse.pde.ui.TargetEnvironmentPreferencePage"; //$NON-NLS-1$
	}

	/**
	 * @see org.eclipse.jface.preference.IPreferenceNode#getLabelImage()
	 */
	public Image getLabelImage() {
		return null;
	}

	/**
	 * @see org.eclipse.jface.preference.IPreferenceNode#getLabelText()
	 */
	public String getLabelText() {
		return PDEPlugin.getDefault().getDescriptor().getResourceString("%preferences.targetEnv.name"); //$NON-NLS-1$
	}

	/**
	 * @see org.eclipse.jface.preference.IPreferenceNode#getPage()
	 */
	public IPreferencePage getPage() {
		return page;
	}

	/**
	 * @see org.eclipse.jface.preference.IPreferenceNode#getSubNodes()
	 */
	public IPreferenceNode[] getSubNodes() {
		return new IPreferenceNode [0];
	}

	/**
	 * @see org.eclipse.jface.preference.IPreferenceNode#remove(java.lang.String)
	 */
	public IPreferenceNode remove(String id) {
		return null;
	}

	/**
	 * @see org.eclipse.jface.preference.IPreferenceNode#remove(org.eclipse.jface.preference.IPreferenceNode)
	 */
	public boolean remove(IPreferenceNode node) {
		return false;
	}
}
