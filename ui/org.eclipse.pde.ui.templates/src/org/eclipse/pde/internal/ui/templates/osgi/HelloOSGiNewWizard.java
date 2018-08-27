/*******************************************************************************
 *  Copyright (c) 2005, 2007 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.templates.osgi;

import org.eclipse.pde.internal.ui.templates.PDETemplateMessages;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.templates.ITemplateSection;
import org.eclipse.pde.ui.templates.NewPluginTemplateWizard;

public class HelloOSGiNewWizard extends NewPluginTemplateWizard {

	@Override
	public void init(IFieldData data) {
		super.init(data);
		setWindowTitle(PDETemplateMessages.HelloOSGiNewWizard_title);
	}

	@Override
	public ITemplateSection[] createTemplateSections() {
		return new ITemplateSection[] {new HelloOSGiTemplate()};
	}

	@Override
	public String[] getImportPackages() {
		return new String[] {"org.osgi.framework;version=\"1.3.0\""}; //$NON-NLS-1$
	}

}
