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
package org.eclipse.pde.internal.ui.editor.plugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IModelChangeProvider;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;

public abstract class GeneralInfoSection extends PDESection {
	private FormEntry fIdEntry;
	private FormEntry fVersionEntry;
	private FormEntry fNameEntry;
	private FormEntry fProviderEntry;

	public GeneralInfoSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		createClient(getSection(), page.getEditor().getToolkit());
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.neweditor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section,
	 *      org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText(PDEPlugin.getResourceString("ManifestEditor.PluginSpecSection.title")); //$NON-NLS-1$
		section.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		section.setDescription(getSectionDescription()); 
		
		Composite client = toolkit.createComposite(section);
		TableWrapLayout layout = new TableWrapLayout();
		layout.leftMargin = layout.rightMargin = toolkit.getBorderStyle() != SWT.NULL ? 0 : 2;
		layout.numColumns = 3;
		client.setLayout(layout);
		section.setClient(client);
		
		IActionBars actionBars = getPage().getPDEEditor().getEditorSite().getActionBars();
		createIDEntry(client, toolkit, actionBars);
		createVersionEntry(client, toolkit, actionBars);
		createNameEntry(client, toolkit, actionBars);
		createProviderEntry(client, toolkit, actionBars);		
		createSpecificControls(client, toolkit, actionBars);
		toolkit.paintBordersFor(client);
		
		IBaseModel model = getPage().getModel();
		if (model instanceof IModelChangeProvider)
			((IModelChangeProvider) model).addModelChangedListener(this);
	}
	
	protected abstract String getSectionDescription();
	
	protected abstract void createSpecificControls(Composite parent, FormToolkit toolkit, IActionBars actionBars);
	
	protected  IPluginBase getPluginBase() {
		IBaseModel model = getPage().getPDEEditor().getAggregateModel();
		return ((IPluginModelBase) model).getPluginBase();
	}
	
	private void createIDEntry(Composite client, FormToolkit toolkit, IActionBars actionBars) {
		fIdEntry = new FormEntry(client, toolkit, PDEPlugin.getResourceString("GeneralInfoSection.id"), null, false); //$NON-NLS-1$
		fIdEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				try {
					getPluginBase().setId(entry.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});
		fIdEntry.setEditable(isEditable());
	}
	
	private void createVersionEntry(Composite client, FormToolkit toolkit, IActionBars actionBars) {
		fVersionEntry = new FormEntry(client, toolkit, PDEPlugin.getResourceString("GeneralInfoSection.version"), null, false); //$NON-NLS-1$
		fVersionEntry.setFormEntryListener(new FormEntryAdapter(this,actionBars) {
			public void textValueChanged(FormEntry entry) {
				try {
					getPluginBase().setVersion(entry.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});
		fVersionEntry.setEditable(isEditable());
	}
	
	private void createNameEntry(Composite client, FormToolkit toolkit,IActionBars actionBars) {
		fNameEntry = new FormEntry(client, toolkit, PDEPlugin.getResourceString("GeneralInfoSection.name"), null, false); //$NON-NLS-1$
		fNameEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				try {
					getPluginBase().setName(entry.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});
		fNameEntry.setEditable(isEditable());
	}
	
	private void createProviderEntry(Composite client, FormToolkit toolkit, IActionBars actionBars) {
		fProviderEntry = new FormEntry(client, toolkit, PDEPlugin.getResourceString("GeneralInfoSection.provider"), null, false);//$NON-NLS-1$
		fProviderEntry.setFormEntryListener(new FormEntryAdapter(this,actionBars) {
			public void textValueChanged(FormEntry entry) {
				try {
					getPluginBase().setProviderName(entry.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});
		fProviderEntry.setEditable(isEditable());
	}
	
	public void commit(boolean onSave) {
		fIdEntry.commit();
		fVersionEntry.commit();
		fNameEntry.commit();
		fProviderEntry.commit();
		super.commit(onSave);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#modelChanged(org.eclipse.pde.core.IModelChangedEvent)
	 */
	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
			return;
		}
		refresh();
		if (e.getChangeType() == IModelChangedEvent.CHANGE) {
			Object obj = e.getChangedObjects()[0];
			if (obj instanceof IPluginBase) {
				String property = e.getChangedProperty();
				if (property != null && property.equals(getPage().getPDEEditor().getTitleProperty()))
					getPage().getPDEEditor().updateTitle();
			}
		}
	}

	public void refresh() {
		IPluginModelBase model = (IPluginModelBase) getPage().getPDEEditor()
				.getContextManager().getAggregateModel();
		IPluginBase pluginBase = model.getPluginBase();
		fIdEntry.setValue(pluginBase.getId(), true);
		fNameEntry.setValue(pluginBase.getName(), true);
		fVersionEntry.setValue(pluginBase.getVersion(), true);
		fProviderEntry.setValue(pluginBase.getProviderName(), true);
		getPage().getPDEEditor().updateTitle();
		super.refresh();
	}
	
	public void cancelEdit() {
		fIdEntry.cancelEdit();
		fNameEntry.cancelEdit();
		fVersionEntry.cancelEdit();
		fProviderEntry.cancelEdit();
		super.cancelEdit();
	}
	
	public void dispose() {
		IBaseModel model = getPage().getModel();
		if (model instanceof IModelChangeProvider)
			((IModelChangeProvider) model).removeModelChangedListener(this);
		super.dispose();
	}
	
	public boolean canPaste(Clipboard clipboard) {
		Display d = getSection().getDisplay();
		Control c = d.getFocusControl();
		if (c instanceof Text)
			return true;
		return false;
	}
}
