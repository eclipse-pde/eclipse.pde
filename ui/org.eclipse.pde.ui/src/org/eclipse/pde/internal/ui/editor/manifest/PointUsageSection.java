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
package org.eclipse.pde.internal.ui.editor.manifest;

import java.util.Vector;

import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.TableSection;
import org.eclipse.pde.internal.ui.elements.DefaultTableProvider;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.pde.internal.ui.search.PluginSearchActionGroup;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.update.ui.forms.internal.*;

public class PointUsageSection extends TableSection {
	public static final String SECTION_TITLE =
		"ManifestEditor.PointUsageSection.title";
	public static final String SECTION_DESC =
		"ManifestEditor.PointUsageSection.desc";
	public static final String SECTION_FDESC =
		"ManifestEditor.PointUsageSection.fdesc";
	public static final String POPUP_OPEN = "Actions.open.label";
	private TableViewer tableViewer;

	class UsageContentProvider extends DefaultTableProvider {
		public Object[] getElements(Object parent) {
			if (parent instanceof IPluginExtensionPoint)
				return getReferencedPlugins((IPluginExtensionPoint) parent);
			else
				return new Object[0];
		}
	}

	public PointUsageSection(ManifestExtensionPointPage page) {
		super(page, null);
		setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
		boolean fragment = ((ManifestEditor) page.getEditor()).isFragmentEditor();
		if (fragment)
			setDescription(PDEPlugin.getResourceString(SECTION_FDESC));
		else
			setDescription(PDEPlugin.getResourceString(SECTION_DESC));
		getTablePart().setEditable(false);
	}

	private void addReferencingPlugins(
		String pluginId,
		String fullPointId,
		IPluginModelBase[] models,
		Vector result) {
		for (int i = 0; i < models.length; i++) {
			IPluginModelBase model = models[i];
			if (model.isEnabled() == false)
				continue;
			IPluginBase pluginBase = model.getPluginBase();
			if (pluginBase.getId().equals(pluginId))
				continue;
			if (testUsage(pluginBase, fullPointId)) {
				result.addElement(pluginBase);
			}
		}
	}

	public Composite createClient(Composite parent, FormWidgetFactory factory) {
		Composite container = createClientContainer(parent, 2, factory);
		TablePart part = getTablePart();
		createViewerPartControl(container, SWT.SINGLE, 2, factory);
		tableViewer = part.getTableViewer();
		tableViewer.setContentProvider(new UsageContentProvider());
		tableViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		tableViewer.setSorter(ListUtil.NAME_SORTER);
		factory.paintBordersFor(container);
		return container;
	}

	protected void handleDoubleClick(IStructuredSelection selection) {
		handleOpen(selection);
	}

	private Object[] getReferencedPlugins(IPluginExtensionPoint currentPoint) {
		Vector result = new Vector();
		if (currentPoint == null)
			return new Object[0];

		String fullPointId = currentPoint.getFullId();

		// Test this plugin
		Object model = getFormPage().getModel();
		IPluginBase thisPluginBase = ((IPluginModelBase) model).getPluginBase();
		if (testUsage(thisPluginBase, fullPointId)) {
			result.add(thisPluginBase);
		}

		WorkspaceModelManager manager =
			PDECore.getDefault().getWorkspaceModelManager();
		addReferencingPlugins(
			thisPluginBase.getId(),
			fullPointId,
			manager.getPluginModels(),
			result);

		IExternalModelManager registry =
			PDECore.getDefault().getExternalModelManager();
		addReferencingPlugins(
			thisPluginBase.getId(),
			fullPointId,
			registry.getPluginModels(),
			result);
		return result.toArray();
	}

	protected void fillContextMenu(IMenuManager manager) {
		final IStructuredSelection sel =
			(IStructuredSelection) tableViewer.getSelection();
		if (!sel.isEmpty()) {
			manager.add(new Action(PDEPlugin.getResourceString(POPUP_OPEN)) {
				public void run() {
					handleOpen(sel);
				}
			});
			manager.add(new Separator());
		}
		// defect 19558
		getFormPage().getEditor().getContributor().contextMenuAboutToShow(
			manager,
			false);
		if (!sel.isEmpty()) {
			manager.add(new Separator());
			PluginSearchActionGroup actionGroup = new PluginSearchActionGroup();
			actionGroup.setContext(new ActionContext(sel));
			actionGroup.fillContextMenu(manager);
		}
	}
	
	private void handleOpen(IStructuredSelection selection) {
		IPluginBase pluginToOpen = (IPluginBase) selection.getFirstElement();
		if (pluginToOpen != null) {
			ManifestEditor.openPluginEditor(pluginToOpen);
		} else {
			Display.getCurrent().beep();
		}
	}

	public void initialize(Object input) {
		IPluginModelBase model = (IPluginModelBase) input;
		model.addModelChangedListener(this);
	}

	public void dispose() {
		IPluginModelBase model = (IPluginModelBase) getFormPage().getModel();
		model.removeModelChangedListener(this);
		super.dispose();
	}

	public void sectionChanged(
		FormSection source,
		int changeType,
		Object changeObject) {
		inputChanged((IPluginExtensionPoint) changeObject);
	}

	private boolean testUsage(IPluginBase pluginBase, String pointId) {
		IPluginExtension[] extensions = pluginBase.getExtensions();
		for (int i = 0; i < extensions.length; i++) {
			IPluginExtension extension = extensions[i];
			if (extension.getPoint().equals(pointId)) {
				return true;
			}
		}
		return false;
	}

	private void inputChanged(IPluginExtensionPoint extensionPoint) {
		tableViewer.setInput(extensionPoint);
	}
	public void modelChanged(IModelChangedEvent event) {
		/*
		int type = event.getChangeType();
		if (type == IModelChangedEvent.WORLD_CHANGED)
			needsUpdate = true;
		else {
			Object[] objects = event.getChangedObjects();
			if (objects[0] instanceof IPluginImport) {
				needsUpdate = true;
			}
		}
		if (getFormPage().isVisible())
			update();
		*/
	}
	public void update() {
		tableViewer.refresh();
	}
}
