package org.eclipse.pde.internal.ui.editor.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.core.feature.FeaturePlugin;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeaturePlugin;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.WizardCheckboxTablePart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;

public class NewFeaturePluginWizardPage extends WizardPage {
	public static final String KEY_TITLE = "FeatureEditor.PluginSection.new.title";
	public static final String KEY_DESC = "FeatureEditor.PluginSection.new.desc";
	public static final String KEY_PLUGINS =
		"FeatureEditor.PluginSection.new.label";
	public static final String KEY_ADDING = "FeatureEditor.PluginSection.new.adding";
	public static final String KEY_UPDATING =
		"FeatureEditor.PluginSection.new.updating";
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
			super(PDEPlugin.getResourceString(KEY_PLUGINS));
		}
		public void updateCounter(int count) {
			super.updateCounter(count);
			setPageComplete(count>0);
		}
	}

	public NewFeaturePluginWizardPage(IFeatureModel model) {
		super("newFeaturePluginPage");
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
				if (object instanceof IPluginModelBase) {
					IPluginModelBase model = (IPluginModelBase) object;
					return !isOnTheList(model);
				}
				return true;
			}
		});
		GridData gd = (GridData)checkboxTablePart.getControl().getLayoutData();
		gd.heightHint = 300;
	}

	private boolean isOnTheList(IPluginModelBase candidate) {
		IPluginBase plugin = candidate.getPluginBase();
		IFeaturePlugin[] fplugins = model.getFeature().getPlugins();

		for (int i = 0; i < fplugins.length; i++) {
			IFeaturePlugin fplugin = fplugins[i];
			if (fplugin.getId().equals(plugin.getId()))
				return true;
		}
		return false;
	}

	public void init(IWorkbench workbench) {
	}

	private void initialize() {
		pluginViewer.setInput(model.getFeature());
	}

	private Object[] getChoices() {
		WorkspaceModelManager mng = PDECore.getDefault().getWorkspaceModelManager();
		IPluginModel[] plugins = mng.getWorkspacePluginModels();
		IFragmentModel[] fragments = mng.getWorkspaceFragmentModels();
		Object[] choices = new Object[plugins.length + fragments.length];
		System.arraycopy(plugins, 0, choices, 0, plugins.length);
		System.arraycopy(fragments, 0, choices, plugins.length, fragments.length);
		return choices;
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
		IFeaturePlugin[] added = new IFeaturePlugin[candidates.length];
		for (int i = 0; i < candidates.length; i++) {
			IPluginModelBase candidate = (IPluginModelBase) candidates[i];
			monitor.subTask(candidate.getPluginBase().getTranslatedName());
			FeaturePlugin fplugin = (FeaturePlugin) model.getFactory().createPlugin();
			fplugin.loadFrom(candidate.getPluginBase());
			added[i] = fplugin;
			monitor.worked(1);
		}
		monitor.subTask("");
		monitor.setTaskName(PDEPlugin.getResourceString(KEY_UPDATING));
		feature.addPlugins(added);
		monitor.worked(1);
	}
}