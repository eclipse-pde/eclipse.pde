package org.eclipse.pde.internal.ui.editor.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.feature.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.WizardCheckboxTablePart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;

public class IncludeFeaturesWizardPage extends WizardPage {
	public static final String KEY_TITLE = "FeatureEditor.IncludedFeaturesSection.new.title";
	public static final String KEY_DESC = "FeatureEditor.IncludedFeaturesSection.new.desc";
	public static final String KEY_FEATURES =
		"FeatureEditor.IncludedFeaturesSection.new.label";
	public static final String KEY_ADDING = "FeatureEditor.IncludedFeaturesSection.new.adding";
	public static final String KEY_UPDATING =
		"FeatureEditor.IncludedFeaturesSection.new.updating";
	private IFeatureModel model;
	private TablePart checkboxTablePart;
	private CheckboxTableViewer pluginViewer;

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
	}

	public IncludeFeaturesWizardPage(IFeatureModel model) {
		super("IncludeFeaturesPage");
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
	}

	protected void createPluginList(Composite parent) {
		checkboxTablePart.createControl(parent);
		pluginViewer = checkboxTablePart.getTableViewer();
		pluginViewer.setContentProvider(new PluginContentProvider());
		pluginViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		pluginViewer.addFilter(new ViewerFilter() {
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
		IFeatureChild[] features = model.getFeature().getIncludedFeatures();
		IFeature cfeature = candidate.getFeature();
		
		if (isThisModel(candidate)) return true;

		for (int i = 0; i < features.length; i++) {
			FeatureChild child = (FeatureChild)features[i];
			if (child.getId().equals(cfeature.getId()) &&
				child.getVersion().equals(cfeature.getVersion())) return true;				return true;
		}
		return false;
	}
	
	private boolean isThisModel(IFeatureModel candidate) {
		IFeature cfeature = candidate.getFeature();
		IFeature thisFeature = this.model.getFeature();
		
		return cfeature.getId().equals(thisFeature.getId()) &&
			cfeature.getVersion().equals(thisFeature.getVersion());
	}

	public void init(IWorkbench workbench) {
	}

	private void initialize() {
		pluginViewer.setInput(model.getFeature());
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
		IFeature feature = model.getFeature();
		IFeatureChild[] added = new IFeatureChild[candidates.length];
		for (int i = 0; i < candidates.length; i++) {
			IFeatureModel candidate = (IFeatureModel) candidates[i];
			String name = candidate.getFeature().getLabel();
			monitor.subTask(candidate.getResourceString(name));
			FeatureChild child = (FeatureChild) model.getFactory().createChild();
			child.loadFrom(candidate.getFeature());
			added[i] = child;
			monitor.worked(1);
		}
		monitor.subTask("");
		monitor.setTaskName(PDEPlugin.getResourceString(KEY_UPDATING));
		feature.addIncludedFeatures(added);
		monitor.worked(1);
	}
}