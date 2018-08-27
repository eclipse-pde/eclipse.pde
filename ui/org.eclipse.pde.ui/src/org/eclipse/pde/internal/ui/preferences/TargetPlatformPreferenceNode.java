/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.preferences;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.preference.IPreferencePage;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.graphics.Image;

public class TargetPlatformPreferenceNode implements IPreferenceNode {
	protected TargetPlatformPreferencePage fPage;

	/**
	 * @see org.eclipse.jface.preference.IPreferenceNode#add(org.eclipse.jface.preference.IPreferenceNode)
	 */
	@Override
	public void add(IPreferenceNode node) {
	}

	/**
	 * @see org.eclipse.jface.preference.IPreferenceNode#createPage()
	 */
	@Override
	public void createPage() {
		fPage = new TargetPlatformPreferencePage();
	}

	/**
	 * @see org.eclipse.jface.preference.IPreferenceNode#disposeResources()
	 */
	@Override
	public void disposeResources() {
		if (fPage != null)
			fPage.dispose();
		fPage = null;
	}

	/**
	 * @see org.eclipse.jface.preference.IPreferenceNode#findSubNode(java.lang.String)
	 */
	@Override
	public IPreferenceNode findSubNode(String id) {
		return null;
	}

	/**
	 * @see org.eclipse.jface.preference.IPreferenceNode#getId()
	 */
	@Override
	public String getId() {
		return "org.eclipse.pde.ui.TargetPlatformPreferencePage"; //$NON-NLS-1$
	}

	/**
	 * @see org.eclipse.jface.preference.IPreferenceNode#getLabelImage()
	 */
	@Override
	public Image getLabelImage() {
		return null;
	}

	/**
	 * @see org.eclipse.jface.preference.IPreferenceNode#getLabelText()
	 */
	@Override
	public String getLabelText() {
		return Platform.getResourceString(PDEPlugin.getDefault().getBundle(), "%preferences.target.name"); //$NON-NLS-1$
	}

	/**
	 * @see org.eclipse.jface.preference.IPreferenceNode#getPage()
	 */
	@Override
	public IPreferencePage getPage() {
		return fPage;
	}

	/**
	 * @see org.eclipse.jface.preference.IPreferenceNode#getSubNodes()
	 */
	@Override
	public IPreferenceNode[] getSubNodes() {
		return new IPreferenceNode[0];
	}

	/**
	 * @see org.eclipse.jface.preference.IPreferenceNode#remove(java.lang.String)
	 */
	@Override
	public IPreferenceNode remove(String id) {
		return null;
	}

	/**
	 * @see org.eclipse.jface.preference.IPreferenceNode#remove(org.eclipse.jface.preference.IPreferenceNode)
	 */
	@Override
	public boolean remove(IPreferenceNode node) {
		return false;
	}
}
