package org.eclipse.pde.internal.ui.editor.site;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.ui.PDEPlugin;

public class SiteFeatureSection extends ObjectListSection {
	private static final String SECTION_TITLE =
		"SiteEditor.SiteFeatureSection.title";
	private static final String SECTION_DESC =
		"SiteEditor.SiteFeatureSection.desc";
	private static final String KEY_NEW =
		"SiteEditor.SiteFeatureSection.new";
	private static final String POPUP_OPEN =
		"SiteEditor.SiteFeatureSection.popup.open";

	public SiteFeatureSection(FeaturePage page) {
		super(
			page,
			PDEPlugin.getResourceString(SECTION_TITLE),
			PDEPlugin.getResourceString(SECTION_DESC),
			new String[] {
				PDEPlugin.getResourceString(KEY_NEW)});
	}
	
	protected Object[] getElements(Object parent) {
		if (parent instanceof ISite) {
			return ((ISite)parent).getFeatures();
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
		return object instanceof ISiteFeature;
	}
	
	protected String getOpenPopupLabel() {
		return PDEPlugin.getResourceString(POPUP_OPEN);
	}
	
	protected boolean isOpenable() {
		return true;
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
		ISiteFeature[] array =
			(ISiteFeature[]) objects.toArray(new ISiteFeature[objects.size()]);
		ISite site = (ISite) input;
		site.removeFeatures(array);
	}

	protected void handleOpen() {
	}

	protected void setButtonsEnabled(boolean value) {
		getTablePart().setButtonEnabled(0, value);
	}
}