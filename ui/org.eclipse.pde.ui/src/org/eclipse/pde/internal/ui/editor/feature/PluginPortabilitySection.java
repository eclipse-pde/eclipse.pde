/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.feature;

import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

public class PluginPortabilitySection extends DataPortabilitySection {
	private static final String SECTION_DESC = "FeatureEditor.PluginPortabilitySection.desc"; //$NON-NLS-1$

	private static final String SECTION_TITLE = "FeatureEditor.PluginPortabilitySection.title"; //$NON-NLS-1$

	public PluginPortabilitySection(PDEFormPage page, Composite parent) {
		super(page, parent, PDEPlugin.getResourceString(SECTION_TITLE),
				PDEPlugin.getResourceString(SECTION_DESC), SWT.NULL);
	}

}
