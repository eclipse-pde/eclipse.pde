package org.eclipse.pde.internal.ui.editor.site;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.core.IEditable;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.core.site.WorkspaceSiteBuildModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.custom.BusyIndicator;

public class FeatureProjectSection extends CheckboxObjectListSection {
	private static final String SECTION_TITLE =
		"SiteEditor.FeatureProjectSection.title";
	private static final String SECTION_DESC =
		"SiteEditor.FeatureProjectSection.desc";
	private static final String KEY_NEW =
		"SiteEditor.FeatureProjectSection.new";
	private static final String KEY_SELECT_ALL =
		"SiteEditor.FeatureProjectSection.selectAll";
	private static final String KEY_DESELECT_ALL =
		"SiteEditor.FeatureProjectSection.deselectAll";

	private Action buildAction;

	public FeatureProjectSection(BuildPage page) {
		super(
			page,
			PDEPlugin.getResourceString(SECTION_TITLE),
			PDEPlugin.getResourceString(SECTION_DESC),
			new String[] {
				PDEPlugin.getResourceString(KEY_NEW),
				null,
				PDEPlugin.getResourceString(KEY_SELECT_ALL),
				PDEPlugin.getResourceString(KEY_DESELECT_ALL)});
	}

	protected Object[] getElements(Object input) {
		if (input instanceof ISiteBuild) {
			return ((ISiteBuild) input).getFeatures();
		}
		return new Object[0];
	}

	protected boolean isApplicable(Object object) {
		return object instanceof ISiteBuildFeature;
	}

	public void commitChanges(boolean onSave) {
		ISiteModel model = (ISiteModel) getFormPage().getModel();
		ISiteBuildModel buildModel = model.getBuildModel();
		if (onSave
			&& buildModel instanceof WorkspaceSiteBuildModel
			&& ((WorkspaceSiteBuildModel) buildModel).isDirty()) {
			((WorkspaceSiteBuildModel) buildModel).save();
		}
	}

	protected void buttonSelected(int index) {
		switch (index) {
			case 0 :
				handleNew();
				break;
			case 2 :
				handleSelectAll(true);
				break;
			case 3 :
				handleSelectAll(false);
				break;
		}
	}

	public void dispose() {
		ISiteModel model = (ISiteModel) getFormPage().getModel();
		ISiteBuildModel buildModel = model.getBuildModel();
		if (buildModel != null)
			buildModel.removeModelChangedListener(this);
		WorkspaceModelManager mng =
			PDECore.getDefault().getWorkspaceModelManager();
		mng.removeModelProviderListener(this);
		super.dispose();
	}

	public void expandTo(Object object) {
		if (object instanceof ISiteBuildFeature) {
			tableViewer.setSelection(new StructuredSelection(object), true);
		}
	}

	public void handleNew() {
		final ISiteModel model = (ISiteModel) getFormPage().getModel();
		final ISiteBuildModel buildModel = model.getBuildModel();

		BusyIndicator
			.showWhile(tableViewer.getTable().getDisplay(), new Runnable() {
			public void run() {
				BuiltFeaturesWizard wizard =
					new BuiltFeaturesWizard(buildModel);
				WizardDialog dialog =
					new WizardDialog(
						tableViewer.getControl().getShell(),
						wizard);
				dialog.open();
				forceDirty();
			}
		});
	}

	private void forceDirty() {
		ISiteModel model = (ISiteModel) getFormPage().getModel();
		ISiteBuildModel buildModel = model.getBuildModel();
		if (((IEditable) buildModel).isDirty()) {
			setDirty(true);
			((IEditable) model).setDirty(true);
			getFormPage().getEditor().fireSaveNeeded();
		}
	}

	private void handleSelectAll() {
		IStructuredContentProvider provider =
			(IStructuredContentProvider) tableViewer.getContentProvider();
		Object[] elements = provider.getElements(tableViewer.getInput());
		StructuredSelection ssel = new StructuredSelection(elements);
		tableViewer.setSelection(ssel);
	}

	protected void handleOpen() {
	}

	protected void remove(Object input, List removed) throws CoreException {
		ISiteBuildFeature[] array =
			(ISiteBuildFeature[]) removed.toArray(
				new ISiteBuildFeature[removed.size()]);
		ISiteBuild siteBuild = (ISiteBuild) input;
		siteBuild.removeFeatures(array);
	}

	public boolean isOpenable() {
		return true;
	}

	public void initialize(Object input) {
		ISiteModel model = (ISiteModel) input;
		update(input);

		if (model.isEditable() == false) {
			tableViewer.setAllGrayed(true);
			getTablePart().setButtonEnabled(2, false);
			getTablePart().setButtonEnabled(3, false);
		}

		ISiteBuildModel buildModel = model.getBuildModel();
		getTablePart().setButtonEnabled(0, buildModel.isEditable());
		model.addModelChangedListener(this);
		buildModel.addModelChangedListener(this);
		WorkspaceModelManager mng =
			PDECore.getDefault().getWorkspaceModelManager();
		mng.addModelProviderListener(this);
	}

	protected void makeActions() {
		super.makeActions();
		buildAction = new Action() {
			public void run() {
				doBuild();
			}
		};
		buildAction.setText("Build Selected");
	}

	protected void fillClientActions(IMenuManager manager) {
		IStructuredSelection selection =
			(IStructuredSelection) tableViewer.getSelection();
		if (selection.size() > 0) {
			manager.add(buildAction);
			manager.add(new Separator());
		}
	}

