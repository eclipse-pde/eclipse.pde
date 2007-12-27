/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.templates.ide;

import org.eclipse.pde.internal.ui.templates.PDETemplateMessages;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.templates.ITemplateSection;
import org.eclipse.pde.ui.templates.NewPluginTemplateWizard;

public class HelloWorldCmdNewWizard extends NewPluginTemplateWizard {
	/**
	 * Constructor for HelloWorldNewWizard.
	 */
	public HelloWorldCmdNewWizard() {
		super();
	}

	public void init(IFieldData data) {
		super.init(data);
		setWindowTitle(PDETemplateMessages.HelloWorldCmdNewWizard_wtitle);
	}

	/*
	 * @see NewExtensionTemplateWizard#createTemplateSections()
	 */
	public ITemplateSection[] createTemplateSections() {
		return new ITemplateSection[] {new HelloWorldCmdTemplate()};
	}
}
