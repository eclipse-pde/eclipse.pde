package org.eclipse.pde.internal.ui.editor.site;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.ui.PDEPlugin;

public class SiteArchiveSection extends ObjectListSection {
	private static final String SECTION_TITLE =
		"SiteEditor.SiteArchiveSection.title";
	private static final String SECTION_DESC =
		"SiteEditor.SiteArchiveSection.desc";
	private static final String KEY_NEW = "SiteEditor.SiteArchiveSection.new";

	public SiteArchiveSection(ArchivePage page) {
		super(
			page,
			PDEPlugin.getResourceString(SECTION_TITLE),
			PDEPlugin.getResourceString(SECTION_DESC),
			new String[] { PDEPlugin.getResourceString(KEY_NEW)});
	}

	protected Object[] getElements(Object parent) {
		if (parent instanceof ISite) {
			return ((ISite) parent).getArchives();
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
		return object instanceof ISiteArchive;
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
		ISiteArchive[] array =
			(ISiteArchive[]) objects.toArray(new ISiteArchive[objects.size()]);
		ISite site = (ISite) input;
		site.removeArchives(array);
	}

	protected void handleOpen() {
	}

	protected void setButtonsEnabled(boolean value) {
		getTablePart().setButtonEnabled(0, value);
	}
}