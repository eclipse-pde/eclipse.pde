/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;
import org.eclipse.jface.action.Action;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginParent;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDEMasterDetailsBlock;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.DetailsPart;
import org.eclipse.ui.forms.IDetailsPage;
import org.eclipse.ui.forms.IDetailsPageProvider;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.ScrolledForm;

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
			collapseAction.setToolTipText(PDEUIMessages.ExtensionsPage_collapseAll); 
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
		super(editor, PAGE_ID, PDEUIMessages.ExtensionsPage_tabName);  
		block = new ExtensionsBlock();
	}
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		form.setText(PDEUIMessages.ExtensionsPage_title); 
		block.createContent(managedForm);
		BodyTextSection bodyTextSection = new BodyTextSection(this, form.getBody());
		GridData gd = new GridData(GridData.FILL_HORIZONTAL|GridData.VERTICAL_ALIGN_BEGINNING);
		bodyTextSection.getSection().setLayoutData(gd);
		bodyTextSection.getSection().marginWidth = 5;
		managedForm.addPart(bodyTextSection);
		//refire selection
		section.fireSelection();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.MANIFEST_PLUGIN_EXTENSIONS);		
	}
}
