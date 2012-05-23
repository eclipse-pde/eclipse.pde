/*******************************************************************************
 *  Copyright (c) 2005, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
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

	public void run(final IMarker marker) {
		BusyIndicator.showWhile(SWTUtil.getStandardDisplay(), new Runnable() {
			public void run() {
				GetNonExternalizedStringsAction fGetExternAction = new GetNonExternalizedStringsAction();
				IStructuredSelection selection = new StructuredSelection(marker.getResource().getProject());
				fGetExternAction.runGetNonExternalizedStringsAction(selection);
			}
		});
	}

	protected void createChange(IBaseModel model) {
		// nothin to do - all handled by run
	}

	public String getDescription() {
		return PDEUIMessages.ExternalizeStringsResolution_desc;
	}

	public String getLabel() {
		return PDEUIMessages.ExternalizeStringsResolution_label;
	}

}
