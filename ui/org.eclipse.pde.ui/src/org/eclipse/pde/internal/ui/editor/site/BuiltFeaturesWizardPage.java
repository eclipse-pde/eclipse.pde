package org.eclipse.pde.internal.ui.editor.site;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.core.site.SiteBuildFeature;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.WizardCheckboxTablePart;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;

public class BuiltFeaturesWizardPage extends WizardPage {
	public static final String KEY_TITLE = "SiteEditor.FeatureProjectSection.new.title";
	public static final String KEY_DESC = "SiteEditor.FeatureProjectSection.new.desc";
	public static final String KEY_FEATURES =
		"SiteEditor.FeatureProjectSection.new.label";
	public static final String KEY_ADDING = "SiteEditor.FeatureProjectSection.new.adding";
	public static final String KEY_UPDATING =
		"SiteEditor.FeatureProjectSection.new.updating";
	private ISiteBuildModel model;
	private TablePart checkboxTablePart;
	private CheckboxTableViewer featureViewer;

	class PluginContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			return getChoices();
		}
	}
	
	class TablePart extends WizardCheckboxTablePart {
		public TablePart() {
			super(PDEPlugin.getResourceString(KEY_FEATURES));
		}
		public void updateCounter(int count) {
			super.updateCounter(count);
			setPageComplete(count>0);
		}
		protected StructuredViewer createStructuredViewer(
			Composite parent,
			int style,
			FormWidgetFactory factory) {
			StructuredViewer viewer =
				super.createStructuredViewer(parent, style, factory);
			viewer.setSorter(ListUtil.FEATURE_SORTER);
			return viewer;
		}
	}

	public BuiltFeaturesWizardPage(ISiteBuildModel model) {
		super("BuiltFeaturesWizardPage");
		this.model = model;
		setTitle(PDEPlugin.getResourceString(KEY_TITLE));
		setDescription(PDEPlugin.getResourceString(KEY_DESC));
		setPageComplete(false);
		
		checkboxTablePart = new TablePart();	
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}
	
	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		super.dispose();
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		container.setLayout(layout);
		
		createPluginList(container);
		initialize();
		setControl(container);
		Dialog.applyDialogFont(container);
		WorkbenchHelp.setHelp(container, IHelpContextIds.FEATURE_INCLUDED_FEATURES_WIZARD);
	}

	protected void createPluginList(Composite parent) {
		checkboxTablePart.createControl(parent);
		featureViewer = checkboxTablePart.getTableViewer();
		featureViewer.setContentProvider(new PluginContentProvider());
		featureViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		featureViewer.addFilter(new ViewerFilter() {
			public boolean select(Viewer v, Object parent, Object object) {
				if (object instanceof IFeatureModel) {
					IFeatureModel model = (IFeatureModel) object;
					return !isOnTheList(model);
				}
				return true;
			}
		});
		GridData gd = (GridData)checkboxTablePart.getControl().getLayoutData();
		gd.heightHint = 300;
	}

	private boolean isOnTheList(IFeatureModel candidate) {
		ISiteBuildFeature[] features = model.getSiteBuild().getFeatures();
		IFeature cfeature = candidate.getFeature();
		
		for (int i = 0; i < features.length; i++) {
			ISiteBuildFeature bfeature = features[i];
			if (bfeature.getId().equals(cfeature.getId()) &&
				bfeature.getVersion().equals(cfeature.getVersion())) return true;
		}
		return false;
	}
	
	public void init(IWorkbench workbench) {
	}

	private void initialize() {
		featureViewer.setInput(model.getSiteBuild());
		checkboxTablePart.setSelection(new Object[0]);
	}

	private Object[] getChoices() {
		WorkspaceModelManager mng = PDECore.getDefault().getWorkspaceModelManager();
		return mng.getWorkspaceFeatureModels();
	}

	public boolean finish() {
		final Object [] candidates = checkboxTablePart.getSelection();
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					doAdd(candidates, monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				}
			}
		};
		try {
			getContainer().run(false, false, op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
			return false;
		}
		return true;
	}

	private void doAdd(Object [] candidates, IProgressMonitor monitor) throws CoreException {
		monitor.beginTask(
			PDEPlugin.getResourceString(KEY_ADDING),
			candidates.length + 1);
		ISiteBuild siteBuild = model.getSiteBuild();
		ISiteBuildFeature[] added = new ISiteBuildFeature[candidates.length];
		for (int i = 0; i < candidates.length; i++) {
			IFeatureModel candidate = (IFeatureModel) candidates[i];
			String name = candidate.getFeature().getLabel();
			monitor.subTask(candidate.getResourceString(name));
			SiteBuildFeature child = (SiteBuildFeature) model.createFeature();
			child.setReferencedFeature(candidate.getFeature());
			added[i] = child;
			monitor.worked(1);
		}
		monitor.subTask("");
		monitor.setTaskName(PDEPlugin.getResourceString(KEY_UPDATING));
		siteBuild.addFeatures(added);
		monitor.worked(1);
	}
}