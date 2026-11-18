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

import java.util.Arrays;
import java.util.stream.Collectors;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.PDEPluginImages;

/**
 * Exports workspace bundles (plug-ins and fragments) without generating or
 * running Ant scripts. The export itself is not implemented yet, it only
 * reports the chosen options.
 */
public class BundleExportWizard extends BaseExportWizard {

	private static final String STORE_SECTION = "BundleExportWizard"; //$NON-NLS-1$

	private BundleExportWizardPage fPage;

	public BundleExportWizard() {
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_PLUGIN_EXPORT_WIZ);
	}

	@Override
	public void addPages() {
		fPage = new BundleExportWizardPage(getSelection());
		addPage(fPage);
	}

	@Override
	protected String getSettingsSectionName() {
		return STORE_SECTION;
	}

	@Override
	protected boolean performPreliminaryChecks() {
		return true;
	}

	@Override
	protected boolean confirmDelete() {
		return true;
	}

	@Override
	protected void scheduleExportJob() {
		String bundles = Arrays.stream(fPage.getSelectedItems()).filter(IPluginModelBase.class::isInstance).map(o -> ((IPluginModelBase) o).getPluginBase().getId()).collect(Collectors.joining(", ")); //$NON-NLS-1$
		String message = "Export is not implemented yet.\n" //$NON-NLS-1$
				+ "Bundles: " + bundles + "\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "To directory: " + fPage.doExportToDirectory() + "\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "Destination: " + fPage.getDestination() + "\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "Archive: " + fPage.getFileName() + "\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "JAR format: " + fPage.useJARFormat() + "\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "Export source: " + fPage.doExportSource() + "\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "Qualifier: " + fPage.getQualifier(); //$NON-NLS-1$
		System.out.println(message);
		MessageDialog.openInformation(getContainer().getShell(), getWindowTitle(), message);
	}
}
