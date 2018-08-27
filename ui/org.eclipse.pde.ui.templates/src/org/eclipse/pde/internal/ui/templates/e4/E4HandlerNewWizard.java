/*******************************************************************************
 * Copyright (c) 2015 OPCoach
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Olivier Prouvost <olivier.prouvost@opcoach.com> - initial API and implementation (bug #481340)
 *******************************************************************************/
package org.eclipse.pde.internal.ui.templates.e4;

import org.eclipse.pde.internal.ui.templates.PDETemplateMessages;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.templates.ITemplateSection;

public class E4HandlerNewWizard extends AbstractE4NewPluginTemplateWizard {

	@Override
	public void init(IFieldData data) {
		super.init(data);
		setWindowTitle(PDETemplateMessages.E4HandlerNewWizard_wtitle);
	}

	@Override
	public ITemplateSection[] createTemplateSections() {
		return new ITemplateSection[] { new E4HandlerTemplate() };
	}

	@Override
	protected String getFilenameToEdit() {
		return E4HandlerTemplate.E4_FRAGMENT_FILE;
	}

	@Override
	public String[] getImportPackages() {
		return new String[] {"javax.annotation;version=\"1.2.0\""}; //$NON-NLS-1$
	}

}
