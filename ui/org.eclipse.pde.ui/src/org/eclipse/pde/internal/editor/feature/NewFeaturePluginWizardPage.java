package org.eclipse.pde.internal.editor.feature;
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
import org.eclipse.pde.internal.elements.*;
import org.eclipse.pde.internal.*;
import java.util.*;
import org.eclipse.ui.views.properties.*;
import org.eclipse.pde.internal.builders.*;
import org.eclipse.pde.internal.base.model.feature.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.pde.internal.model.feature.*;
import org.eclipse.swt.events.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import java.lang.reflect.InvocationTargetException;

public class NewFeaturePluginWizardPage extends WizardPage {
	public static final String KEY_TITLE = "FeatureEditor.PluginSection.new.title";
	public static final String KEY_DESC = "FeatureEditor.PluginSection.new.desc";
	public static final String KEY_PLUGINS =
		"FeatureEditor.PluginSection.new.label";
	public static final String KEY_SELECT_ALL =
		"FeatureEditor.PluginSection.new.selectAll";
	public static final String KEY_DESELECT_ALL =
		"FeatureEditor.PluginSection.new.deselectAll";
	public static final String KEY_ADDING = "FeatureEditor.PluginSection.new.adding";
	public static final String KEY_UPDATING =
		"FeatureEditor.PluginSection.new.updating";
	private IFeatureModel model;
	private CheckboxTableViewer pluginViewer;
	private Image pluginImage;
	private Image errorPluginImage;
	private Image fragmentImage;
	private Image errorFragmentImage;
	private Vector candidates = new Vector();

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
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		container.setLayout(layout);

		Label label = new Label(container, SWT.NULL);
		label.setText(PDEPlugin.getResourceString(KEY_PLUGINS));
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);

		Control c = createPluginList(container);
		gd = new GridData(GridData.FILL_BOTH);
		c.setLayoutData(gd);

		Composite buttonContainer = new Composite(container, SWT.NULL);
		layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		buttonContainer.setLayout(layout);
		gd = new GridData(GridData.FILL_VERTICAL);
		buttonContainer.setLayoutData(gd);

		Button button = new Button(buttonContainer, SWT.PUSH);
		gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		button.setLayoutData(gd);
		button.setText(PDEPlugin.getResourceString(KEY_SELECT_ALL));
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleSelectAll(true);
			}
		});
		button = new Button(buttonContainer, SWT.PUSH);
		gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		button.setLayoutData(gd);
		button.setText(PDEPlugin.getResourceString(KEY_DESELECT_ALL));
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleSelectAll(false);
			}
		});
		initialize();
		setControl(container);
	}

	protected Control createPluginList(Composite parent) {
		pluginViewer = CheckboxTableViewer.newCheckList(parent, SWT.BORDER);
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
		pluginViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				Object element = event.getElement();
				if (element instanceof IPluginModelBase) {
					IPluginModelBase model = (IPluginModelBase) event.getElement();
					handleCheckStateChanged(model, event.getChecked());
				}
			}
		});
		pluginViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent e) {
				Object item = ((IStructuredSelection) e.getSelection()).getFirstElement();
				if (item instanceof IPluginModel)
					pluginSelected((IPluginModel) item);
				else
					pluginSelected(null);
			}
		});
		return pluginViewer.getTable();
	}

	public void dispose() {
		super.dispose();
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

	private void pluginSelected(IPluginModel model) {
	}

	private void handleCheckStateChanged(
		IPluginModelBase candidate,
		boolean checked) {
		if (checked)
			candidates.add(candidate);
		else
			candidates.remove(candidate);
		setPageComplete(candidates.size() > 0);
	}

	private void handleSelectAll(boolean select) {
		pluginViewer.setAllChecked(select);
		if (!select) {
			candidates.clear();
		} else {
			Object[] choices = getChoices();
			for (int i = 0; i < choices.length; i++)
				candidates.add(choices[i]);
		}
		setPageComplete(select);
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
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					doAdd(monitor);
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

	private void doAdd(IProgressMonitor monitor) throws CoreException {
		monitor.beginTask(
			PDEPlugin.getResourceString(KEY_ADDING),
			candidates.size() + 1);
		IFeature feature = model.getFeature();
		IFeaturePlugin[] added = new IFeaturePlugin[candidates.size()];
		for (int i = 0; i < candidates.size(); i++) {
			IPluginModelBase candidate = (IPluginModelBase) candidates.get(i);
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