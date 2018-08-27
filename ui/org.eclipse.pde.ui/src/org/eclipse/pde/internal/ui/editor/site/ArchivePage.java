/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.editor.site;

import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.ScrolledForm;

/**
 *
 * Features page.
 */
public class ArchivePage extends PDEFormPage {
	public static final String PAGE_ID = "archives"; //$NON-NLS-1$
	private DescriptionSection fDescSection;
	private MirrorsSection fMirrorsSection;
	private ArchiveSection fArchiveSection;

	public ArchivePage(PDEFormEditor editor) {
		super(editor, PAGE_ID, PDEUIMessages.ArchivePage_name);
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		super.createFormContent(mform);
		ScrolledForm form = mform.getForm();
		form.getBody().setLayout(FormLayoutFactory.createFormGridLayout(false, 1));

		fDescSection = new DescriptionSection(this, form.getBody());
		fArchiveSection = new ArchiveSection(this, form.getBody());
		fMirrorsSection = new MirrorsSection(this, form.getBody());

		mform.addPart(fDescSection);
		mform.addPart(fMirrorsSection);
		mform.addPart(fArchiveSection);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.MANIFEST_SITE_ARCHIVES);
		form.setText(PDEUIMessages.ArchivePage_title);
		form.setImage(PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_JAVA_LIB_OBJ));
	}

	@Override
	protected String getHelpResource() {
		return IHelpContextIds.MANIFEST_SITE_ARCHIVES;
	}
}
