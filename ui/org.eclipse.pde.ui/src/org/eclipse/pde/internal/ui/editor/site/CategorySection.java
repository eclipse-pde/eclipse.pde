package org.eclipse.pde.internal.ui.editor.site;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.update.ui.forms.internal.FormSection;

public class CategorySection extends ObjectListSection {
	private static final String SECTION_TITLE =
		"SiteEditor.CategorySection.title";
	private static final String SECTION_DESC =
		"SiteEditor.CategorySection.desc";
	private static final String KEY_NEW = "SiteEditor.CategorySection.new";

	public CategorySection(FeaturePage page) {
		super(
			page,
			PDEPlugin.getResourceString(SECTION_TITLE),
			PDEPlugin.getResourceString(SECTION_DESC),
			new String[] { PDEPlugin.getResourceString(KEY_NEW)});
	}

	protected Object[] getElements(Object parent) {
		if (parent instanceof ISiteFeature) {
			return ((ISiteFeature) parent).getCategories();
		}
		return new Object[0];
	}

	protected void buttonSelected(int index) {
		switch (index) {
			case 0 :
				handleNew();
				break;
		}
	}

	protected boolean isApplicable(Object object) {
		return object instanceof ISiteCategory;
	}

	protected String getOpenPopupLabel() {
		return null;
	}

	protected boolean isOpenable() {
		return false;
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

	protected void remove(Object input, List objects) throws CoreException {
		ISiteCategory[] array =
			(ISiteCategory[]) objects.toArray(
				new ISiteCategory[objects.size()]);
		ISiteFeature feature = (ISiteFeature) input;
		feature.removeCategories(array);
	}

	protected void handleOpen() {
	}

	protected void setButtonsEnabled(boolean value) {
		getTablePart().setButtonEnabled(0, value);
	}
	
	public void sectionChanged(
		FormSection source,
		int changeType,
		Object changeObject) {
		if (changeType == SELECTION) {
			inputChanged(changeObject);
		}
	}

	private void inputChanged(Object changeObject) {
		if (changeObject instanceof ISiteFeature) {
			getTablePart().getViewer().setInput(changeObject);
		} else {
			getTablePart().getViewer().setInput(null);
		}
	}
}