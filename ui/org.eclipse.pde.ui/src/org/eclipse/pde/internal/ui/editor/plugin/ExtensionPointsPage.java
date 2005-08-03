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
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.editor.*;
import org.eclipse.ui.forms.widgets.*;

public class ExtensionPointsPage extends PDEFormPage {
	public static final String PAGE_ID = "ex-points"; //$NON-NLS-1$
	private ExtensionPointsSection extensionPointsSection;
	private ExtensionPointsBlock block;
	public class ExtensionPointsBlock extends PDEMasterDetailsBlock {
		public ExtensionPointsBlock() {
			super(ExtensionPointsPage.this);
		}
		protected PDESection createMasterSection(IManagedForm managedForm,
				Composite parent) {
			extensionPointsSection = new ExtensionPointsSection(getPage(), parent);
			return extensionPointsSection;
		}
		protected void registerPages(DetailsPart detailsPart) {
			detailsPart.setPageProvider(new IDetailsPageProvider() {
				public Object getPageKey(Object object) {
					if (object instanceof IPluginExtensionPoint)
						return IPluginExtensionPoint.class;
					return object.getClass();
				}
				public IDetailsPage getPage(Object key) {
					if (key.equals(IPluginExtensionPoint.class))
						return new ExtensionPointDetails();
					return null;
				}
			});
		}
	}
	/**
	 * @param editor
	 * @param id
	 * @param title
	 */
	public ExtensionPointsPage(FormEditor editor) {
		super(editor, PAGE_ID, PDEUIMessages.ExtensionPointsPage_tabName);  
		block = new ExtensionPointsBlock();
	}
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		form.setText(PDEUIMessages.ExtensionPointsPage_title); 
		block.createContent(managedForm);
		extensionPointsSection.fireSelection();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.MANIFEST_PLUGIN_EXT_POINTS);
	}
}
