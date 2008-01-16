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
package org.eclipse.pde.internal.ui.correction.java;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.text.bundle.ExportPackageHeader;
import org.eclipse.pde.internal.core.text.bundle.ExportPackageObject;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.ModelModification;
import org.eclipse.pde.internal.ui.util.PDEModelUtility;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.osgi.framework.Constants;

public class ForbiddenAccessProposal implements IJavaCompletionProposal {

	private IProject fProject;
	private IPackageFragment fFragment;

	public ForbiddenAccessProposal(IPackageFragment fragment, IProject project) {
		fProject = project;
		fFragment = fragment;
	}

	public void apply(IDocument document) {
		ModelModification mod = new ModelModification(fProject) {
			protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
				if (model instanceof IBundlePluginModelBase) {
					IBundle bundle = ((IBundlePluginModelBase) model).getBundleModel().getBundle();

					ExportPackageHeader header = (ExportPackageHeader) bundle.getManifestHeader(Constants.EXPORT_PACKAGE);
					if (header == null) {
						bundle.setHeader(Constants.EXPORT_PACKAGE, ""); //$NON-NLS-1$
						header = (ExportPackageHeader) bundle.getManifestHeader(Constants.EXPORT_PACKAGE);
					}
					header.addPackage(new ExportPackageObject(header, fFragment, Constants.VERSION_ATTRIBUTE));
				}
			}
		};
		PDEModelUtility.modifyModel(mod, new NullProgressMonitor());
	}

	public String getDisplayString() {
		return NLS.bind(PDEUIMessages.ForbiddenAccessProposal_quickfixMessage, new String[] {fFragment.getElementName(), fProject.getName()});
	}

	public Image getImage() {
		return PDEPluginImages.get(PDEPluginImages.OBJ_DESC_BUNDLE);
	}

	public int getRelevance() {
		return 100;
	}

	public String getAdditionalProposalInfo() {
		return null;
	}

	public IContextInformation getContextInformation() {
		return null;
	}

	public Point getSelection(IDocument document) {
		return null;
	}
}
