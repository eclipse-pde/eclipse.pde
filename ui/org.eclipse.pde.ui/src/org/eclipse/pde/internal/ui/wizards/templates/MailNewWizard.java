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

import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.ui.*;
import org.eclipse.pde.ui.templates.*;


public class MailNewWizard extends NewPluginTemplateWizard {

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.AbstractNewPluginTemplateWizard#init(org.eclipse.pde.ui.IFieldData)
	 */
	public void init(IFieldData data) {
		super.init(data);
		setWindowTitle(PDEPlugin.getResourceString("MailNewWizard.title")); //$NON-NLS-1$
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.NewPluginTemplateWizard#createTemplateSections()
	 */
	public ITemplateSection[] createTemplateSections() {
		return new ITemplateSection[] {new MailTemplate()};
	}

}
