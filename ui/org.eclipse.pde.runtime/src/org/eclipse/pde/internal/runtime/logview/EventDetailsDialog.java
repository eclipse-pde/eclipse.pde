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
package org.eclipse.pde.internal.runtime.logview;

import java.io.*;
import java.text.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.runtime.*;
import org.eclipse.swt.*;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.update.ui.forms.internal.*;

public class EventDetailsDialog extends Dialog {
	private LogEntry entry, parentEntry;
	private LogViewLabelProvider labelProvider;
	private static int COPY_ID = 22;
	private TableTreeViewer provider;
	private int elementNum, totalElementCount;
	private LogEntry[] entryChildren;
	private int childIndex = 0;
	private boolean isOpen;
	
	private Label dateLabel;
	private Label severityImageLabel;
	private Label severityLabel;
	private Text msgText;
	private Text stackTraceText;
	private Text sessionDataText;
	private Clipboard clipboard;
	private Button copyButton;
	private Button backButton;
	private Button nextButton;
	private Image imgNextEnabled, imgNextDisabled;
	private Image imgPrevEnabled, imgPrevDisabled;
	private Image imgCopyEnabled;
	
	// sorting
	private static int ASCENDING = 1;
	private static int DESCENDING = -1;
	private Comparator comparator = null;
	private Collator collator;


	/**
	 * @param parentShell
	 *            shell in which dialog is displayed
	 */
	protected EventDetailsDialog(Shell parentShell, IAdaptable selection, ISelectionProvider provider) {
		super(parentShell);
		labelProvider = new LogViewLabelProvider();
		this.provider = (TableTreeViewer) provider;
		this.entry = (LogEntry)selection;
		setShellStyle(SWT.MODELESS | SWT.MAX | SWT.RESIZE | SWT.CLOSE | SWT.BORDER | SWT.TITLE);
		clipboard = new Clipboard(parentShell.getDisplay());
		initialize();
		createImages();
		collator = Collator.getInstance();
	}

	private void initialize() {
		elementNum = getParentElementNum();
		totalElementCount = provider.getTableTree().getTable().getItemCount() - getVisibleChildrenCount();
		parentEntry = (LogEntry) entry.getParent(entry);
		if (isChild(entry)){
			setEntryChildren(parentEntry);
			for (int i = 0; i<entryChildren.length; i++){
				if (entryChildren[i].getMessage().equals(entry.getMessage())
						&& entryChildren[i].getDate().equals(entry.getDate())){
					childIndex = i;
					break;
				}
			}
		}
	}
	
