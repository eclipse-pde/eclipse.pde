/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.pde.internal.ui.elements.ElementList;
import org.eclipse.ui.IPluginContribution;

public class WizardCollectionElement extends ElementList implements IPluginContribution {
	private WizardCollectionElement parent;
	private ElementList wizards = new ElementList("wizards"); //$NON-NLS-1$
	private String id;

	// properties
	public static String P_WIZARDS = "org.eclipse.pde.ui.wizards"; //$NON-NLS-1$

	public WizardCollectionElement(String id, String name, WizardCollectionElement parent) {
		super(name, null, parent);
		this.id = id;
	}

	public WizardCollectionElement findChildCollection(IPath searchPath) {
		String searchString = searchPath.segment(0);

		Object[] children = getChildren();
		for (int i = 0; i < children.length; i++) {
			WizardCollectionElement currentCategory = (WizardCollectionElement) children[i];
			if (currentCategory.getLabel().equals(searchString)) {
				if (searchPath.segmentCount() == 1)
					return currentCategory;

				return currentCategory.findChildCollection(searchPath.removeFirstSegments(1));
			}
		}

		return null;
	}

	public WizardElement findWizard(String searchId) {
		Object[] children = getWizards().getChildren();

		for (int i = 0; i < children.length; i++) {
			WizardElement currentWizard = (WizardElement) children[i];
			if (currentWizard.getID().equals(searchId))
				return currentWizard;
		}
		return null;
	}

	public String getId() {
		return id;
	}

	public IPath getPath() {
		if (parent == null)
			return new Path(""); //$NON-NLS-1$

		return parent.getPath().append(getLabel());
	}

	public ElementList getWizards() {
		return wizards;
	}

	public void setId(java.lang.String newId) {
		id = newId;
	}

	public void setWizards(ElementList value) {
		wizards = value;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPluginContribution#getLocalId()
	 */
	public String getLocalId() {
		return getId();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPluginContribution#getPluginId()
	 */
	public String getPluginId() {
		return null;
	}
}
