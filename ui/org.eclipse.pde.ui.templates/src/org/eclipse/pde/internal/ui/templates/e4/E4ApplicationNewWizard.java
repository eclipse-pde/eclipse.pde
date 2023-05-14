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
 *     Olivier Prouvost <olivier.prouvost@opcoach.com> - initial API and implementation (bug #441331,  466269, 466680)
 *******************************************************************************/
package org.eclipse.pde.internal.ui.templates.e4;

import java.util.List;

import org.eclipse.pde.internal.ui.templates.PDETemplateMessages;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.templates.ITemplateSection;

public class E4ApplicationNewWizard extends AbstractE4NewPluginTemplateWizard {

	private static final List<String> PACKAGE_IMPORTS = List.of( //
			"javax.annotation;version=\"[1.2.0,2.0.0)\"", //$NON-NLS-1$
			"javax.inject;version=\"[1.0.0,2.0.0)\""); //$NON-NLS-1$

	@Override
	public void init(IFieldData data) {
		super.init(data);
		setWindowTitle(PDETemplateMessages.E4ApplicationNewWizard_title);
	}

	@Override
	public ITemplateSection[] createTemplateSections() {
		return new ITemplateSection[] { new E4ApplicationTemplate() };
	}

	@Override
	protected String getFilenameToEdit() {
		return E4ApplicationTemplate.E4_MODEL_FILE;
	}

	@Override
	public String[] getImportPackages() {
		return PACKAGE_IMPORTS.toArray(String[]::new);
	}

}
