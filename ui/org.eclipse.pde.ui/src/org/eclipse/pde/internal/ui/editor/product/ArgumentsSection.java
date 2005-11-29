/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.pde.internal.core.iproduct.IArgumentsInfo;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.editor.text.TextUtil;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class ArgumentsSection extends PDESection {

	private static final String[] TAB_LABELS = new String[5];
	static {
		TAB_LABELS[IArgumentsInfo.L_ARGS_ALL] = "All Platforms";
		TAB_LABELS[IArgumentsInfo.L_ARGS_LINUX] = "Linux";
		TAB_LABELS[IArgumentsInfo.L_ARGS_MACOS] = "MacOS";
		TAB_LABELS[IArgumentsInfo.L_ARGS_SOLAR] = "Solaris";
		TAB_LABELS[IArgumentsInfo.L_ARGS_WIN32] = "Win32";
	}
	
	private FormEntry fVMArgs;
	private CTabFolder fTabFolder;
	private SourceViewer fSourceViewer;
	private Document fDocument;
	private boolean fIgnoreChange;
	private int fLastTab;

	public ArgumentsSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		createClient(getSection(), page.getEditor().getToolkit());
	}
	
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText(PDEUIMessages.ArgumentsSection_title); 
		section.setDescription(PDEUIMessages.ArgumentsSection_desc); 
		section.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Composite client = toolkit.createComposite(section);
		client.setLayout(new GridLayout());
		
		IActionBars actionBars = getPage().getPDEEditor().getEditorSite().getActionBars();
		
		toolkit.createLabel(client, PDEUIMessages.ArgumentsSection_program).setForeground(
				toolkit.getColors().getColor(FormColors.TITLE));
		fTabFolder = new CTabFolder(client, SWT.FLAT | SWT.TOP);
		toolkit.adapt(fTabFolder, true, true);
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		fTabFolder.setLayoutData(gd);
		gd.heightHint = 2;
		toolkit.getColors().initializeSectionToolBarColors();
		Color selectedColor1 = toolkit.getColors().getColor(FormColors.TB_BG);
		Color selectedColor2 = toolkit.getColors().getColor(FormColors.TB_GBG);
		fTabFolder.setSelectionBackground(new Color[] { selectedColor1,
				selectedColor2, toolkit.getColors().getBackground() },
				new int[] { 50, 100 }, true);

		fTabFolder.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateEditorInput();
			}
		});
		fTabFolder.setUnselectedImageVisible(false);
		
		fSourceViewer = new SourceViewer(client, null, SWT.MULTI | SWT.WRAP | SWT.H_SCROLL);
		fSourceViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateSelection(event.getSelection());
			}
		});
		StyledText styledText = fSourceViewer.getTextWidget();
		styledText.setFont(JFaceResources.getTextFont());
		styledText.setMenu(getPage().getPDEEditor().getContextMenu());
		styledText.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
		
		gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 50;
		fSourceViewer.getControl().setLayoutData(gd);
		
		fVMArgs = new FormEntry(client, toolkit, PDEUIMessages.ArgumentsSection_vm, SWT.MULTI|SWT.WRAP); 
		fVMArgs.getText().setLayoutData(new GridData(GridData.FILL_BOTH));		
		fVMArgs.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				getLauncherArguments().setVMArguments(entry.getValue());
			}
		});
		fVMArgs.setEditable(isEditable());
		
		createTabs();
		initialize();
		toolkit.paintBordersFor(client);
		section.setClient(client);	
	}
	
	private void createTabs() {
		for (int i = 0; i < TAB_LABELS.length; i++) {
			CTabItem item = new CTabItem(fTabFolder, SWT.NULL);
			item.setText(TAB_LABELS[i]);
			item.setImage(PDEPlugin.getDefault().getLabelProvider().get(
					PDEPluginImages.DESC_DOC_SECTION_OBJ));
		}
		fLastTab = 0;
		fTabFolder.setSelection(fLastTab);
	}
	
	public void initialize() {
		fDocument = new Document();
		fDocument.addDocumentListener(new IDocumentListener() {
			public void documentChanged(DocumentEvent e) {
				if (!fIgnoreChange) {
					markDirty();
				}
			}
			public void documentAboutToBeChanged(DocumentEvent e) {
			}
		});
		fSourceViewer.setDocument(fDocument);
	}
	
	private void refreshArguments() {
		String text = getLauncherArguments().getProgramArguments(fLastTab);
		if (text != null)
			text = TextUtil.createMultiLine(text, 60, false);
		fDocument.set(text == null ? "" : text); //$NON-NLS-1$
	}
	
	public void refresh() {
		refreshArguments();
		fVMArgs.setValue(getLauncherArguments().getVMArguments(), true);
		super.refresh();
	}
	
	public void commit(boolean onSave) {
		fVMArgs.commit();
		super.commit(onSave);
	}
	
	public void cancelEdit() {
		fVMArgs.cancelEdit();
		super.cancelEdit();
	}
	
	private void updateSelection(ISelection selection) {
		getPage().getPDEEditor().setSelection(selection);
	}
	
	private IArgumentsInfo getLauncherArguments() {
		IArgumentsInfo info = getProduct().getLauncherArguments();
		if (info == null) {
			info = getModel().getFactory().createLauncherArguments();
			getProduct().setLauncherArguments(info);
		}
		return info;
	}

	private IProduct getProduct() {
		return getModel().getProduct();
	}
	
	private IProductModel getModel() {
		return (IProductModel)getPage().getPDEEditor().getAggregateModel();
	}

	public boolean canPaste(Clipboard clipboard) {
		Display d = getSection().getDisplay();
		Control c = d.getFocusControl();
		if (c instanceof Text)
			return true;
		else if (c.equals(fSourceViewer.getControl()))
			return fSourceViewer.canDoOperation(SourceViewer.PASTE);
		return false;
	}
	
	private void updateEditorInput() {
		fIgnoreChange = true;
		IArgumentsInfo info = getLauncherArguments();
		String last = info.getProgramArguments(fLastTab);
		String curr = fDocument.get();
		if (last.length() != curr.length() || !last.equals(curr))
			info.setProgramArguments(fDocument.get(), fLastTab);
		fLastTab = fTabFolder.getSelectionIndex();
		refreshArguments();
		fIgnoreChange = false;
	}
	
	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(ActionFactory.CUT.getId())) {
			fSourceViewer.doOperation(SourceViewer.CUT);
			return true;
		} else if (
			actionId.equals(ActionFactory.COPY.getId())) {
			fSourceViewer.doOperation(SourceViewer.COPY);
			return true;
		} else if (
			actionId.equals(ActionFactory.PASTE.getId())) {
			fSourceViewer.doOperation(SourceViewer.PASTE);
			return true;
		} else if (
			actionId.equals(ActionFactory.SELECT_ALL.getId())) {
			fSourceViewer.doOperation(SourceViewer.SELECT_ALL);
			return true;
		} else if (
			actionId.equals(ActionFactory.DELETE.getId())) {
			fSourceViewer.doOperation(SourceViewer.DELETE);
			return true;
		} else if (
			actionId.equals(ActionFactory.UNDO.getId())) {
			fSourceViewer.doOperation(SourceViewer.UNDO);
			return true;
		} else if (
			actionId.equals(ActionFactory.REDO.getId())) {
			fSourceViewer.doOperation(SourceViewer.REDO);
			return true;
		}
		return false;
	}
}
