/*******************************************************************************
 * Copyright (c) 2008, 2009 Code 9 Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Code 9 Corporation - initial API and implementation
 *     Rafael Oliveira Nobrega <rafael.oliveira@gmail.com> - bug 244558
 *******************************************************************************/
package org.eclipse.pde.internal.ui.templates.osgi;

import org.eclipse.pde.core.plugin.IPluginReference;
import org.eclipse.pde.internal.ui.templates.PDETemplateMessages;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.templates.ITemplateSection;
import org.eclipse.pde.ui.templates.NewPluginTemplateWizard;

public class HelloServiceComponentTemplateWizard extends NewPluginTemplateWizard {

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.AbstractNewPluginTemplateWizard#init(org.eclipse.pde.ui.IFieldData)
	 */
	public void init(IFieldData data) {
		super.init(data);
		setWindowTitle(PDETemplateMessages.DSTemplateWizard_title);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.NewPluginTemplateWizard#createTemplateSections()
	 */
	public ITemplateSection[] createTemplateSections() {
		return new ITemplateSection[] {new HelloServiceComponentTemplate()};
	}

	public String[] getImportPackages() {
		return new String[] {"org.osgi.framework;version=\"1.3.0\"", "org.osgi.util.tracker;version=\"1.3.1\"", "org.eclipse.osgi.framework.console;version=\"1.0.0\""}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public IPluginReference[] getDependencies(String schemaVersion) {
		return new IPluginReference[0];
	}

}
