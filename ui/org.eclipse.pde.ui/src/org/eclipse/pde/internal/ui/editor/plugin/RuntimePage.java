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

import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.swt.layout.*;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.editor.*;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.help.*;

public class RuntimePage extends PDEFormPage {
	public static final String PAGE_ID="runtime"; //$NON-NLS-1$

    public RuntimePage(FormEditor editor) {
		super(editor, PAGE_ID, PDEPlugin.getResourceString("RuntimePage.tabName"));  //$NON-NLS-1$
	}
    
	protected void createFormContent(IManagedForm mform) {
		super.createFormContent(mform);
		ScrolledForm form = mform.getForm();
		form.setText(PDEPlugin.getResourceString("ManifestEditor.RuntimeForm.title")); //$NON-NLS-1$
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = 10;
		layout.horizontalSpacing = 10;
		layout.verticalSpacing = 20;
		layout.makeColumnsEqualWidth = true;
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
			WorkbenchHelp.setHelp(form, IHelpContextIds.MANIFEST_FRAGMENT_RUNTIME);
		else
			WorkbenchHelp.setHelp(form, IHelpContextIds.MANIFEST_PLUGIN_RUNTIME);
	}
    
    private boolean isBundle() {
        return getPDEEditor().getContextManager().findContext(BundleInputContext.CONTEXT_ID) != null;
    }


}
