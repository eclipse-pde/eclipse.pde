package org.eclipse.pde.internal.ui.editor.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.resources.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.swt.*;
import org.eclipse.ui.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.pde.internal.ui.*;
import java.util.*;
import org.eclipse.ui.views.properties.*;
import org.eclipse.pde.internal.core.builders.*;
import org.eclipse.pde.internal.ui.model.ifeature.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.feature.*;
import org.eclipse.swt.events.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import java.lang.reflect.InvocationTargetException;
import org.eclipse.pde.internal.ui.parts.*;

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
	private Image pluginImage;
	private Image errorPluginImage;
	private Image fragmentImage;
	private Image errorFragmentImage;

	class PluginLabelProvider
		extends LabelProvider
		implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			if (obj instanceof IPluginModelBase) {
				IPluginModelBase model = (IPluginModelBase) obj;
				IPluginBase plugin = model.getPluginBase();
				return plugin.getTranslatedName() + " (" + plugin.getVersion() + ")";
			}
			return obj.toString();
		}
		public Image getColumnImage(Object obj, int index) {
			if (obj instanceof IPluginModelBase) {
				boolean error = !((IPluginModelBase) obj).isLoaded();
				if (obj instanceof IPluginModel)
					return error ? errorPluginImage : pluginImage;
				else
					return error ? errorFragmentImage : fragmentImage;

			}
			return null;
		}
	}

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
		pluginImage = PDEPluginImages.get(PDEPluginImages.IMG_PLUGIN_OBJ);
		errorPluginImage = PDEPluginImages.get(PDEPluginImages.IMG_ERR_PLUGIN_OBJ);
		fragmentImage = PDEPluginImages.get(PDEPluginImages.IMG_FRAGMENT_OBJ);
		errorFragmentImage = PDEPluginImages.get(PDEPluginImages.IMG_ERR_FRAGMENT_OBJ);
		setTitle(PDEPlugin.getResourceString(KEY_TITLE));
		setDescription(PDEPlugin.getResourceString(KEY_DESC));
		setPageComplete(false);
		
		checkboxTablePart = new TablePart();	
	
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
		pluginViewer.setLabelProvider(new PluginLabelProvider());
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
		WorkspaceModelManager mng = PDEPlugin.getDefault().getWorkspaceModelManager();
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