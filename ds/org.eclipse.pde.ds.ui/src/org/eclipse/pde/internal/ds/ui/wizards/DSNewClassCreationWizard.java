/*******************************************************************************
 * Copyright (c) 2008 Code 9 Corporation and others.
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
 *     Chris Aniszczyk <caniszczyk@gmail.com>
 *     Rafael Oliveira Nobrega <rafael.oliveira@gmail.com> - bug 242028
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
