/*
 * Created on Aug 2, 2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.launcher;

import org.eclipse.jdt.internal.debug.ui.jres.JREsPreferencePage;
import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.preference.IPreferencePage;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.graphics.Image;

/**
 * @author melhem
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class InstalledJREsPreferenceNode implements IPreferenceNode {
	
	private JREsPreferencePage page;

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferenceNode#add(org.eclipse.jface.preference.IPreferenceNode)
	 */
	public void add(IPreferenceNode node) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferenceNode#createPage()
	 */
	public void createPage() {
		page = new JREsPreferencePage();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferenceNode#disposeResources()
	 */
	public void disposeResources() {
		if (page != null)
			page.dispose();
		page = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferenceNode#findSubNode(java.lang.String)
	 */
	public IPreferenceNode findSubNode(String id) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferenceNode#getId()
	 */
	public String getId() {
		return "org.eclipse.jdt.debug.ui.preferences.VMPreferencePage"; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferenceNode#getLabelImage()
	 */
	public Image getLabelImage() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferenceNode#getLabelText()
	 */
	public String getLabelText() {
		return PDEPlugin.getResourceString("BasicLauncherTab.jrePreferencePage"); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferenceNode#getPage()
	 */
	public IPreferencePage getPage() {
		return page;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferenceNode#getSubNodes()
	 */
	public IPreferenceNode[] getSubNodes() {
		return new IPreferenceNode[0];
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferenceNode#remove(java.lang.String)
	 */
	public IPreferenceNode remove(String id) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferenceNode#remove(org.eclipse.jface.preference.IPreferenceNode)
	 */
	public boolean remove(IPreferenceNode node) {
		return false;
	}

}
