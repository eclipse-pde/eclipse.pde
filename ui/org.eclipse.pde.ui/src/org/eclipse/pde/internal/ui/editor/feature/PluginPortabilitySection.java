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
package org.eclipse.pde.internal.ui.editor.feature;

import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

public class PluginPortabilitySection extends DataPortabilitySection {
	public PluginPortabilitySection(PDEFormPage page, Composite parent) {
		super(page, parent, PDEUIMessages.FeatureEditor_PluginPortabilitySection_title, PDEUIMessages.FeatureEditor_PluginPortabilitySection_desc, SWT.NULL);
	}

}
