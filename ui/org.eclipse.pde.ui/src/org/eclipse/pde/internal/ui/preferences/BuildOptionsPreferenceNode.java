/*
 * Created on Aug 23, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.eclipse.pde.internal.ui.preferences;

import org.eclipse.jface.preference.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.graphics.Image;

/**
 * @author melhem
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class BuildOptionsPreferenceNode implements IPreferenceNode {

	private BuildOptionsPreferencePage page;
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferenceNode#add(org.eclipse.jface.preference.IPreferenceNode)
	 */
	public void add(IPreferenceNode node) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferenceNode#createPage()
	 */
	public void createPage() {
		page = new BuildOptionsPreferencePage();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferenceNode#disposeResources()
	 */
	public void disposeResources() {
		if (page!=null) page.dispose();
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
		return "org.eclipse.pde.ui.buildOptionsPreferencePage";
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
		return PDEPlugin.getDefault().getDescriptor().getResourceString("%preferences.buildOptions.name");
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
