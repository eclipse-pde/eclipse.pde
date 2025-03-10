/*******************************************************************************
 * Copyright (c) 2005, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Code 9 Corporation - ongoing enhancements
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 262885
 *     Alena Laskavaia - Bug 453392 - No debug options help
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TracingOptionsManager;
import org.eclipse.pde.internal.launching.launcher.LaunchArgumentsHelper;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.pde.launching.IPDELauncherConstants;
import org.eclipse.pde.ui.launcher.AbstractLauncherTab;
import org.eclipse.pde.ui.launcher.TracingTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledPageBook;

public class TracingBlock {

	/**
	 * This class encapsulates all calls to the tracing options manager and runs
	 * them using a progress monitor.
	 */
	private static record TracingOptionsManagerDelegate(IRunnableContext fRunnableContext) {

		Map<String, String> getTracingTemplateCopy() {
			return runShowingProgress(
					(tracingOptionsManager, monitor) -> tracingOptionsManager.getTracingTemplateCopy(monitor));
		}

		public Map<String, String> getTracingOptions(Map<String, String> options) {
			return runShowingProgress(
					(tracingOptionsManager, monitor) -> tracingOptionsManager.getTracingOptions(options, monitor));
		}

		public Map<String, String> getTemplateTable(String pluginId) {
			return runShowingProgress(
					(tracingOptionsManager, monitor) -> tracingOptionsManager.getTemplateTable(pluginId, monitor));
		}

		private Map<String, String> runShowingProgress(
				BiFunction<TracingOptionsManager, IProgressMonitor, Map<String, String>> task) {
			try {
				String taskName = PDEUIMessages.TracingBlock_initializing_tracing_options;
				final Map<String, String> result = new HashMap<>();

				// due to a bug
				// (https://github.com/eclipse-platform/eclipse.platform/issues/769),
				// the task needs to be forked otherwise the UI
				// does not show the progress indicator
				fRunnableContext.run(true, false, monitor -> {
					SubMonitor subMonitor = SubMonitor.convert(monitor, taskName, 1);
					result.putAll(task.apply(PDECore.getDefault().getTracingOptionsManager(), subMonitor.split(1)));
				});

				return result;
			} catch (InvocationTargetException e) {
				throw new IllegalStateException(e);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException(e);
			}
		}
	}

	private TracingOptionsManagerDelegate fTracingOptionsManagerDelegate;
	private final TracingTab fTab;
	private Button fTracingCheck;
	private CheckboxTableViewer fPluginViewer;
	private IPluginModelBase[] fTraceableModels;
	private final Map<String, String> fMasterOptions = new HashMap<>();
	private Button fSelectAllButton;
	private Button fDeselectAllButton;
	private Button fRestoreSelectedDefaultButton;
	private Button fRestoreDefaultButton;
	private final Map<IPluginModelBase, TracingPropertySource> fPropertySources = new HashMap<>();
	private FormToolkit fToolkit;
	private ScrolledPageBook fPageBook;
	private boolean activated;

	/**
	 * The last selected item in the list is stored in the dialog settings.
	 */
	private static final String TRACING_SETTINGS = "TracingTab"; //$NON-NLS-1$
	private static final String SETTINGS_SELECTED_PLUGIN = "selectedPlugin"; //$NON-NLS-1$

	public TracingBlock(TracingTab tab) {
		fTab = tab;
	}

	public AbstractLauncherTab getTab() {
		return fTab;
	}

	public void createControl(Composite parent) {
		fTracingCheck = new Button(parent, SWT.CHECK);
		fTracingCheck.setText(PDEUIMessages.TracingLauncherTab_tracing);
		fTracingCheck.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fTracingCheck.addSelectionListener(widgetSelectedAdapter(e -> {
			masterCheckChanged();
			fTab.updateLaunchConfigurationDialog();
			if (fTracingCheck.getSelection()) {
				IStructuredSelection selection = fPluginViewer.getStructuredSelection();
				if (!selection.isEmpty()) {
					pluginSelected((IPluginModelBase) selection.getFirstElement(),
							fPluginViewer.getChecked(selection.getFirstElement()));
				}
			}
		}));

		createSashSection(parent);
	}

	private void createSashSection(Composite container) {
		SashForm sashForm = new SashForm(container, SWT.HORIZONTAL);
		sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));
		createPluginViewer(sashForm);
		createPropertySheetClient(sashForm);
	}

	private void createPluginViewer(Composite sashForm) {
		Composite composite = new Composite(sashForm, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginWidth = layout.marginHeight = 1;
		composite.setLayout(layout);

		fPluginViewer = CheckboxTableViewer.newCheckList(composite, SWT.BORDER);
		fPluginViewer.setContentProvider(ArrayContentProvider.getInstance());
		fPluginViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		fPluginViewer.setComparator(new ListUtil.PluginComparator());
		fPluginViewer.addSelectionChangedListener(e -> {
			fRestoreSelectedDefaultButton.setEnabled(true);
			CheckboxTableViewer tableViewer = (CheckboxTableViewer) e.getSource();
			boolean selected = tableViewer.getChecked(getSelectedModel());
			pluginSelected(getSelectedModel(), selected);
			storeSelectedModel();
		});
		fPluginViewer.addCheckStateListener(event -> {
			CheckboxTableViewer tableViewer = (CheckboxTableViewer) event.getSource();
			tableViewer.setSelection(new StructuredSelection(event.getElement()));
			pluginSelected(getSelectedModel(), event.getChecked());
			fTab.updateLaunchConfigurationDialog();
		});
		fPluginViewer.addDoubleClickListener(event -> {
			CheckboxTableViewer tableViewer = (CheckboxTableViewer) event.getSource();
			Object selection = ((IStructuredSelection) event.getSelection()).getFirstElement();
			boolean addingCheck = !tableViewer.getChecked(selection);
			tableViewer.setChecked(selection, addingCheck);
			pluginSelected(getSelectedModel(), addingCheck);
			fTab.updateLaunchConfigurationDialog();
		});
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 125;
		gd.heightHint = 100;
		fPluginViewer.getTable().setLayoutData(gd);
		createButtonSection(composite);
	}

	private void createPropertySheetClient(Composite sashForm) {
		Composite tableChild = new Composite(sashForm, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		tableChild.setLayout(layout);
		int margin = createPropertySheet(tableChild);
		layout.marginWidth = layout.marginHeight = margin;
	}

	private void createButtonSection(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		container.setLayout(layout);

		fSelectAllButton = new Button(container, SWT.PUSH);
		fSelectAllButton.setText(PDEUIMessages.TracingLauncherTab_enableAll);
		fSelectAllButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
		SWTUtil.setButtonDimensionHint(fSelectAllButton);
		fSelectAllButton.addSelectionListener(widgetSelectedAdapter(e -> {
			fPluginViewer.setAllChecked(true);
			pluginSelected(getSelectedModel(), true);
			fTab.updateLaunchConfigurationDialog();
		}));

		fDeselectAllButton = new Button(container, SWT.PUSH);
		fDeselectAllButton.setText(PDEUIMessages.TracinglauncherTab_disableAll);
		fDeselectAllButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
		SWTUtil.setButtonDimensionHint(fDeselectAllButton);
		fDeselectAllButton.addSelectionListener(widgetSelectedAdapter(e -> {
			fPluginViewer.setAllChecked(false);
			pluginSelected(getSelectedModel(), false);
			fTab.updateLaunchConfigurationDialog();
		}));
	}

	private void createRestoreButtonSection(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		container.setLayout(layout);

		fRestoreSelectedDefaultButton = new Button(container, SWT.PUSH);
		fRestoreSelectedDefaultButton.setText(PDEUIMessages.TracingBlock_restore_default_selected);
		fRestoreSelectedDefaultButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
		SWTUtil.setButtonDimensionHint(fRestoreSelectedDefaultButton);
		fRestoreSelectedDefaultButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			IStructuredSelection selec = fPluginViewer.getStructuredSelection();
			if (selec.getFirstElement() instanceof IPluginModelBase model) {
				String modelName = model.getBundleDescription().getSymbolicName();
				if (modelName != null) {
					Map<String, String> properties = fTracingOptionsManagerDelegate.getTracingTemplateCopy();
					properties.forEach((key, value) -> {
						if (key.startsWith(modelName + '/')) {
							fMasterOptions.put(key, value);
							TracingPropertySource source = getPropertySource(model);
							source.setChanged(true);
						}
					});
					pluginSelected(model, fPluginViewer.getChecked(model));
				}
			}
		}));

		fRestoreDefaultButton = new Button(container, SWT.PUSH);
		fRestoreDefaultButton.setText(PDEUIMessages.TracingBlock_restore_default);
		fRestoreDefaultButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
		SWTUtil.setButtonDimensionHint(fRestoreDefaultButton);
		fRestoreDefaultButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			disposePropertySources();
			fMasterOptions.clear();
			fMasterOptions.putAll(fTracingOptionsManagerDelegate.getTracingTemplateCopy());
			Object[] elements = fPluginViewer.getCheckedElements();
			for (Object element : elements) {
				if (element instanceof IPluginModelBase model) {
					TracingPropertySource source = getPropertySource(model);
					PageBookKey key = new PageBookKey(model, true);
					Composite parentPage = fPageBook.createPage(key);
					source.createContents(parentPage, true);
					source.setChanged(false);
				}
			}
			IStructuredSelection selec = fPluginViewer.getStructuredSelection();
			if (selec.getFirstElement() instanceof IPluginModelBase) {
				IPluginModelBase model = (IPluginModelBase) fPluginViewer.getStructuredSelection().getFirstElement();
				pluginSelected(model, fPluginViewer.getChecked(model));
			}
		}));
	}

	protected int createPropertySheet(Composite parent) {
		fToolkit = new FormToolkit(parent.getDisplay());
		int toolkitBorderStyle = fToolkit.getBorderStyle();
		int style = toolkitBorderStyle == SWT.BORDER ? SWT.NULL : SWT.BORDER;

		Composite container = new Composite(parent, style);
		FillLayout flayout = new FillLayout();
		flayout.marginWidth = 1;
		flayout.marginHeight = 1;
		container.setLayout(flayout);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		fPageBook = new ScrolledPageBook(container, style | SWT.V_SCROLL | SWT.H_SCROLL);
		fToolkit.adapt(fPageBook, false, false);

		createRestoreButtonSection(parent);

		if (style == SWT.NULL) {
			fPageBook.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TREE_BORDER);
			fToolkit.paintBordersFor(container);
		}
		return style == SWT.NULL ? 2 : 0;
	}

	private void activateInternal(ILaunchConfiguration config) {
		fMasterOptions.clear();
		disposePropertySources();
		try {
			fTracingCheck.setSelection(config.getAttribute(IPDELauncherConstants.TRACING, false));
			Map<String, String> options = config.getAttribute(IPDELauncherConstants.TRACING_OPTIONS, (Map<String, String>) null);

			fMasterOptions.putAll(options == null //
					? fTracingOptionsManagerDelegate.getTracingTemplateCopy() //
					: fTracingOptionsManagerDelegate.getTracingOptions(options));

			masterCheckChanged();
			String checked = config.getAttribute(IPDELauncherConstants.TRACING_CHECKED, (String) null);
			if (checked == null) {
				fPluginViewer.setAllChecked(true);
			} else if (checked.equals(IPDELauncherConstants.TRACING_NONE)) {
				fPluginViewer.setAllChecked(false);
			} else {
				List<IPluginModelBase> list = LaunchArgumentsHelper.splitElementsByComma(checked) //
						.map(PluginRegistry::findModel).filter(Objects::nonNull).toList();

				fPluginViewer.setCheckedElements(list.toArray());
				IPluginModelBase model = getLastSelectedPlugin();
				if (model == null && !list.isEmpty()) {
					model = list.get(0);
				}
				if (model != null) {
					fPluginViewer.setSelection(new StructuredSelection(model), true);
					if (fTracingCheck.getSelection()) {
						pluginSelected(model, list.contains(model));
					}
				} else {
					pluginSelected(null, false);
				}
			}
			activated = true;
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	public void setRunnableContext(IRunnableContext runnableContext) {
		fTracingOptionsManagerDelegate = new TracingOptionsManagerDelegate(runnableContext);
	}

	public void performApply(ILaunchConfigurationWorkingCopy config) {
		if (!activated) {
			// nothing to apply as we weren't initialized before,
			// all the data is empty anyway
			return;
		}
		boolean tracingEnabled = fTracingCheck.getSelection();
		config.setAttribute(IPDELauncherConstants.TRACING, tracingEnabled);
		if (tracingEnabled) {
			boolean changes = false;
			for (TracingPropertySource source : fPropertySources.values()) {
				if (source.isModified()) {
					changes = true;
					source.save();
				}
			}
			if (changes) {
				Map<String, String> atts = new HashMap<>(fMasterOptions.size());
				fMasterOptions.forEach((key, value) -> {
					// these are comment keys which we don't want to save
					if (!key.startsWith("#")) { //$NON-NLS-1$
						atts.put(key, value);
					}
				});
				config.setAttribute(IPDELauncherConstants.TRACING_OPTIONS, atts);
			}
		}

		Object[] checked = fPluginViewer.getCheckedElements();
		if (checked.length == 0) {
			config.setAttribute(IPDELauncherConstants.TRACING_CHECKED, IPDELauncherConstants.TRACING_NONE);
		} else if (checked.length == fPluginViewer.getTable().getItemCount()) {
			config.setAttribute(IPDELauncherConstants.TRACING_CHECKED, (String) null);
		} else {
			StringBuilder buffer = new StringBuilder();
			for (int i = 0; i < checked.length; i++) {
				IPluginModelBase model = (IPluginModelBase) checked[i];
				buffer.append(model.getPluginBase().getId());
				if (i < checked.length - 1) {
					buffer.append(',');
				}
			}
			config.setAttribute(IPDELauncherConstants.TRACING_CHECKED, buffer.toString());
		}
	}

	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(IPDELauncherConstants.TRACING, false);
		configuration.setAttribute(IPDELauncherConstants.TRACING_CHECKED, IPDELauncherConstants.TRACING_NONE);
	}

	public void activated(ILaunchConfigurationWorkingCopy workingCopy) {
		activateInternal(workingCopy);
		fPageBook.getParent().getParent().layout(true);
	}

	public void dispose() {
		if (fToolkit != null) {
			fToolkit.dispose();
		}
	}

	public FormToolkit getToolkit() {
		return fToolkit;
	}

	private IPluginModelBase getSelectedModel() {
		if (fTracingCheck.isEnabled()
				&& fPluginViewer.getStructuredSelection().getFirstElement() instanceof IPluginModelBase pluginModel) {
			return pluginModel;
		}
		return null;
	}

	private void pluginSelected(IPluginModelBase model, boolean checked) {
		TracingPropertySource source = getPropertySource(model);
		if (source == null) {
			fPageBook.showEmptyPage();
		} else {
			PageBookKey key = new PageBookKey(model, checked);
			if (!fPageBook.hasPage(key) || source.isChanged()) {
				Composite parent = fPageBook.createPage(key);
				source.createContents(parent, checked);
				source.setChanged(false);
			}
			fPageBook.showPage(key);
		}
	}

	private IPluginModelBase[] getTraceableModels() {
		if (fTraceableModels == null) {
			IPluginModelBase[] models = PluginRegistry.getActiveModels();
			List<IPluginModelBase> result = new ArrayList<>();
			for (IPluginModelBase model : models) {
				if (TracingOptionsManager.isTraceable(model)) {
					result.add(model);
				}
			}
			fTraceableModels = result.toArray(new IPluginModelBase[result.size()]);
		}
		return fTraceableModels;
	}

	/**
	 * Returns the last selected plug-in as stored in dialog settings or <code>null</code> if no
	 * previous selection is found.
	 *
	 * @return model for the last selected plug-in or <code>null</code>
	 */
	private IPluginModelBase getLastSelectedPlugin() {
		IDialogSettings settings = PDEPlugin.getDefault().getDialogSettings().getSection(TRACING_SETTINGS);
		if (settings != null) {
			String id = settings.get(SETTINGS_SELECTED_PLUGIN);
			if (id != null && id.trim().length() > 0) {
				return PluginRegistry.findModel(id);
			}
		}
		return null;
	}

	/**
	 * Stores the currently selected model in the dialog settings for later retrieval using
	 * {@link #getLastSelectedPlugin()}.  If no model is selected, the settings are cleared.
	 */
	private void storeSelectedModel() {
		IDialogSettings settings = PDEPlugin.getDefault().getDialogSettings().getSection(TRACING_SETTINGS);
		if (settings == null) {
			settings = PDEPlugin.getDefault().getDialogSettings().addNewSection(TRACING_SETTINGS);
		}
		IPluginModelBase model = getSelectedModel();
		if (model != null && fPluginViewer.getChecked(model)) {
			settings.put(SETTINGS_SELECTED_PLUGIN, model.getPluginBase().getId());
		} else {
			settings.put(SETTINGS_SELECTED_PLUGIN, (String) null);
		}
	}

	private TracingPropertySource getPropertySource(IPluginModelBase model) {
		if (model == null) {
			return null;
		}
		return fPropertySources.computeIfAbsent(model, m -> {
			String id = m.getPluginBase().getId();
			Map<String, String> defaults = fTracingOptionsManagerDelegate.getTemplateTable(id);
			TracingPropertySource source = new TracingPropertySource(m, fMasterOptions, defaults, this);
			source.setChanged(true);
			return source;
		});
	}

	private void masterCheckChanged() {
		boolean enabled = fTracingCheck.getSelection();
		fPluginViewer.getTable().setEnabled(enabled);
		Control currentPage = fPageBook.getCurrentPage();
		if (currentPage != null && !enabled) {
			fPageBook.showEmptyPage();
		}
		if (enabled) {
			fPluginViewer.setInput(getTraceableModels());
		}

		int count = 0;
		if (fPluginViewer != null) {
			count = fPluginViewer.getTable().getItemCount();
		}
		fSelectAllButton.setEnabled(enabled && count > 0);
		fDeselectAllButton.setEnabled(enabled && count > 0);
		fRestoreDefaultButton.setEnabled(enabled && count > 0);
		fRestoreSelectedDefaultButton.setEnabled(!fPluginViewer.getStructuredSelection().isEmpty());
		if (!enabled) {
			fRestoreSelectedDefaultButton.setEnabled(false);
		}
	}

	private void disposePropertySources() {
		for (TracingPropertySource source : fPropertySources.values()) {
			fPageBook.removePage(source.getModel());
		}
		fPropertySources.clear();
	}

	private record PageBookKey(IPluginModelBase fModel, boolean fEnabled) {
	}

}
