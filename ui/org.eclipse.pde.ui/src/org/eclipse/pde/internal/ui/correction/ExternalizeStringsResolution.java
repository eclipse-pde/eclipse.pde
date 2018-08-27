/*******************************************************************************
 *  Copyright (c) 2005, 2018 IBM Corporation and others.
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

	public ExternalizeStringsResolution(int type) {
		super(type);
	}

	@Override
	public void run(final IMarker marker) {
		BusyIndicator.showWhile(SWTUtil.getStandardDisplay(), () -> {
			GetNonExternalizedStringsAction fGetExternAction = new GetNonExternalizedStringsAction();
			IStructuredSelection selection = new StructuredSelection(marker.getResource().getProject());
			fGetExternAction.runGetNonExternalizedStringsAction(selection);
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
