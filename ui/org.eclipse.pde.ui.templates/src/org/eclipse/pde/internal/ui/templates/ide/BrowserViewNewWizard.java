/*******************************************************************************
 *  Copyright (c) 2000, 2021 IBM Corporation and others and others.
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
 *     Wim Jongman - Create browser view plugin
 *******************************************************************************/

package org.eclipse.pde.internal.ui.templates.ide;

import org.eclipse.pde.internal.ui.templates.PDETemplateMessages;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.templates.ITemplateSection;
import org.eclipse.pde.ui.templates.NewPluginTemplateWizard;

public class BrowserViewNewWizard extends NewPluginTemplateWizard {
	/**
	 * Constructor for ViewNewWizard.
	 */
	public BrowserViewNewWizard() {
		super();
	}

	@Override
	public void init(IFieldData data) {
		super.init(data);
		setWindowTitle(PDETemplateMessages.BrowserView_newPlugin);
	}

	@Override
	public ITemplateSection[] createTemplateSections() {
		return new ITemplateSection[] { new BrowserViewTemplate() };
	}
}
