/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.views.properties.PropertySheetPage;

public class TracingLauncherTab
	extends AbstractLauncherTab
	implements ILauncherSettings {

	private Button fTracingCheck;
	private TreeViewer fPluginTreeViewer;
	private NamedElement fWorkspacePlugins;
	private NamedElement fExternalPlugins;
	private Properties fMasterOptions = new Properties();
	private ArrayList fExternalList;
	private ArrayList fWorkspaceList;
	private Hashtable fPropertySources = new Hashtable();
	private PropertySheetPage fPropertySheet;
	private SashForm fSashForm;
	private Composite fTableChild;
	private Label fPropertyLabel;
	private ToolItem fMaximizeItem;
	private Image fImage;

	class PluginContentProvider
		extends DefaultContentProvider
		implements ITreeContentProvider {
		public boolean hasChildren(Object parent) {
			return !(parent instanceof IPluginModel);
		}
		public Object[] getChildren(Object parent) {
			if (parent == fExternalPlugins) {
				return getExternalTraceablePlugins();
			}
			if (parent == fWorkspacePlugins) {
				return getWorkspaceTraceablePlugins();
			}
			return new Object[0];
		}
		public Object getParent(Object child) {
			if (child instanceof IPluginModel) {
				IPluginModel model = (IPluginModel) child;
				return (model.getUnderlyingResource() != null)
					? fWorkspacePlugins
					: fExternalPlugins;
			}
			return null;
		}
		public Object[] getElements(Object input) {
			return new Object[] { fWorkspacePlugins, fExternalPlugins };
		}
	}

	public TracingLauncherTab() {
		PDEPlugin.getDefault().getLabelProvider().connect(this);
		fImage = PDEPluginImages.DESC_DOC_SECTION_OBJ.createImage();
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		container.setLayout(new GridLayout());
		Dialog.applyDialogFont(container);
		
		createStartingSpace(container, 1);
		createEnableTracingButton(container);
		Label separator = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
		separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		createSashSection(container);
		
		setControl(container);		
		WorkbenchHelp.setHelp(container, IHelpContextIds.LAUNCHER_TRACING);
	}
	
	private void createEnableTracingButton(Composite container) {
		fTracingCheck = new Button(container, SWT.CHECK);
		fTracingCheck.setText(PDEPlugin.getResourceString("TracingLauncherTab.tracing"));
		fTracingCheck.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fTracingCheck.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				masterCheckChanged(true);
				updateLaunchConfigurationDialog();
			}
		});
	}

	private void createSashSection(Composite container) {
		fSashForm = new SashForm(container, SWT.VERTICAL);
		fSashForm.setLayoutData(new GridData(GridData.FILL_BOTH));

		createPluginViewer();
		createPropertySheetClient();
	}
	
	private void createPluginViewer() {
		Composite composite = new Composite(fSashForm, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		composite.setLayout(layout);

		Label label = new Label(composite, SWT.NULL);
		label.setText(PDEPlugin.getResourceString("TracingLauncherTab.plugins"));

		fPluginTreeViewer = new TreeViewer(composite, SWT.BORDER);
		fPluginTreeViewer.setContentProvider(new PluginContentProvider());
		fPluginTreeViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		fPluginTreeViewer.addFilter(new ViewerFilter() {
			public boolean select(Viewer v, Object parent, Object object) {
				if (object instanceof IPluginModel) {
					return ((IPluginModel) object).isEnabled();
				}
				return true;
			}
		});
		fPluginTreeViewer.setSorter(new ListUtil.PluginSorter() {
			public int category(Object obj) {
				if (obj == fWorkspacePlugins)
					return -1;
				if (obj == fExternalPlugins)
					return 1;
				return 0;
			}
		});
		fPluginTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent e) {
				Object item = ((IStructuredSelection) e.getSelection()).getFirstElement();
				if (item instanceof IPluginModel)
					pluginSelected((IPluginModel) item);
				else
					pluginSelected(null);
			}
		});
		Image pluginsImage =
		PDEPlugin.getDefault().getLabelProvider().get(
				PDEPluginImages.DESC_REQ_PLUGINS_OBJ);
		fWorkspacePlugins =
		new NamedElement(
				PDEPlugin.getResourceString("TracingLauncherTab.workspacePlugins"),
				pluginsImage);
		fExternalPlugins =
		new NamedElement(
				PDEPlugin.getResourceString("TracingLauncherTab.externalPlugins"),
				pluginsImage);
		
		fPluginTreeViewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
		fPluginTreeViewer.setInput(PDEPlugin.getDefault());
		fPluginTreeViewer.expandAll();
		
	}
	
	private void createPropertySheetClient() {
		fTableChild = new Composite(fSashForm, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		layout.verticalSpacing = 2;
		fTableChild.setLayout(layout);

		Composite titleBar = new Composite(fTableChild, SWT.NULL);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = layout.marginHeight = 0;
		titleBar.setLayout(layout);
		titleBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fPropertyLabel = new Label(titleBar, SWT.NULL);
		fPropertyLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		updatePropertyLabel(null);
		fPropertyLabel.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(MouseEvent e) {
				doMaximize(fSashForm.getMaximizedControl() == null);
			}
		});
		
		ToolBar toolbar = new ToolBar(titleBar, SWT.FLAT);
		fMaximizeItem = new ToolItem(toolbar, SWT.PUSH);
		fMaximizeItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				doMaximize(fSashForm.getMaximizedControl() == null);
			}
		});
		updateMaximizeItem();
		createPropertySheet(toolbar, fTableChild);		
	}

	private void doMaximize(boolean maximize) {
		Control maxControl = maximize ? fTableChild : null;
		fSashForm.setMaximizedControl(maxControl);
		updateMaximizeItem();
	}

	private void updateMaximizeItem() {
		boolean maximized = fSashForm.getMaximizedControl() != null;
		Image image;
		String tooltip;

		if (maximized) {
			image =
				PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_RESTORE);
			tooltip = PDEPlugin.getResourceString("TracingLauncherTab.restore");
		} else {
			image =
				PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_MAXIMIZE);
			tooltip = PDEPlugin.getResourceString("TracingLauncherTab.maximize");
		}
		fMaximizeItem.setImage(image);
		fMaximizeItem.setToolTipText(tooltip);
		fMaximizeItem.getParent().redraw();
	}

	protected void createPropertySheet(final ToolBar toolbar, Composite parent) {
		Composite composite = new Composite(parent, SWT.BORDER);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		fPropertySheet = new PropertySheetPage();
		fPropertySheet.createControl(composite);
		fPropertySheet.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
		fPropertySheet.makeContributions(
			//new NullMenuManager(),
			//new NullToolBarManager(),
			  new MenuManager(),
			  new ToolBarManager(),
			null);
	}

	public void dispose() {
		if (fPropertySheet != null)
			fPropertySheet.dispose();
		fImage.dispose();
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		super.dispose();
	}

	private ArrayList fillTraceableModelList(IPluginModelBase[] models) {
		ArrayList result = new ArrayList();
		for (int i = 0; i < models.length; i++) {
			if (TracingOptionsManager.isTraceable(models[i]))
				result.add(models[i]);
		}
		return result;
	}
	
	private IAdaptable getAdaptable(IPluginModel model) {
		if (model == null)
			return null;
		IAdaptable adaptable = (IAdaptable) fPropertySources.get(model);
		if (adaptable == null) {
			String id = model.getPlugin().getId();
			Hashtable defaults =
				PDECore.getDefault().getTracingOptionsManager().getTemplateTable(id);
			adaptable = new TracingPropertySource(model, fMasterOptions, defaults, this);
			fPropertySources.put(model, adaptable);
		}
		return adaptable;
	}
	
	private Object[] getExternalTraceablePlugins() {
		IPluginModel[] models =
			PDECore.getDefault().getExternalModelManager().getPluginModels();
		fExternalList = fillTraceableModelList(models);
		return fExternalList.toArray();
	}
	
	private Object[] getWorkspaceTraceablePlugins() {
		IPluginModelBase[] models =
			PDECore.getDefault().getWorkspaceModelManager().getAllModels();
		fWorkspaceList = fillTraceableModelList(models);
		return fWorkspaceList.toArray();
	}

	private void masterCheckChanged(boolean userChange) {
		boolean enabled = fTracingCheck.getSelection();
		fPluginTreeViewer.getTree().setEnabled(enabled);
		fPropertySheet.getControl().setEnabled(enabled);
	}

	public void initializeFrom(final ILaunchConfiguration config) {
		BusyIndicator.showWhile(getControl().getDisplay(), new Runnable() {
			public void run() {
				fMasterOptions.clear();
				fPropertySources.clear();
				try {
					fTracingCheck.setSelection(config.getAttribute(TRACING, false));

					Map options =
						(config
							.getAttribute(
								TRACING_OPTIONS,
								PDECore
									.getDefault()
									.getTracingOptionsManager()
									.getTracingTemplateCopy()));
					fMasterOptions.putAll(options);

					doMaximize(config.getAttribute(TRACING_VIEWER_MAXIMIZED, false));

					masterCheckChanged(false);
					pluginSelected(null);
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});
	}


	public void performApply(ILaunchConfigurationWorkingCopy config) {
		boolean tracingEnabled = fTracingCheck.getSelection();
		if (!tracingEnabled) return;
		
		config.setAttribute(TRACING, tracingEnabled);
		
		config.setAttribute(TRACING_VIEWER_MAXIMIZED, fSashForm.getMaximizedControl() != null);
		
		boolean changes = false;
		for (Enumeration enum = fPropertySources.elements(); enum.hasMoreElements();) {
			TracingPropertySource source = (TracingPropertySource) enum.nextElement();
			if (source.isModified()) {
				changes = true;
				source.save();
			}
		}
		if (changes)
			config.setAttribute(TRACING_OPTIONS, fMasterOptions);
	}


	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(TRACING, false);
	}

	private void updatePropertyLabel(IPluginModel model) {
		String text =
			(model == null)
				? PDEPlugin.getResourceString("TracingLauncherTab.options")
				: PDEPlugin.getDefault().getLabelProvider().getText(model);
		fPropertyLabel.setText(text);
	}

	private void pluginSelected(IPluginModel model) {
		IAdaptable adaptable = getAdaptable(model);
		ISelection selection =
			adaptable != null
				? new StructuredSelection(adaptable)
				: new StructuredSelection();
		fPropertySheet.selectionChanged(null, selection);
		updatePropertyLabel(model);
	}
	
	public String getName() {
		return PDEPlugin.getResourceString("TracingLauncherTab.name");
	}
	
	public Image getImage() {
		return fImage;
	}

}
