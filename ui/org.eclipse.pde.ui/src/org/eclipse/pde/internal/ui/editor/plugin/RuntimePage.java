/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.ScrolledForm;

public class RuntimePage extends PDEFormPage {
	public static final String PAGE_ID="runtime"; //$NON-NLS-1$

    public RuntimePage(FormEditor editor) {
		super(editor, PAGE_ID, PDEUIMessages.RuntimePage_tabName);  //$NON-NLS-1$
	}
    
	protected void createFormContent(IManagedForm mform) {
		super.createFormContent(mform);
		ScrolledForm form = mform.getForm();
		form.setText(PDEUIMessages.ManifestEditor_RuntimeForm_title); //$NON-NLS-1$
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = 10;
		layout.horizontalSpacing = 10;
		layout.verticalSpacing = 20;
        form.getBody().setLayout(layout);
		
        if (isBundle()) {
            mform.addPart(new ExportPackageSection(this, form.getBody()));
            mform.addPart(new ExportPackageVisibilitySection(this, form.getBody()));
            mform.addPart(new LibrarySection(this, form.getBody()));
        } else {
            mform.addPart(new LibrarySection(this, form.getBody()));		
            mform.addPart(new LibraryVisibilitySection(this, form.getBody()));
        }
		
		if (((IPluginModelBase)getPDEEditor().getAggregateModel()).isFragmentModel())
			PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.MANIFEST_FRAGMENT_RUNTIME);
		else
			PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.MANIFEST_PLUGIN_RUNTIME);
	}
    
    private boolean isBundle() {
        return getPDEEditor().getContextManager().findContext(BundleInputContext.CONTEXT_ID) != null;
    }


}
