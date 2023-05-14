/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.internal.ua.ui.editor.ctxhelp;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.internal.ua.core.ctxhelp.text.CtxHelpCommand;
import org.eclipse.pde.internal.ua.core.ctxhelp.text.CtxHelpContext;
import org.eclipse.pde.internal.ua.core.ctxhelp.text.CtxHelpDescription;
import org.eclipse.pde.internal.ua.core.ctxhelp.text.CtxHelpTopic;
import org.eclipse.pde.internal.ua.ui.editor.ctxhelp.details.CtxHelpAbstractDetails;
import org.eclipse.pde.internal.ua.ui.editor.ctxhelp.details.CtxHelpCommandDetails;
import org.eclipse.pde.internal.ua.ui.editor.ctxhelp.details.CtxHelpContextDetails;
import org.eclipse.pde.internal.ua.ui.editor.ctxhelp.details.CtxHelpDescriptionDetails;
import org.eclipse.pde.internal.ua.ui.editor.ctxhelp.details.CtxHelpTopicDetails;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDEMasterDetailsBlock;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.DetailsPart;
import org.eclipse.ui.forms.IDetailsPage;
import org.eclipse.ui.forms.IDetailsPageProvider;
import org.eclipse.ui.forms.IManagedForm;

/**
 * Block containing the UI elements for the context editor.  Extends from PDEMasterDetailsBlock
 * which splits the editor into two areas, a tree section and a details section.
 * @since 3.4
 * @see CtxHelpTreeSection
 * @see CtxHelpAbstractDetails
 */
public class CtxHelpBlock extends PDEMasterDetailsBlock implements IModelChangedListener, IDetailsPageProvider {

	private CtxHelpTreeSection fMasterSection;

	public CtxHelpBlock(PDEFormPage page) {
		super(page);
	}

	@Override
	protected PDESection createMasterSection(IManagedForm managedForm, Composite parent) {
		fMasterSection = new CtxHelpTreeSection(getPage(), parent);
		return fMasterSection;
	}

	@Override
	protected void registerPages(DetailsPart detailsPart) {
		// Only static pages to be defined.  Do not cache pages
		detailsPart.setPageLimit(0);
		detailsPart.registerPage(CtxHelpContextDetails.class, new CtxHelpContextDetails(fMasterSection));
		detailsPart.registerPage(CtxHelpDescriptionDetails.class, new CtxHelpDescriptionDetails(fMasterSection));
		detailsPart.registerPage(CtxHelpTopicDetails.class, new CtxHelpTopicDetails(fMasterSection));
		detailsPart.registerPage(CtxHelpCommandDetails.class, new CtxHelpCommandDetails(fMasterSection));
		detailsPart.setPageProvider(this);
	}

	@Override
	public void modelChanged(IModelChangedEvent event) {
		if (fMasterSection != null) {
			fMasterSection.modelChanged(event);
		}
	}

	@Override
	public IDetailsPage getPage(Object key) {
		// No dynamic pages.  Static pages already registered
		return null;
	}

	@Override
	public Object getPageKey(Object object) {
		ISelection selection = getSelection();
		if (!(selection instanceof IStructuredSelection) || ((IStructuredSelection) selection).size() > 1) {
			return object.getClass();
		}

		if (object instanceof CtxHelpContext) {
			return CtxHelpContextDetails.class;
		} else if (object instanceof CtxHelpDescription) {
			return CtxHelpDescriptionDetails.class;
		} else if (object instanceof CtxHelpTopic) {
			return CtxHelpTopicDetails.class;
		} else if (object instanceof CtxHelpCommand) {
			return CtxHelpCommandDetails.class;
		}

		return object.getClass();
	}

	/**
	 * @return the current selection of the tree section
	 */
	public ISelection getSelection() {
		if (fMasterSection != null) {
			return fMasterSection.getSelection();
		}
		return StructuredSelection.EMPTY;
	}

	/**
	 * @return the tree section
	 */
	public CtxHelpTreeSection getMasterSection() {
		return fMasterSection;
	}
}
