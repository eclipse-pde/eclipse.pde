/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.templates;

import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.ui.*;
import org.eclipse.pde.ui.templates.*;

public class EditorNewWizard extends NewPluginTemplateWizard {
	private static final String KEY_WTITLE = "EditorNewWizard.wtitle"; //$NON-NLS-1$

	/**
	 * Constructor for EditorNewWizard.
	 */
	public EditorNewWizard() {
		super();
	}
	public void init(IFieldData data) {
		super.init(data);
		setWindowTitle(PDEPlugin.getResourceString(KEY_WTITLE));
	}

	/*
	 * @see NewExtensionTemplateWizard#createTemplateSections()
	 */
	public ITemplateSection[] createTemplateSections() {
		return new ITemplateSection[] { new EditorTemplate()};
	}
}
