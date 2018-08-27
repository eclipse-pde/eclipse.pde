/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 507831
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

	@Override
	public void update(ViewerCell cell) {
		Object element = cell.getElement();
		cell.setText(getText(element));
		cell.setImage(getImage(element));
	}

	@Override
	public Image getImage(Object element) {
		ILabelProvider provider = Platform.getAdapterManager().getAdapter(element, ILabelProvider.class);
		if (provider != null) {
			return provider.getImage(element);
		}

		// TODO If all locations are set up to use adapters, we could log an error here

		return super.getImage(element);
	}

	@Override
	public String getText(Object element) {
		ILabelProvider provider = Platform.getAdapterManager().getAdapter(element, ILabelProvider.class);
		if (provider != null) {
			return provider.getText(element);
		}

		// TODO If all locations are set up to use adapters, we could log an error here

		return super.getText(element);
	}

}