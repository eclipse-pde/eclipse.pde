/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.swt.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.help.*;
public class TracingLauncherTab extends AbstractLauncherTab
		implements
			ILauncherSettings {
	private Button fTracingCheck;
	private CheckboxTableViewer fPluginViewer;
	private IPluginModelBase[] fTraceableModels;
	private Properties fMasterOptions = new Properties();
	private Hashtable fPropertySources = new Hashtable();
	private FormToolkit fToolkit;
	private ScrolledPageBook fPageBook;
	private Label fPropertyLabel;
	private Image fImage;
	private Button fSelectAllButton;
	private Button fDeselectAllButton;
	public TracingLauncherTab() {
		PDEPlugin.getDefault().getLabelProvider().connect(this);
		fImage = PDEPluginImages.DESC_DOC_SECTION_OBJ.createImage();
	}
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		container.setLayout(new GridLayout());
		createEnableTracingButton(container);
		Label separator = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
		separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		createSashSection(container);
		createButtonSection(container);
		setControl(container);
		Dialog.applyDialogFont(container);
		WorkbenchHelp.setHelp(container, IHelpContextIds.LAUNCHER_TRACING);
	}
	private void createButtonSection(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		container.setLayout(layout);
		fSelectAllButton = new Button(container, SWT.PUSH);
		fSelectAllButton.setText(PDEPlugin
				.getResourceString("TracingLauncherTab.selectAll")); //$NON-NLS-1$
		fSelectAllButton.setLayoutData(new GridData(
				GridData.HORIZONTAL_ALIGN_BEGINNING));
		SWTUtil.setButtonDimensionHint(fSelectAllButton);
		fSelectAllButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fPluginViewer.setAllChecked(true);
				updateLaunchConfigurationDialog();
			}
		});
		fDeselectAllButton = new Button(container, SWT.PUSH);
		fDeselectAllButton.setText(PDEPlugin
				.getResourceString("TracinglauncherTab.deselectAll")); //$NON-NLS-1$
		fDeselectAllButton.setLayoutData(new GridData(
				GridData.HORIZONTAL_ALIGN_BEGINNING));
		SWTUtil.setButtonDimensionHint(fDeselectAllButton);
		fDeselectAllButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fPluginViewer.setAllChecked(false);
				updateLaunchConfigurationDialog();
			}
		});
	}
	private void createEnableTracingButton(Composite container) {
		fTracingCheck = new Button(container, SWT.CHECK);
		fTracingCheck.setText(PDEPlugin
				.getResourceString("TracingLauncherTab.tracing")); //$NON-NLS-1$
		fTracingCheck.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fTracingCheck.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				masterCheckChanged(true);
				updateLaunchConfigurationDialog();
			}
		});
	}
	private void createSashSection(Composite container) {
		SashForm sashForm = new SashForm(container, SWT.HORIZONTAL);
		sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));
		createPluginViewer(sashForm);
		createPropertySheetClient(sashForm);
	}
	private void createPluginViewer(Composite sashForm) {
		Composite composite = new Composite(sashForm, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 1;
		composite.setLayout(layout);
		Label label = new Label(composite, SWT.NULL);
		label
				.setText(PDEPlugin
						.getResourceString("TracingLauncherTab.plugins")); //$NON-NLS-1$
		fPluginViewer = CheckboxTableViewer.newCheckList(composite, SWT.BORDER);
		fPluginViewer.setContentProvider(new ArrayContentProvider());
		fPluginViewer.setLabelProvider(PDEPlugin.getDefault()
				.getLabelProvider());
		fPluginViewer.setSorter(new ListUtil.PluginSorter());
		fPluginViewer
				.addSelectionChangedListener(new ISelectionChangedListener() {
					public void selectionChanged(SelectionChangedEvent e) {
						pluginSelected(getSelectedModel());
					}
				});
		fPluginViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				updateLaunchConfigurationDialog();
			}
		});
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 125;
		gd.heightHint = 100;
		fPluginViewer.getTable().setLayoutData(gd);
		fPluginViewer.setInput(getTraceableModels());
	}
	private void createPropertySheetClient(Composite sashForm) {
		Composite tableChild = new Composite(sashForm, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		tableChild.setLayout(layout);
		fPropertyLabel = new Label(tableChild, SWT.NULL);
		fPropertyLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		updatePropertyLabel(null);
		int margin = createPropertySheet(tableChild);
		layout.marginWidth = layout.marginHeight = margin;
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
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 100;
		gd.widthHint = 125;
		fPageBook.setLayoutData(gd);
		
		if (style == SWT.NULL) {
			fPageBook.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TREE_BORDER);
			fToolkit.paintBordersFor(container);
		}
		return style == SWT.NULL ? 2 : 0;
	}
	public void activated(ILaunchConfigurationWorkingCopy workingCopy) {
		fPageBook.getParent().getParent().layout(true);
	}
	public void dispose() {
		if (fToolkit != null)
			fToolkit.dispose();
		if (fImage != null)
			fImage.dispose();
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		super.dispose();
	}
	public FormToolkit getToolkit() {
		return fToolkit;
	}
	private IPluginModelBase[] getTraceableModels() {
		if (fTraceableModels == null) {
			PluginModelManager manager = PDECore.getDefault().getModelManager();
			IPluginModelBase[] models = manager.getPlugins();
			ArrayList result = new ArrayList();
			for (int i = 0; i < models.length; i++) {
				if (TracingOptionsManager.isTraceable(models[i]))
					result.add(models[i]);
			}
			fTraceableModels = (IPluginModelBase[]) result
					.toArray(new IPluginModelBase[result.size()]);
		}
		return fTraceableModels;
	}
	private TracingPropertySource getPropertySource(IPluginModelBase model) {
		if (model == null)
			return null;
		TracingPropertySource source = (TracingPropertySource) fPropertySources
				.get(model);
		if (source == null) {
			String id = model.getPluginBase().getId();
			Hashtable defaults = PDECore.getDefault()
					.getTracingOptionsManager().getTemplateTable(id);
			source = new TracingPropertySource(model, fMasterOptions,
					defaults, this);
			fPropertySources.put(model, source);
		}
		return source;
	}
	private void masterCheckChanged(boolean userChange) {
		boolean enabled = fTracingCheck.getSelection();
		fPluginViewer.getTable().setEnabled(enabled);
		Control currentPage = fPageBook.getCurrentPage();
		if (currentPage != null)
			currentPage.setEnabled(enabled);
		fSelectAllButton.setEnabled(enabled);
		fDeselectAllButton.setEnabled(enabled);
	}
	private void disposePropertySources() {
		for (Enumeration elements = fPropertySources.elements(); elements
				.hasMoreElements();) {
			TracingPropertySource source = (TracingPropertySource) elements
					.nextElement();
			fPageBook.removePage(source.getModel());
		}
		fPropertySources.clear();
	}
	
	public void initializeFrom(ILaunchConfiguration config) {
		fMasterOptions.clear();
		disposePropertySources();
		try {
			fTracingCheck.setSelection(config.getAttribute(TRACING, false));
			Map options = config.getAttribute(TRACING_OPTIONS, (Map) null);
			if (options == null)
				options = PDECore.getDefault().getTracingOptionsManager()
						.getTracingTemplateCopy();
			else
				options = PDECore.getDefault().getTracingOptionsManager()
						.getTracingOptions(options);
			fMasterOptions.putAll(options);
			masterCheckChanged(false);
			IPluginModelBase model = getLastSelectedPlugin(config);
			if (model != null) {
				fPluginViewer.setSelection(new StructuredSelection(model));
			} else {
				pluginSelected(null);
			}
			String checked = config
					.getAttribute(TRACING_CHECKED, (String) null);
			if (checked == null) {
				fPluginViewer.setAllChecked(true);
			} else if (checked.equals(TRACING_NONE)) {
				fPluginViewer.setAllChecked(false);
			} else {
				StringTokenizer tokenizer = new StringTokenizer(checked, ","); //$NON-NLS-1$
				ArrayList list = new ArrayList();
				PluginModelManager manager = PDECore.getDefault()
						.getModelManager();
				while (tokenizer.hasMoreTokens()) {
					String id = tokenizer.nextToken();
					ModelEntry entry = manager.findEntry(id);
					if (entry != null) {
						list.add(entry.getActiveModel());
					}
				}
				fPluginViewer.setCheckedElements(list.toArray());
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
	private IPluginModelBase getLastSelectedPlugin(ILaunchConfiguration config)
			throws CoreException {
		String pluginID = config.getAttribute(TRACING_SELECTED_PLUGIN,
				(String) null);
		if (pluginID != null) {
			ModelEntry entry = PDECore.getDefault().getModelManager()
					.findEntry(pluginID);
			return (entry == null) ? null : entry.getActiveModel();
		}
		return null;
	}
	public void performApply(ILaunchConfigurationWorkingCopy config) {
		boolean tracingEnabled = fTracingCheck.getSelection();
		config.setAttribute(TRACING, tracingEnabled);
		if (tracingEnabled) {
			IPluginModelBase model = getSelectedModel();
			String id = (model == null) ? null : model.getPluginBase().getId();
			config.setAttribute(TRACING_SELECTED_PLUGIN, id);
			boolean changes = false;
			for (Enumeration elements = fPropertySources.elements(); elements
					.hasMoreElements();) {
				TracingPropertySource source = (TracingPropertySource) elements
						.nextElement();
				if (source.isModified()) {
					changes = true;
					source.save();
				}
			}
			if (changes)
				config.setAttribute(TRACING_OPTIONS, fMasterOptions);
		} else {
			config.setAttribute(TRACING_SELECTED_PLUGIN, (String) null);
		}
		Object[] checked = fPluginViewer.getCheckedElements();
		if (checked.length == fPluginViewer.getTable().getItemCount()) {
			config.setAttribute(TRACING_CHECKED, (String) null);
		} else if (checked.length == 0) {
			config.setAttribute(TRACING_CHECKED, TRACING_NONE);
		} else {
			StringBuffer buffer = new StringBuffer();
			for (int i = 0; i < checked.length; i++) {
				IPluginModelBase model = (IPluginModelBase) checked[i];
				buffer.append(model.getPluginBase().getId());
				if (i < checked.length - 1)
					buffer.append(',');
			}
			config.setAttribute(TRACING_CHECKED, buffer.toString());
		}
	}
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(TRACING, false);
		config.setAttribute(TRACING_CHECKED, TRACING_NONE);
	}
	private void updatePropertyLabel(IPluginModelBase model) {
		String text = (model == null) ? PDEPlugin
				.getResourceString("TracingLauncherTab.options") : PDEPlugin //$NON-NLS-1$
				.getDefault().getLabelProvider().getText(model);
		fPropertyLabel.setText(text);
	}
	private void pluginSelected(IPluginModelBase model) {
		TracingPropertySource source = getPropertySource(model);
		if (source==null)
			fPageBook.showEmptyPage();
		else {
			if (!fPageBook.hasPage(model)) {
				Composite parent = fPageBook.createPage(model);
				source.createContents(parent);
			}
			fPageBook.showPage(model);
		}
		updatePropertyLabel(model);
	}
	public String getName() {
		return PDEPlugin.getResourceString("TracingLauncherTab.name"); //$NON-NLS-1$
	}
	public Image getImage() {
		return fImage;
	}
	private IPluginModelBase getSelectedModel() {
		if (fTracingCheck.isEnabled()) {
			Object item = ((IStructuredSelection) fPluginViewer.getSelection())
					.getFirstElement();
			if (item instanceof IPluginModelBase)
				return ((IPluginModelBase) item);
		}
		return null;
	}
}
