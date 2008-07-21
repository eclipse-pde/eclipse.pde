/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Rafael Oliveira Nóbrega <rafael.oliveira@gmail.com> - bug 239501
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.wizards;

import org.eclipse.core.resources.IProject;
import org.eclipse.pde.internal.ui.editor.schema.NewClassCreationWizard;

public class DSNewClassCreationWizard extends NewClassCreationWizard {

	public DSNewClassCreationWizard(IProject project, boolean isInterface,
			String value) {
		super(project, isInterface, value);
	}

}
