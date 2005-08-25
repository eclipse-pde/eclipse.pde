/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.templates;

import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.wizards.plugin.PluginFieldData;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.templates.ITemplateSection;
import org.eclipse.pde.ui.templates.NewPluginTemplateWizard;


public class HelloOSGiNewWizard extends NewPluginTemplateWizard {

	protected PluginFieldData fData;
	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.AbstractNewPluginTemplateWizard#init(org.eclipse.pde.ui.IFieldData)
	 */
	public void init(IFieldData data) {
		super.init(data);
		fData = (PluginFieldData) data;
		setWindowTitle(PDEUIMessages.HelloOSGiNewWizard_title); 
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.NewPluginTemplateWizard#createTemplateSections()
	 */
	public ITemplateSection[] createTemplateSections() {
		return new ITemplateSection[] {new HelloOSGiTemplate(this)};
	}
	
	public String[] getImportPackages() {
		return new String[] {"org.osgi.framework;version=\"1.3.0\""}; //$NON-NLS-1$
	}

}