	private void createImages(){
		imgCopyEnabled =
			PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_COPY).createImage(
				true);
		imgNextDisabled = PDERuntimePluginImages.DESC_NEXT_EVENT_DISABLED.createImage(true);
		imgPrevDisabled = PDERuntimePluginImages.DESC_PREV_EVENT_DISABLED.createImage(true);
		imgPrevEnabled = PDERuntimePluginImages.DESC_PREV_EVENT.createImage(true);
		imgNextEnabled = PDERuntimePluginImages.DESC_NEXT_EVENT.createImage(true);
	}

	private boolean isChild(LogEntry entry) {
		return entry.getParent(entry) != null;
	}
	
	public boolean isOpen(){
		return isOpen;
	}

	public int open(){
		isOpen = true;
		return super.open();
	}
	
	public boolean close() {
		isOpen = false;
		imgCopyEnabled.dispose();
		imgNextDisabled.dispose();
		imgNextEnabled.dispose();
		imgPrevDisabled.dispose();
		imgPrevEnabled.dispose();
		return super.close();
	}

	public void create() {
		super.create();
		applyDialogFont(buttonBar);
		getButton(IDialogConstants.OK_ID).setFocus();
	}

	protected void buttonPressed(int buttonId) {
		if (IDialogConstants.OK_ID == buttonId)
			okPressed();
		else if (IDialogConstants.CANCEL_ID == buttonId)
			cancelPressed();
		else if (IDialogConstants.BACK_ID == buttonId)
			backPressed();
		else if (IDialogConstants.NEXT_ID == buttonId)
			nextPressed();
		else if (COPY_ID == buttonId)
			copyPressed();
	}

	protected void backPressed() {
		if (isChild(entry)) {
			if (childIndex > 0) {
				childIndex--;
				entry = entryChildren[childIndex];
			} else
				entry = parentEntry;
		} else {
			if (elementNum - 1 >= 0)
				elementNum -= 1;
			entry = (LogEntry) ((TableTreeViewer) provider).getElementAt(elementNum);
		}
		setEntrySelectionInTable();
	}

	protected void nextPressed() {
		if (isChild(entry) && childIndex < entryChildren.length-1) {
			childIndex++;
			entry = entryChildren[childIndex];
		} else {
			if (elementNum + 1 < totalElementCount)
				elementNum += 1;
			entry = (LogEntry) ((TableTreeViewer) provider).getElementAt(elementNum);
		}
		setEntrySelectionInTable();
	}

	protected void copyPressed() {
		StringWriter writer = new StringWriter();
		PrintWriter pwriter = new PrintWriter(writer);

		entry.write(pwriter);
		pwriter.flush();
		String textVersion = writer.toString();
		try {
			pwriter.close();
			writer.close();
		} catch (IOException e) {
		}
		// set the clipboard contents
		clipboard.setContents(new Object[] { textVersion }, new Transfer[] { TextTransfer.getInstance()});	
	}
	
	private void setComparator(byte sortType, final int sortOrder){
		if (sortType == LogView.DATE){
			comparator = new Comparator(){
				public int compare(Object e1, Object e2) {
					try {
						SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss.SS"); //$NON-NLS-1$
						Date date1 = formatter.parse(((LogEntry)e1).getDate());
						Date date2 = formatter.parse(((LogEntry)e2).getDate());
						if (sortOrder == ASCENDING) {
							return date1.before(date2) ? -1 : 1;
						} else {
							return date1.after(date2) ? -1 : 1;
						}
					} catch (ParseException e) {
					}
					return 0;
				}
			};
		} else if (sortType == LogView.PLUGIN){
			comparator = new Comparator(){
				public int compare(Object e1, Object e2) {
					LogEntry entry1 = (LogEntry)e1;
					LogEntry entry2 = (LogEntry)e2;
					return collator.compare(entry1.getPluginId(), entry2.getPluginId()) * sortOrder;
				}
			};
		} else {
			comparator = new Comparator(){
				public int compare(Object e1, Object e2) {
					LogEntry entry1 = (LogEntry)e1;
					LogEntry entry2 = (LogEntry)e2;
					return collator.compare(entry1.getMessage(), entry2.getMessage()) * sortOrder;
				}
			};
		}
	}
	
	public void resetSelection(IAdaptable selectedEntry, byte sortType, int sortOrder){
		setComparator(sortType, sortOrder);
		resetSelection(selectedEntry);
	}
	
	public void resetSelection(IAdaptable selectedEntry){
		if (entry.equals((LogEntry)selectedEntry) &&
				elementNum == getParentElementNum()){
			updateProperties();
			return;
		}
		entry = (LogEntry)selectedEntry;
		initialize();
		updateProperties();
	}
	
	private void setEntrySelectionInTable(){
		ISelection selection = new StructuredSelection(entry);
		provider.setSelection(selection);
	}
	
	public void updateProperties() {		
		if (entry.hasChildren()) {
			setEntryChildren(entry);
		} else if (isChild(entry)){
			parentEntry = (LogEntry) entry.getParent(entry);
			setEntryChildren(parentEntry);
		}

		totalElementCount = provider.getTableTree().getTable().getItemCount() - getVisibleChildrenCount();
		dateLabel.setText(entry.getDate());
		severityImageLabel.setImage(labelProvider.getColumnImage(entry, 1));
		severityLabel.setText(entry.getSeverityText());
		msgText.setText(entry.getMessage());
		String stack = entry.getStack();
		if (stack != null) {
			stackTraceText.setText(stack);
		} else {
			stackTraceText.setText(PDERuntimePlugin.getResourceString("LogView.preview.noStack"));
		}
		LogSession session = entry.getSession();
		if (session != null && session.getSessionData() != null)
			sessionDataText.setText(session.getSessionData());

		updateButtons();
	}
	
	private void updateButtons(){
		if (isChild(entry)){
			backButton.setImage(imgPrevEnabled);
			nextButton.setImage((childIndex == entryChildren.length-1 && elementNum == totalElementCount - 1) ? imgNextDisabled : imgNextEnabled);
			backButton.setEnabled(true);
			nextButton.setEnabled(childIndex < entryChildren.length-1 || elementNum < totalElementCount - 1);
		} else {
			backButton.setImage(elementNum == 0 ? imgPrevDisabled : imgPrevEnabled);
			nextButton.setImage(elementNum == totalElementCount - 1 ? imgNextDisabled : imgNextEnabled);
			backButton.setEnabled(elementNum != 0);
			nextButton.setEnabled(elementNum != totalElementCount - 1);
		}
	}
	
	private void setEntryChildren(LogEntry parent){
		Object[] children = parent.getChildren(parent);
		if (comparator != null)
			Arrays.sort(children, comparator);
		entryChildren = new LogEntry[children.length];
		System.arraycopy(children,0,entryChildren,0,children.length);
	}
	
	private int getParentElementNum(){
		LogEntry itemEntry = (LogEntry)((IStructuredSelection)provider.getSelection()).getFirstElement();
		if (isChild(itemEntry))
			itemEntry = (LogEntry)itemEntry.getParent(itemEntry);
		
		for (int i = 0; i<provider.getTableTree().getItemCount(); i++){
			try {
				LogEntry littleEntry = (LogEntry)provider.getElementAt(i);
				if (itemEntry.equals(littleEntry)){
					return i;
				}
			} catch (Exception e){
				
			}
		}
		return 0;
	}
	
	private int getVisibleChildrenCount(){
		Object[] elements = provider.getVisibleExpandedElements();
		LogEntry[] expandedElements = new LogEntry[elements.length];
		System.arraycopy(elements, 0, expandedElements, 0, elements.length);
		int count = 0;
		for (int i = 0; i<expandedElements.length; i++){
			count += expandedElements[i].getChildren(expandedElements[i]).length;
		}
		return count;
	}
	
	protected Control createDialogArea(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		container.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_BOTH);
		container.setLayoutData(gd);

		createDetailsSection(container);
		createStackSection(container);
		createSessionSection(container);

		updateProperties();
		Dialog.applyDialogFont(container);
		return container;
	}

	private void createToolbarButtonBar(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		layout.numColumns = 1;
		comp.setLayout(layout);
		comp.setLayoutData(new GridData(GridData.FILL_VERTICAL));

		Composite container = new Composite(comp, SWT.NONE);
		layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 10;
		layout.numColumns = 1;
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		backButton = createButton(container, IDialogConstants.BACK_ID, "", false);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		gd.verticalSpan = 1;
		backButton.setLayoutData(gd);
		backButton.setImage(elementNum == 0 ? imgPrevDisabled : imgPrevEnabled);

		nextButton = createButton(container, IDialogConstants.NEXT_ID, "", false);
		gd = new GridData();
		gd.horizontalSpan = 3;
		gd.verticalSpan = 1;
		nextButton.setLayoutData(gd);
		nextButton.setImage(elementNum == totalElementCount - 1 ? imgNextDisabled : imgNextEnabled);

		copyButton = createButton(container, COPY_ID, "", false);
		gd = new GridData();
		gd.horizontalSpan = 3;
		gd.verticalSpan = 1;
		copyButton.setLayoutData(gd);
		copyButton.setImage(imgCopyEnabled);

	}

	protected void createButtonsForButtonBar(Composite parent) {
		// create OK button only by default
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
	}

	private void createDetailsSection(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		createTextSection(container);
		createToolbarButtonBar(container);
	}

	private void createTextSection(Composite parent) {
		Composite textContainer = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.marginHeight = layout.marginWidth = 0;
		textContainer.setLayout(layout);
		textContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Label label = new Label(textContainer, SWT.NONE);
		label.setText(PDERuntimePlugin.getResourceString("EventDetailsDialog.date")); //$NON-NLS-1$
		dateLabel = new Label(textContainer, SWT.NULL);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		dateLabel.setLayoutData(gd);

		label = new Label(textContainer, SWT.NONE);
		label.setText(PDERuntimePlugin.getResourceString("EventDetailsDialog.severity")); //$NON-NLS-1$
		severityImageLabel = new Label(textContainer, SWT.NULL);
		severityLabel = new Label(textContainer, SWT.NULL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		severityLabel.setLayoutData(gd);

		label = new Label(textContainer, SWT.NONE);
		label.setText(PDERuntimePlugin.getResourceString("EventDetailsDialog.message")); //$NON-NLS-1$
		gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		label.setLayoutData(gd);
		msgText = new Text(textContainer, SWT.MULTI | SWT.V_SCROLL | SWT.WRAP | SWT.BORDER | FormWidgetFactory.BORDER_STYLE);
		msgText.setEditable(false);
		gd = new GridData(GridData.FILL_BOTH | GridData.VERTICAL_ALIGN_BEGINNING | GridData.GRAB_VERTICAL);
		gd.horizontalSpan = 2;
		gd.verticalSpan = 8;
		gd.grabExcessVerticalSpace = true;
		msgText.setLayoutData(gd);
	}

	private void createStackSection(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout());
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		String stack = entry.getStack();

		Label label = new Label(container, SWT.NULL);
		label.setText(PDERuntimePlugin.getResourceString("EventDetailsDialog.exception")); //$NON-NLS-1$
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		label.setLayoutData(gd);

		stackTraceText = new Text(container, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		gd = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL);
		gd.grabExcessHorizontalSpace = true;
		stackTraceText.setLayoutData(gd);
		stackTraceText.setEditable(false);
	}

	private void createSessionSection(Composite parent) {
		LogSession session = entry.getSession();
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout());
		container.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Label label = new Label(container, SWT.NONE);
		label.setText(PDERuntimePlugin.getResourceString("EventDetailsDialog.session")); //$NON-NLS-1$
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		label.setLayoutData(gd);
		sessionDataText = new Text(container, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL );
		gd = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL);
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalSpan = 3;
		gd.widthHint = 300;
		gd.heightHint = 65;
		sessionDataText.setLayoutData(gd);
		sessionDataText.setEditable(false);
	}

}
