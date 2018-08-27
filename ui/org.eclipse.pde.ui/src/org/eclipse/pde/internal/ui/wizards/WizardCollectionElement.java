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
		for (Object child : children) {
			WizardCollectionElement currentCategory = (WizardCollectionElement) child;
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

		for (Object child : children) {
			WizardElement currentWizard = (WizardElement) child;
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

	@Override
	public String getLocalId() {
		return getId();
	}

	@Override
	public String getPluginId() {
		return null;
	}
}
