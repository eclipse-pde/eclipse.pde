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
package org.eclipse.pde.internal.ua.ui.editor.toc;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.internal.ua.core.toc.text.Toc;
import org.eclipse.pde.internal.ua.core.toc.text.TocAnchor;
import org.eclipse.pde.internal.ua.core.toc.text.TocLink;
import org.eclipse.pde.internal.ua.core.toc.text.TocTopic;
import org.eclipse.pde.internal.ua.ui.editor.toc.details.TocAbstractDetails;
import org.eclipse.pde.internal.ua.ui.editor.toc.details.TocAnchorDetails;
import org.eclipse.pde.internal.ua.ui.editor.toc.details.TocDetails;
import org.eclipse.pde.internal.ua.ui.editor.toc.details.TocLinkDetails;
import org.eclipse.pde.internal.ua.ui.editor.toc.details.TocTopicDetails;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDEMasterDetailsBlock;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.DetailsPart;
import org.eclipse.ui.forms.IDetailsPage;
import org.eclipse.ui.forms.IDetailsPageProvider;
import org.eclipse.ui.forms.IManagedForm;

public class TocBlock extends PDEMasterDetailsBlock implements IModelChangedListener, IDetailsPageProvider {

	private TocTreeSection fMasterSection;

	private TocAbstractDetails fDetails;

	private TocAbstractDetails fTopicDetails;

	private TocAnchorDetails fAnchorDetails;

	private TocLinkDetails fLinkDetails;

	public TocBlock(PDEFormPage page) {
		super(page);
	}

	@Override
	protected PDESection createMasterSection(IManagedForm managedForm, Composite parent) {
		fMasterSection = new TocTreeSection(getPage(), parent);
		return fMasterSection;
	}

	@Override
	protected void registerPages(DetailsPart detailsPart) {
		// Only static pages to be defined.  Do not cache pages
		detailsPart.setPageLimit(0);
		// Register static page:  toc
		fDetails = new TocDetails(fMasterSection);
		detailsPart.registerPage(TocDetails.class, fDetails);
		// Register static page:  tocTopic
		fTopicDetails = new TocTopicDetails(fMasterSection);
		detailsPart.registerPage(TocTopicDetails.class, fTopicDetails);
		// Register static page:  tocAnchor
		fAnchorDetails = new TocAnchorDetails(fMasterSection);
		detailsPart.registerPage(TocAnchorDetails.class, fAnchorDetails);
		// Register static page:  tocLink
		fLinkDetails = new TocLinkDetails(fMasterSection);
		detailsPart.registerPage(TocLinkDetails.class, fLinkDetails);
		// Set this class as the page provider
		detailsPart.setPageProvider(this);
	}

	@Override
	public void modelChanged(IModelChangedEvent event) {
		// Inform the master section
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

		// Get static page key
		if (object instanceof Toc) {
			// Static page:  toc
			return TocDetails.class;
		} else if (object instanceof TocTopic) {
			// Static page:  tocTopic
			return TocTopicDetails.class;
		} else if (object instanceof TocAnchor) {
			// Static page:  tocAnchor
			return TocAnchorDetails.class;
		} else if (object instanceof TocLink) {
			// Static page:  tocLink
			return TocLinkDetails.class;
		}

		// Should never reach here
		return object.getClass();
	}

	public ISelection getSelection() {
		if (fMasterSection != null) {
			return fMasterSection.getSelection();
		}
		return StructuredSelection.EMPTY;
	}

	public TocTreeSection getMasterSection() {
		return fMasterSection;
	}
}
