/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.shared.target;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;

/**
 * Label provider for the tree.  Will try to adapt the element to an ILabelProvider to create
 * the text and image.
 */
public class TargetLocationLabelProvider extends StyledBundleLabelProvider {

	public TargetLocationLabelProvider(boolean showVersion, boolean appendResolvedVariables) {
		super(showVersion, appendResolvedVariables);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.StyledCellLabelProvider#update(org.eclipse.jface.viewers.ViewerCell)
	 */
	public void update(ViewerCell cell) {
		Object element = cell.getElement();
		cell.setText(getText(element));
		cell.setImage(getImage(element));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.shared.target.StyledBundleLabelProvider#getImage(java.lang.Object)
	 */
	public Image getImage(Object element) {
		ILabelProvider provider = (ILabelProvider) Platform.getAdapterManager().getAdapter(element, ILabelProvider.class);
		if (provider != null) {
			return provider.getImage(element);
		}

		// TODO If all locations are set up to use adapters, we could log an error here

		return super.getImage(element);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.shared.target.StyledBundleLabelProvider#getText(java.lang.Object)
	 */
	public String getText(Object element) {
		ILabelProvider provider = (ILabelProvider) Platform.getAdapterManager().getAdapter(element, ILabelProvider.class);
		if (provider != null) {
			return provider.getText(element);
		}

		// TODO If all locations are set up to use adapters, we could log an error here

		return super.getText(element);
	}

}