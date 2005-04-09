/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;
import org.eclipse.jface.action.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.editor.*;
import org.eclipse.ui.forms.widgets.*;

public class ExtensionsPage extends PDEFormPage {
	public static final String PAGE_ID = "extensions"; //$NON-NLS-1$
	private ExtensionsBlock block;
	private ExtensionsSection section;
	
	public class ExtensionsBlock extends PDEMasterDetailsBlock implements IDetailsPageProvider {
		public ExtensionsBlock() {
			super(ExtensionsPage.this);
		}
		protected PDESection createMasterSection(IManagedForm managedForm,
				Composite parent) {
			section = new ExtensionsSection(getPage(), parent);
			return section;
		}
		protected void registerPages(DetailsPart detailsPart) {
			detailsPart.setPageLimit(10);
			// register static page for the extensions
			detailsPart.registerPage(IPluginExtension.class, new ExtensionDetails());
			// register a dynamic provider for elements
			detailsPart.setPageProvider(this);
		}
		public Object getPageKey(Object object) {
			if (object instanceof IPluginExtension)
				return IPluginExtension.class;
			if (object instanceof IPluginElement) {
				ISchemaElement element = ExtensionsSection.getSchemaElement((IPluginElement)object);
				if (element!=null) return element;
				// no element - construct one
				IPluginElement pelement = (IPluginElement)object;
				String ename = pelement.getName();
				IPluginExtension extension = ExtensionsSection.getExtension((IPluginParent)pelement.getParent());
				return extension.getPoint()+"/"+ename; //$NON-NLS-1$
			}
			return object.getClass();
		}
		public IDetailsPage getPage(Object object) {
			if (object instanceof ISchemaElement)
				return new ExtensionElementDetails((ISchemaElement)object);
			if (object instanceof String)
				return new ExtensionElementDetails(null);
			return null;
		}
		protected void createToolBarActions(IManagedForm managedForm) {
			final ScrolledForm form = managedForm.getForm();
			Action collapseAction = new Action("col") { //$NON-NLS-1$
				public void run() {
					section.handleCollapseAll();
				}
			};
			collapseAction.setToolTipText(PDEUIMessages.ExtensionsPage_collapseAll); //$NON-NLS-1$
			collapseAction.setImageDescriptor(PDEPluginImages.DESC_COLLAPSE_ALL);
			form.getToolBarManager().add(collapseAction);
			super.createToolBarActions(managedForm);
		}
	}
	/**
	 * @param editor
	 * @param id
	 * @param title
	 */
	public ExtensionsPage(FormEditor editor) {
		super(editor, PAGE_ID, PDEUIMessages.ExtensionsPage_tabName);  //$NON-NLS-1$
		block = new ExtensionsBlock();
	}
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		form.setText(PDEUIMessages.ExtensionsPage_title); //$NON-NLS-1$
		block.createContent(managedForm);
		BodyTextSection bodyTextSection = new BodyTextSection(this, form.getBody());
		GridData gd = new GridData(GridData.FILL_HORIZONTAL|GridData.VERTICAL_ALIGN_BEGINNING);
		bodyTextSection.getSection().setLayoutData(gd);
		bodyTextSection.getSection().marginWidth = 5;
		managedForm.addPart(bodyTextSection);
		//refire selection
		section.fireSelection();
	}
}
