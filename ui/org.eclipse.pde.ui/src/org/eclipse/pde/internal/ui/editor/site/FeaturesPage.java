/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.site;
import org.eclipse.pde.internal.core.isite.ISiteCategoryDefinition;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDEMasterDetailsBlock;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.DetailsPart;
import org.eclipse.ui.forms.IDetailsPage;
import org.eclipse.ui.forms.IDetailsPageProvider;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.ScrolledForm;
/**
 * 
 * Features page.
 */
public class FeaturesPage extends PDEFormPage {
	public static final String PAGE_ID = "features"; //$NON-NLS-1$
//	private FeatureSection featureSection;
	private CategorySection categorySection;
	private SiteFeaturesBlock block;
	public class SiteFeaturesBlock extends PDEMasterDetailsBlock {
		public SiteFeaturesBlock() {
			super(FeaturesPage.this);
		}
		protected PDESection createMasterSection(IManagedForm managedForm,
				Composite parent) {
			categorySection = new CategorySection(getPage(), parent);
			return categorySection;
		}
		protected void registerPages(DetailsPart detailsPart) {
			detailsPart.setPageProvider(new IDetailsPageProvider() {
				public Object getPageKey(Object object) {
					if (object instanceof SiteFeatureAdapter)
						return SiteFeatureAdapter.class;
					if (object instanceof ISiteCategoryDefinition)
						return ISiteCategoryDefinition.class;
					return object.getClass();
				}
				public IDetailsPage getPage(Object key) {
					if (key.equals(SiteFeatureAdapter.class))
						return new FeatureDetails();
					if (key.equals(ISiteCategoryDefinition.class))
						return new CategoryDetails();
					return null;
				}
			});
		}
	}
	
	public FeaturesPage(PDEFormEditor editor) {
		super(editor, PAGE_ID, PDEPlugin.getResourceString("FeaturesPage.title")); //$NON-NLS-1$
		block = new SiteFeaturesBlock();
	}
	protected void createFormContent(IManagedForm managedForm) {
		
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		form.setText(PDEPlugin.getResourceString("FeaturesPage.header")); //$NON-NLS-1$
		block.createContent(managedForm);
		categorySection.fireSelection();

	}
}
