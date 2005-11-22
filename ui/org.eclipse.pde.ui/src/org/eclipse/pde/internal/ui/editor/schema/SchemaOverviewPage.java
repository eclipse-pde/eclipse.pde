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
package org.eclipse.pde.internal.ui.editor.schema;

import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.text.ColorManager;
import org.eclipse.pde.internal.ui.editor.text.IColorManager;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.ScrolledForm;

public class SchemaOverviewPage extends PDEFormPage {
		 
	 public static final String PAGE_ID = "overview"; //$NON-NLS-1$
	 
	 private IColorManager fColorManager = ColorManager.getDefault();
	 
	 private DocSection fDocSection;
	 
	 private SchemaSpecSection fGeneralInfoSection;
	 private SchemaIncludesSection fInclusionSection;
	 
	 public SchemaOverviewPage(PDEFormEditor editor) {
 		 super(editor, PAGE_ID, PDEUIMessages.SchemaEditor_DocPage_title);
	 }
	 
	 public void setActive(boolean active) {
 		 if (!active)
	 		 getManagedForm().commit(false);
 		 super.setActive(active);
	 }
	 
	 protected void createFormContent(IManagedForm managedForm) {
 		 super.createFormContent(managedForm);
 		 ScrolledForm form = managedForm.getForm();
 		 GridLayout layout = new GridLayout();
 		 layout.marginWidth = 9;
 		 form.getBody().setLayout(layout);

 		 Composite top = managedForm.getToolkit().createComposite(form.getBody());
 		 layout = new GridLayout(2, true);
 		 layout.marginHeight = layout.marginWidth = 0;
 		 layout.horizontalSpacing = 9;
 		 top.setLayout(layout);
 		 top.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
 		 fGeneralInfoSection = new SchemaSpecSection(this, top);		 
 		 fInclusionSection = new SchemaIncludesSection(this, top);
 		 
 		 fDocSection = new DocSection(this, form.getBody(), fColorManager);
 		 GridData gd = new GridData(GridData.FILL_BOTH);
 		 gd.horizontalSpan = 2;
 		 gd.verticalIndent = 20;
 		 fDocSection.getSection().setLayoutData(gd);

 		 managedForm.addPart(fGeneralInfoSection);
 		 managedForm.addPart(fInclusionSection);
 		 managedForm.addPart(fDocSection);
 		 
 		 PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.SCHEMA_EDITOR_DOC);
	 }
	 
	 public void dispose() {
 		 fColorManager.dispose();
 		 super.dispose();
	 }

}
