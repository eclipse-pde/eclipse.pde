/*******************************************************************************
 * Copyright (c) 2008, 2016 Code 9 Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Code 9 Corporation - initial API and implementation
 *     Rafael Oliveira Nobrega <rafael.oliveira@gmail.com> - bug 244558
 *     Peter Nehrer <pnehrer@eclipticalsoftware.com> - bug 486245
 *******************************************************************************/
package org.eclipse.pde.internal.ui.templates.osgi;

import org.eclipse.pde.core.plugin.IPluginReference;
import org.eclipse.pde.internal.ui.templates.PDETemplateMessages;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.templates.ITemplateSection;
import org.eclipse.pde.ui.templates.NewPluginTemplateWizard;

public class HelloServiceComponentTemplateWizard extends NewPluginTemplateWizard {

	@Override
	public void init(IFieldData data) {
		super.init(data);
		setWindowTitle(PDETemplateMessages.DSTemplateWizard_title);
	}

	@Override
	public ITemplateSection[] createTemplateSections() {
		return new ITemplateSection[] {new HelloServiceComponentTemplate()};
	}

	@Override
	public String[] getImportPackages() {
		return new String[] {"org.osgi.framework;version=\"1.3.0\"", "org.osgi.util.tracker;version=\"1.3.1\"", "org.eclipse.osgi.framework.console;version=\"1.0.0\"", "org.osgi.service.component.annotations;version=\"1.2.0\";resolution:=\"optional\""}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	@Override
	public IPluginReference[] getDependencies(String schemaVersion) {
		return new IPluginReference[0];
	}

}
