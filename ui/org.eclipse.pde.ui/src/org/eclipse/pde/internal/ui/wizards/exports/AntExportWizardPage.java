/*******************************************************************************
 * Copyright (c) 2005, 2026 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Christoph Läubrich - extracted from ExportOptionsTab and BaseExportWizardPage
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.exports;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.ui.PDEUIMessages;

/**
 * Base for the pages of the legacy, Ant based export wizards. It offers the
 * option to save the export as an Ant script.
 */
public abstract class AntExportWizardPage extends BaseExportWizardPage {

	public AntExportWizardPage(IStructuredSelection selection, String name, String choiceLabel) {
		super(selection, name, choiceLabel);
	}

	@Override
	protected ExportOptionsTab createExportOptionsTab() {
		return new AntExportOptionsTab(this);
	}

	@Override
	protected String getWarningMessage() {
		if (fOptionsTab instanceof AntExportOptionsTab antTab) {
			String path = antTab.getAntBuildFileName();
			if (path != null && path.length() > 0 && "build.xml".equals(IPath.fromOSString(path).lastSegment())) { //$NON-NLS-1$
				return PDEUIMessages.ExportOptionsTab_antReservedMessage;
			}
		}
		return null;
	}

	protected boolean doGenerateAntFile() {
		return fOptionsTab instanceof AntExportOptionsTab antTab && antTab.doGenerateAntFile();
	}

	protected String getAntBuildFileName() {
		return fOptionsTab instanceof AntExportOptionsTab antTab ? antTab.getAntBuildFileName() : null;
	}
}