	private void doBuild() {
		ArrayList result = new ArrayList();
		IStructuredSelection selection =
			(IStructuredSelection) tableViewer.getSelection();
		for (Iterator iter = selection.iterator(); iter.hasNext();) {
			ISiteBuildFeature sbfeature = (ISiteBuildFeature) iter.next();
			if (sbfeature.getReferencedFeature() != null) {
				result.add(sbfeature);
			}
		}
		if (result.size() == 0) {
		} else {
			FeatureBuildOperation op = new FeatureBuildOperation(result, false);
			ProgressMonitorDialog dialog =
				new ProgressMonitorDialog(tableViewer.getControl().getShell());
			try {
				dialog.run(true, true, op);
			} catch (InterruptedException e) {
			} catch (InvocationTargetException e) {
				PDEPlugin.logException(e);
			}
		}
	}

	protected void setButtonsEnabled(boolean value) {
	}

	protected String getOpenPopupLabel() {
		return "&Open";
	}

	public void modelsChanged(IModelProviderEvent event) {
		updateNeeded = true;
		update();
	}

	public void update(Object input) {
		ISiteModel model = (ISiteModel) input;
		ISiteBuildModel buildModel = model.getBuildModel();
		ISiteBuild build = buildModel.getSiteBuild();
		build.resetReferences();
		tableViewer.setInput(build);
		updateChecks(model, buildModel);
		updateNeeded = false;
	}

	protected void elementChecked(Object element, boolean checked) {
		ISiteModel model = (ISiteModel) getFormPage().getModel();
		if (model.isEditable() == false) {
			tableViewer.setChecked(element, !checked);
			return;
		}
		ISiteBuildFeature selection = (ISiteBuildFeature) element;
		updateSiteFeature(selection, checked);
	}

	protected void handleSelectAll(boolean selected) {
		ISiteModel model = (ISiteModel) getFormPage().getModel();
		ISiteBuildModel buildModel = model.getBuildModel();
		tableViewer.setAllChecked(selected);
		ISiteBuildFeature[] sbfeatures =
			buildModel.getSiteBuild().getFeatures();
		ArrayList result = new ArrayList();

		try {
			for (int i = 0; i < sbfeatures.length; i++) {
				ISiteBuildFeature sbfeature = sbfeatures[i];
				ISiteFeature sfeature = findMatchingSiteFeature(sbfeature);
				if (selected && sfeature == null) {
					// add
					sfeature = createSiteFeature(model, sbfeature);
					result.add(sfeature);
				} else if (!selected && sfeature != null) {
					result.add(sfeature);
				}
			}
			if (result.size() > 0) {
				ISiteFeature[] array =
					(ISiteFeature[]) result.toArray(
						new ISiteFeature[result.size()]);
				if (selected)
					model.getSite().addFeatures(array);
				else
					model.getSite().removeFeatures(array);
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private ISiteFeature createSiteFeature(
		ISiteModel model,
		ISiteBuildFeature sbfeature)
		throws CoreException {
		ISiteFeature sfeature = model.getFactory().createFeature();
		sfeature.setId(sbfeature.getId());
		sfeature.setVersion(sbfeature.getVersion());
		sfeature.setURL(sbfeature.getTargetURL());
		return sfeature;
	}

	private void updateSiteFeature(ISiteBuildFeature sbfeature, boolean add) {
		ISiteModel model = (ISiteModel) getFormPage().getModel();
		ISite site = model.getSite();
		ISiteFeature sfeature = findMatchingSiteFeature(sbfeature);
		if (add) {
			// make sure it exists - add if needed
			if (sfeature == null) {
				try {
					sfeature = createSiteFeature(model, sbfeature);
					site.addFeatures(new ISiteFeature[] { sfeature });
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		} else {
			// make sure it does not exist - remove if needed
			if (sfeature != null) {
				try {
					site.removeFeatures(new ISiteFeature[] { sfeature });
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		}
	}

	private ISiteFeature findMatchingSiteFeature(ISiteBuildFeature sbfeature) {
		ISiteModel model = (ISiteModel) getFormPage().getModel();
		ISiteFeature[] sfeatures = model.getSite().getFeatures();
		return findMatchingSiteFeature(sbfeature, sfeatures);
	}

	private ISiteFeature findMatchingSiteFeature(
		ISiteBuildFeature sbfeature,
		ISiteFeature[] sfeatures) {
		for (int j = 0; j < sfeatures.length; j++) {
			ISiteFeature sfeature = sfeatures[j];
			if (matches(sfeature, sbfeature))
				return sfeature;
		}
		return null;
	}

	private void updateChecks(ISiteModel model, ISiteBuildModel buildModel) {
		ISiteBuildFeature[] sbfeatures =
			buildModel.getSiteBuild().getFeatures();
		ISiteFeature[] sfeatures = model.getSite().getFeatures();
		ArrayList checked = new ArrayList();
		for (int i = 0; i < sbfeatures.length; i++) {
			ISiteBuildFeature sbfeature = sbfeatures[i];
			ISiteFeature sfeature =
				findMatchingSiteFeature(sbfeature, sfeatures);
			if (sfeature != null)
				checked.add(sbfeature);
		}
		tableViewer.setCheckedElements(checked.toArray());
	}

	private boolean matches(
		ISiteFeature sfeature,
		ISiteBuildFeature sbfeature) {
		String targetURL = sbfeature.getTargetURL();
		String url = sfeature.getURL();
		return (
			url != null
				&& targetURL != null
				&& url.equalsIgnoreCase(targetURL));
	}
}