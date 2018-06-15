/*******************************************************************************
 * Copyright (c) 2018 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ui.tests.smartimport.plugins;

import org.eclipse.reddeer.workbench.workbenchmenu.WorkbenchMenuWizardDialog;

/**
 * Represents SmartImport Wizard (File -> Open Projects from File System...).
 */
public class SmartImportWizard extends WorkbenchMenuWizardDialog {

	public SmartImportWizard() {
		super("Import Projects from File System or Archive", "File", "Open Projects from File System...");
	}
}
