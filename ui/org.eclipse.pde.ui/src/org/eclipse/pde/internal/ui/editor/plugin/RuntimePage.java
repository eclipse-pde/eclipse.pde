/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.ScrolledForm;

public class RuntimePage extends PDEFormPage {
	public static final String PAGE_ID="runtime"; //$NON-NLS-1$

    public RuntimePage(FormEditor editor) {
		super(editor, PAGE_ID, PDEUIMessages.RuntimePage_tabName);  
	}
    
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormPage#getHelpResource()
	 */
	protected String getHelpResource() {
		return IPDEUIConstants.PLUGIN_DOC_ROOT + "guide/tools/editors/manifest_editor/runtime.htm"; //$NON-NLS-1$
	}    
    
	protected void createFormContent(IManagedForm mform) {
		super.createFormContent(mform);
		ScrolledForm form = mform.getForm();
		form.setImage(PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_JAVA_LIB_OBJ));
		form.setText(PDEUIMessages.ManifestEditor_RuntimeForm_title); 
		form.getBody().setLayout(FormLayoutFactory.createFormGridLayout(false, 2));
		
        if (isBundle()) {
            mform.addPart(new ExportPackageSection(this, form.getBody()));
            if (((ManifestEditor)getEditor()).isEquinox())
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
