/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Common Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
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
import org.eclipse.ui.dialogs.*;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.events.*;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.model.*;
import org.eclipse.ui.part.*;
import org.eclipse.ui.views.navigator.*;

public class ExtensionPointDetails extends AbstractFormPart implements IDetailsPage, IContextPart {
	private IPluginExtensionPoint fInput;
	private FormEntry fIdEntry;
	private FormEntry fNameEntry;
	private FormEntry fSchemaEntry;
	private FormText fRichText;
	private String fRichTextData;
	
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
		glayout.numColumns = 3;
		if (paintedBorder) glayout.verticalSpacing = 7;
		client.setLayout(glayout);
		GridData gd = new GridData();
		gd.horizontalSpan = 3;
		
		fIdEntry = new FormEntry(client, toolkit, "ID:", null, false);
		fIdEntry.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				if (fInput != null) {
					try {
						fInput.setId(fIdEntry.getValue());
					} catch (CoreException e) {
						PDEPlugin.logException(e);
					}
				}
			}
		});
		fNameEntry = new FormEntry(client, toolkit, "Name:", null, false);
		fNameEntry.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				if (fInput != null)
					try {
						fInput.setName(fNameEntry.getValue());
					} catch (CoreException e) {
						PDEPlugin.logException(e);
					}
			}
		});
		fSchemaEntry = new FormEntry(client, toolkit, "Schema:", "Browse...", true);
		fSchemaEntry.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				if (fInput != null) {
					try {
						fInput.setSchema(fSchemaEntry.getValue());
					} catch (CoreException e) {
						PDEPlugin.logException(e);
					}
					updateRichText();
				}
			}

			public void linkActivated(HyperlinkEvent e) {
				IProject project = getPage().getPDEEditor().getCommonProject();
				if (fSchemaEntry.getValue() == null || fSchemaEntry.getValue().length() ==0){
					generateSchema();
					return;
				}
				IFile file = project.getFile(fSchemaEntry.getValue());
				if (file.exists())
					openSchemaFile(file);
				else
					generateSchema();
			}

			public void browseButtonSelected(FormEntry entry) {
				final IProject project = getPage().getPDEEditor().getCommonProject();
				ElementTreeSelectionDialog dialog =
					new ElementTreeSelectionDialog(
						PDEPlugin.getActiveWorkbenchShell(),
						new WorkbenchLabelProvider(),
						new WorkbenchContentProvider());
				dialog.setTitle(PDEPlugin.getResourceString("ManifestEditor.ExtensionPointDetails.schemaLocation.title"));
				dialog.setMessage(PDEPlugin.getResourceString("ManifestEditor.ExtensionPointDetails.schemaLocation.desc"));
				dialog.setDoubleClickSelects(false);
				dialog.setAllowMultiple(false);
				dialog.addFilter(new ViewerFilter(){
					public boolean select(Viewer viewer, Object parent,
							Object element) {
						if (element instanceof IFile){
							String ext = ((IFile)element).getFullPath().getFileExtension();
							return ext.equals("exsd") || ext.equals("mxsd");
						} else if (element instanceof IContainer){ // i.e. IProject, IFolder
							try {
								IResource[] resources = ((IContainer)element).members();
								for (int i = 0; i < resources.length; i++){
									if (select(viewer, parent, resources[i]))
										return true;
								}
							} catch (CoreException e) {
								PDEPlugin.logException(e);
							}
						}
						return false;
					}
				});
				dialog.setValidator(new ISelectionStatusValidator() {
					public IStatus validate(Object[] selection) {
						IPluginModelBase model = (IPluginModelBase) getPage()
								.getPDEEditor().getAggregateModel();
						String pluginName = model.getPluginBase().getId();

						if (selection == null || selection.length != 1
								|| !(selection[0] instanceof IFile))
							return new Status(
									IStatus.ERROR,
									pluginName,
									IStatus.ERROR,
									PDEPlugin
											.getResourceString("ManifestEditor.ExtensionPointDetails.validate.errorStatus"),
									null);
						IFile file = (IFile) selection[0];
						String ext = file.getFullPath().getFileExtension();
						if (ext.equals("exsd") || ext.equals("mxsd"))
							return new Status(IStatus.OK, pluginName,
									IStatus.OK, "", null);
						return new Status(
								IStatus.ERROR,
								pluginName,
								IStatus.ERROR,
								PDEPlugin
										.getResourceString("ManifestEditor.ExtensionPointDetails.validate.errorStatus"),
								null);
					}
				});
				dialog.setDoubleClickSelects(true);
				dialog.setStatusLineAboveButtons(true);
				dialog.setInput(project);
				dialog.setSorter(new ResourceSorter(ResourceSorter.NAME));
				String filePath = fSchemaEntry.getValue();
				if (filePath!=null && filePath.length()!=0 && project.exists(new Path(filePath)))
					dialog.setInitialSelection(project.getFile(new Path(filePath)));
				else
					dialog.setInitialSelection(null);
				if (dialog.open() == ElementTreeSelectionDialog.OK) {
					Object[] elements = dialog.getResult();
					if (elements.length >0){
						IResource elem = (IResource) elements[0];
						fSchemaEntry.setValue(elem.getProjectRelativePath().toString());
					}
				}
			}
		});
		createSpacer(toolkit, client, 2);
		fRichText = toolkit.createFormText(parent, true);
		td = new TableWrapData(TableWrapData.FILL, TableWrapData.TOP);
		td.grabHorizontal = true;
		td.indent = 10;
		fRichText.setLayoutData(td);
		fRichText.setImage("schema", PDEPlugin.getDefault().getLabelProvider().get(
				PDEPluginImages.DESC_SCHEMA_OBJ));
		fRichText.setImage("desc", PDEPlugin.getDefault().getLabelProvider().get(
				PDEPluginImages.DESC_DOC_SECTION_OBJ));
		fRichText.setImage("search", PDEPlugin.getDefault().getLabelProvider().get(
				PDEPluginImages.DESC_PSEARCH_OBJ));
		fRichText.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				if (e.getHref().equals("search")) {
					FindReferencesAction pluginReferencesAction = new FindReferencesAction(fInput);
					pluginReferencesAction.run();
				} else {
					ShowDescriptionAction showDescAction = new ShowDescriptionAction(fInput);
					showDescAction.run();
				}
			}
		});
		
		fIdEntry.setEditable(isEditable());
		fNameEntry.setEditable(isEditable());
		fSchemaEntry.setEditable(isEditable());
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
		fIdEntry.setValue(
				fInput != null && fInput.getId() != null ? fInput.getId() : "",
				true);
		fNameEntry.setValue(fInput != null && fInput.getName() != null ? fInput
				.getName() : "", true);
		fSchemaEntry.setValue(fInput != null && fInput.getSchema() != null ? fInput
				.getSchema() : "", true);
		updateRichText();
	}
	private void updateRichText() {
		boolean hasSchema = fSchemaEntry.getValue().length() > 0;
		if (hasSchema && fRichTextData == SCHEMA_RTEXT_DATA)
			return;
		if (!hasSchema && fRichTextData == NO_SCHEMA_RTEXT_DATA)
			return;
		fRichTextData = hasSchema ? SCHEMA_RTEXT_DATA : NO_SCHEMA_RTEXT_DATA;
		fRichText.setText(fRichTextData, true, false);
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
				NewSchemaFileWizard wizard =
					new NewSchemaFileWizard(project, fInput, true);
				WizardDialog dialog =
					new WizardDialog(
						PDEPlugin.getActiveWorkbenchShell(),
						wizard);
				dialog.create();
				SWTUtil.setDialogSize(dialog, 400, 450);
				if(dialog.open() == WizardDialog.OK)
					update();
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
			fInput = (IPluginExtensionPoint) ssel.getFirstElement();
		} else
			fInput = null;
		update();
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IDetailsPage#commit()
	 */
	public void commit(boolean onSave) {
		fIdEntry.commit();
		fNameEntry.commit();
		fSchemaEntry.commit();
		super.commit(onSave);
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IDetailsPage#setFocus()
	 */
	public void setFocus() {
		fIdEntry.getText().setFocus();
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