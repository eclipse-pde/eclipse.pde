/*******************************************************************************
 * Copyright (c) 2026 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.exports;

/**
 * Export options of the {@link BundleExportWizard}. In contrast to the legacy
 * export it has no Ant specific options.
 */
public class BundleExportOptionsTab extends ExportOptionsTab {

	public BundleExportOptionsTab(BaseExportWizardPage page) {
		super(page);
	}
}
