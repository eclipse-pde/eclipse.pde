/*******************************************************************************
 *  Copyright (c) 2005, 2019 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.nls.GetNonExternalizedStringsAction;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.custom.BusyIndicator;

public class ExternalizeStringsResolution extends AbstractPDEMarkerResolution {

	private boolean hasRun = false;

	public ExternalizeStringsResolution(int type, IMarker marker) {
		super(type, marker);
	}

	@Override
	public void run(final IMarker marker) {
		// even for multiple error markers, this wizard must be run only once
		if (hasRun)
			return;
		BusyIndicator.showWhile(SWTUtil.getStandardDisplay(), () -> {
			GetNonExternalizedStringsAction fGetExternAction = new GetNonExternalizedStringsAction();
			IStructuredSelection selection = new StructuredSelection(marker.getResource().getProject());
			fGetExternAction.runGetNonExternalizedStringsAction(selection);
			hasRun = true;
		});
	}

	@Override
	protected void createChange(IBaseModel model) {
		// nothin to do - all handled by run
	}

	@Override
	public String getDescription() {
		return PDEUIMessages.ExternalizeStringsResolution_desc;
	}

	@Override
	public String getLabel() {
		return PDEUIMessages.ExternalizeStringsResolution_label;
	}

}
