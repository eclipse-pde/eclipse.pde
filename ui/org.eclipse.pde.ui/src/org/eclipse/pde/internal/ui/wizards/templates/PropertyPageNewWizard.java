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
package org.eclipse.pde.internal.ui.wizards.templates;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.ui.*;
import org.eclipse.pde.ui.templates.*;

public class PropertyPageNewWizard extends NewPluginTemplateWizard {
	private static final String KEY_WTITLE = "PropertyPageNewWizard.wtitle";
	/**
	 * Constructor for PropertyPageNewWizard.
	 */
	public PropertyPageNewWizard() {
		super();
	}

	public void init(
		IProjectProvider provider,
		IPluginStructureData structureData,
		boolean fragment,
		IConfigurationElement config) {
		super.init(provider, structureData, fragment, config);
		setWindowTitle(PDEPlugin.getResourceString(KEY_WTITLE));
	}
	/**
	 * @see NewPluginTemplateWizard#createTemplateSections()
	 */
	public ITemplateSection[] createTemplateSections() {
		return new ITemplateSection[] { new PropertyPageTemplate()};
	}

}
