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
package org.eclipse.pde.internal.ui.view;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.graphics.Image;

public class DependenciesLabelProvider extends LabelProvider {
	private PDELabelProvider sharedProvider;

	/**
	 * Constructor for PluginsLabelProvider.
	 */
	public DependenciesLabelProvider() {
		super();
		sharedProvider = PDEPlugin.getDefault().getLabelProvider();
		sharedProvider.connect(this);
	}

	public void dispose() {
		sharedProvider.disconnect(this);
		super.dispose();
	}

	public String getText(Object obj) {
		return sharedProvider.getText(obj);
	}

	public Image getImage(Object obj) {
		return sharedProvider.getImage(obj);
	}
}