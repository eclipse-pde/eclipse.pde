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
package org.eclipse.pde.internal.ui.neweditor.plugin;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.neweditor.*;
import org.eclipse.pde.internal.ui.newparts.*;
import org.eclipse.pde.internal.ui.search.*;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.pde.internal.ui.wizards.extension.*;
import org.eclipse.swt.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.events.*;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.part.*;
/**
 * @author dejan
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class ExtensionPointDetails extends AbstractFormPart implements IDetailsPage, IContextPart {
	private IPluginExtensionPoint input;
	private FormEntry id;
	private FormEntry name;
	private FormEntry schema;
	private FormText rtext;
	private String rtextData;
	
	private static final String SCHEMA_RTEXT_DATA = "<form>"
			+ "<p><img href=\"search\"/> <a href=\"search\">Find references</a></p>"
			+ "<p><img href=\"desc\"/> <a href=\"desc\">Open extension point description</a></p>"
			+ "</form>";
	private static final String NO_SCHEMA_RTEXT_DATA = "<form><p><img href=\"search\"/> <a href=\"search\">Find references</a></p>"
			+ "</form>";
	public ExtensionPointDetails() {
	}
	public String getContextId() {
		return PluginInputContext.CONTEXT_ID;
	}
	public void fireSaveNeeded() {
		markDirty();
		getPage().getPDEEditor().fireSaveNeeded(getContextId(), false);
	}
	public PDEFormPage getPage() {
		return (PDEFormPage)getManagedForm().getContainer();
	}
	public boolean isEditable() {
		return getPage().getPDEEditor().getAggregateModel().isEditable();
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IDetailsPage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	public void createContents(Composite parent) {
		TableWrapLayout layout = new TableWrapLayout();
		layout.topMargin = 0;
		layout.leftMargin = 5;
		layout.rightMargin = 0;
		layout.bottomMargin = 0;
		parent.setLayout(layout);
		FormToolkit toolkit = getManagedForm().getToolkit();
		Section section = toolkit.createSection(parent, Section.DESCRIPTION|Section.TITLE_BAR);
		section.clientVerticalSpacing = PDESection.CLIENT_VSPACING;
		section.marginHeight = 5;
		section.marginWidth = 5;
		section.setText("Extension Point Details");
		section
				.setDescription("Set the properties of the selected extension point.");
		TableWrapData td = new TableWrapData(TableWrapData.FILL,
				TableWrapData.TOP);
		td.grabHorizontal = true;
		section.setLayoutData(td);
		//toolkit.createCompositeSeparator(section);
		Composite client = toolkit.createComposite(section);
		GridLayout glayout = new GridLayout();
		boolean paintedBorder = toolkit.getBorderStyle()!=SWT.BORDER;
		glayout.marginWidth = glayout.marginHeight = 2;//paintedBorder?2:0;
		glayout.numColumns = 2;
		if (paintedBorder) glayout.verticalSpacing = 7;
		client.setLayout(glayout);
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		id = new FormEntry(client, toolkit, "Id:", null, false);
		id.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				if (input != null) {
					try {
						input.setId(id.getValue());
					} catch (CoreException e) {
						PDEPlugin.logException(e);
					}
				}
			}
		});
		name = new FormEntry(client, toolkit, "Name:", null, false);
		name.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				if (input != null)
					try {
						input.setName(name.getValue());
					} catch (CoreException e) {
						PDEPlugin.logException(e);
					}
			}
		});
		schema = new FormEntry(client, toolkit, "Schema:", null, true);
		schema.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				if (input != null) {
					try {
						input.setSchema(schema.getValue());
					} catch (CoreException e) {
						PDEPlugin.logException(e);
					}
					updateRichText();
				}
			}

			public void linkActivated(HyperlinkEvent e) {
				IProject project = getPage().getPDEEditor().getCommonProject();
				IFile file = project.getFile(schema.getValue());
				if (file.exists())
					openSchemaFile(file);
				else
					generateSchema();
			}
		});
		createSpacer(toolkit, client, 2);
		rtext = toolkit.createFormText(parent, true);
		td = new TableWrapData(TableWrapData.FILL, TableWrapData.TOP);
		td.grabHorizontal = true;
		td.indent = 10;
		rtext.setLayoutData(td);
		rtext.setImage("schema", PDEPlugin.getDefault().getLabelProvider().get(
				PDEPluginImages.DESC_SCHEMA_OBJ));
		rtext.setImage("desc", PDEPlugin.getDefault().getLabelProvider().get(
				PDEPluginImages.DESC_DOC_SECTION_OBJ));
		rtext.setImage("search", PDEPlugin.getDefault().getLabelProvider().get(
				PDEPluginImages.DESC_PSEARCH_OBJ));
		rtext.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				if (e.getHref().equals("search")) {
					FindReferencesAction pluginReferencesAction = new FindReferencesAction(input);
					pluginReferencesAction.run();
				} else {
					ShowDescriptionAction showDescAction = new ShowDescriptionAction(input);
					showDescAction.run();
				}
			}
		});
		
		id.setEditable(isEditable());
		name.setEditable(isEditable());
		schema.setEditable(isEditable());
		toolkit.paintBordersFor(client);
		section.setClient(client);
	}
	private void createSpacer(FormToolkit toolkit, Composite parent, int span) {
		Label spacer = toolkit.createLabel(parent, "");
		GridData gd = new GridData();
		gd.horizontalSpan = span;
		spacer.setLayoutData(gd);
	}
	private void update() {
		id.setValue(
				input != null && input.getId() != null ? input.getId() : "",
				true);
		name.setValue(input != null && input.getName() != null ? input
				.getName() : "", true);
		schema.setValue(input != null && input.getSchema() != null ? input
				.getSchema() : "", true);
		updateRichText();
	}
	private void updateRichText() {
		boolean hasSchema = schema.getValue().length() > 0;
		if (hasSchema && rtextData == SCHEMA_RTEXT_DATA)
			return;
		if (!hasSchema && rtextData == NO_SCHEMA_RTEXT_DATA)
			return;
		rtextData = hasSchema ? SCHEMA_RTEXT_DATA : NO_SCHEMA_RTEXT_DATA;
		rtext.setText(rtextData, true, false);
		getManagedForm().getForm().reflow(true);
	}
	private void openSchemaFile(final IFile file) {
		final IWorkbenchWindow ww = PDEPlugin.getActiveWorkbenchWindow();

		Display d = ww.getShell().getDisplay();
		d.asyncExec(new Runnable() {
			public void run() {
				try {
					String editorId = PDEPlugin.SCHEMA_EDITOR_ID;
					ww.getActivePage().openEditor(
						new FileEditorInput(file),
						editorId);
				} catch (PartInitException e) {
					PDEPlugin.logException(e);
				}
			}
		});
	}
	
	private void generateSchema() {
		final IProject project = getPage().getPDEEditor().getCommonProject();
		BusyIndicator
			.showWhile(getPage().getPartControl().getDisplay(), new Runnable() {
			public void run() {
				NewExtensionPointWizard wizard =
					new NewExtensionPointWizard(
						project,
						(IPluginModelBase) getPage().getModel());
				WizardDialog dialog =
					new WizardDialog(
						PDEPlugin.getActiveWorkbenchShell(),
						wizard);
				dialog.create();
				SWTUtil.setDialogSize(dialog, 400, 450);
				dialog.open();
			}
		});
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IDetailsPage#inputChanged(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void selectionChanged(IFormPart masterPart, ISelection selection) {
		IStructuredSelection ssel = (IStructuredSelection) selection;
		if (ssel.size() == 1) {
			input = (IPluginExtensionPoint) ssel.getFirstElement();
		} else
			input = null;
		update();
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IDetailsPage#commit()
	 */
	public void commit(boolean onSave) {
		id.commit();
		name.commit();
		schema.commit();
		super.commit(onSave);
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IDetailsPage#setFocus()
	 */
	public void setFocus() {
		id.getText().setFocus();
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IDetailsPage#refresh()
	 */
	public void refresh() {
		update();
		super.refresh();
	}
}