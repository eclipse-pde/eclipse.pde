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

import org.eclipse.pde.internal.core.isite.ISiteCategoryDefinition;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.widgets.ScrolledForm;

/**
 *
 * Features page.
 */
public class FeaturesPage extends PDEFormPage {
	public static final String PAGE_ID = "features"; //$NON-NLS-1$
	private CategorySection fCategorySection;
	private SiteFeaturesBlock fBlock;

	public class SiteFeaturesBlock extends PDEMasterDetailsBlock {
		public SiteFeaturesBlock() {
			super(FeaturesPage.this);
		}

		@Override
		protected PDESection createMasterSection(IManagedForm managedForm, Composite parent) {
			fCategorySection = new CategorySection(getPage(), parent);
			return fCategorySection;
		}

		@Override
		protected void registerPages(DetailsPart detailsPart) {
			detailsPart.setPageProvider(new IDetailsPageProvider() {
				@Override
				public Object getPageKey(Object object) {
					if (object instanceof SiteFeatureAdapter)
						return SiteFeatureAdapter.class;
					if (object instanceof ISiteCategoryDefinition)
						return ISiteCategoryDefinition.class;
					return object.getClass();
				}

				@Override
				public IDetailsPage getPage(Object key) {
					if (key.equals(SiteFeatureAdapter.class))
						return createFeatureDetails();
					if (key.equals(ISiteCategoryDefinition.class))
						return createCategoryDetails();
					return null;
				}
			});
		}
	}

	public FeaturesPage(PDEFormEditor editor) {
		super(editor, PAGE_ID, PDEUIMessages.FeaturesPage_title);
		fBlock = new SiteFeaturesBlock();
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		form.setText(PDEUIMessages.FeaturesPage_header);
		form.setImage(PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_SITE_XML_OBJ));
		fBlock.createContent(managedForm);
		fCategorySection.fireSelection();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.MANIFEST_SITE_FEATURES);
	}

	private IDetailsPage createFeatureDetails() {
		return new PDEDetailsSections() {
			@Override
			protected PDESection[] createSections(PDEFormPage page, Composite parent) {
				return new PDESection[] {new FeatureDetailsSection(getPage(), parent), new PortabilitySection(getPage(), parent)};
			}

			@Override
			public String getContextId() {
				return SiteInputContext.CONTEXT_ID;
			}
		};
	}

	private IDetailsPage createCategoryDetails() {
		return new PDEDetailsSections() {
			@Override
			protected PDESection[] createSections(PDEFormPage page, Composite parent) {
				return new PDESection[] {new CategoryDetailsSection(getPage(), parent)};
			}

			@Override
			public String getContextId() {
				return SiteInputContext.CONTEXT_ID;
			}
		};
	}

	@Override
	protected String getHelpResource() {
		return IHelpContextIds.MANIFEST_SITE_FEATURES;
	}
}
