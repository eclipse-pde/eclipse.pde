package org.eclipse.pde.internal.ui.editor.site;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.ui.PDEPlugin;

public class CategoryDefinitionSection extends ObjectListSection {
	private static final String SECTION_TITLE =
		"SiteEditor.CategoryDefinitionSection.title";
	private static final String SECTION_DESC =
		"SiteEditor.CategoryDefinitionSection.desc";
	private static final String KEY_NEW =
		"SiteEditor.CategoryDefinitionSection.new";
	private static final String KEY_EDIT =
		"SiteEditor.CategoryDefinitionSection.edit";
	private static final String POPUP_EDIT =
		"SiteEditor.CategoryDefinitionSection.popup.edit";

	public CategoryDefinitionSection(SitePage page) {
		super(
			page,
			PDEPlugin.getResourceString(SECTION_TITLE),
			PDEPlugin.getResourceString(SECTION_DESC),
			new String[] {
				PDEPlugin.getResourceString(KEY_NEW),
				PDEPlugin.getResourceString(KEY_EDIT)});
	}

	protected Object[] getElements(Object parent) {
		if (parent instanceof ISite) {
			return ((ISite) parent).getCategoryDefinitions();
		}
		return new Object[0];
	}

	protected void buttonSelected(int index) {
		switch (index) {
			case 0 :
				handleNew();
				break;
			case 1 :
				handleOpen();
				break;
		}
	}

	protected boolean isApplicable(Object object) {
		return object instanceof ISiteCategoryDefinition;
	}

	protected String getOpenPopupLabel() {
		return PDEPlugin.getResourceString(POPUP_EDIT);
	}

	protected boolean isOpenable() {
		return true;
	}

	protected void remove(Object input, List objects) throws CoreException {
		ISiteCategoryDefinition[] array =
			(ISiteCategoryDefinition[]) objects.toArray(
				new ISiteCategoryDefinition[objects.size()]);
		ISite site = (ISite)input;
		site.removeCategoryDefinitions(array);
	}

	protected void handleNew() {
		/*
				final ISiteModel model = (ISiteModel) getFormPage().getModel();
				final ISiteBuildModel buildModel = model.getBuildModel();
		
				BusyIndicator
					.showWhile(projectViewer.getTable().getDisplay(), new Runnable() {
					public void run() {
						BuiltFeaturesWizard wizard =
							new BuiltFeaturesWizard(buildModel);
						WizardDialog dialog =
							new WizardDialog(
								projectViewer.getControl().getShell(),
								wizard);
						dialog.open();
						forceDirty();
					}
				});
		*/
	}

	protected void handleOpen() {
	}

	protected void setButtonsEnabled(boolean value) {
		getTablePart().setButtonEnabled(0, value);
		getTablePart().setButtonEnabled(1, value);
	}
}