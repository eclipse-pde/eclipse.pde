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
import org.eclipse.swt.custom.*;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;

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
	private Image imgNextEnabled;
	private Image imgPrevEnabled;
	private Image imgCopyEnabled;
	private SashForm sashForm;
	
	// sorting
	private static int ASCENDING = 1;
	private Comparator comparator = null;
	private Collator collator;
	
	// location configuration
	private IDialogSettings dialogSettings;
	private Point dialogLocation;
	private Point dialogSize;
	private int[] sashWeights;
	
	// externalize strings
	private String EVENT_NO_STACK = "EventDetailsDialog.noStack";
	private String EVENT_PREVIOUS = "EventDetailsDialog.previous";
	private String EVENT_NEXT = "EventDetailsDialog.next";
	private String EVENT_COPY = "EventDetailsDialog.copy";

	/**
	 * @param parentShell
	 *            shell in which dialog is displayed
	 */
	protected EventDetailsDialog(Shell parentShell, IAdaptable selection, ISelectionProvider provider) {
		super(parentShell);
		labelProvider = new LogViewLabelProvider();
		this.provider = (TableTreeViewer) provider;
		this.entry = (LogEntry)selection;
		setShellStyle(SWT.MODELESS | SWT.MIN | SWT.MAX | SWT.RESIZE | SWT.CLOSE | SWT.BORDER | SWT.TITLE);
		clipboard = new Clipboard(parentShell.getDisplay());
		initialize();
		createImages();
		collator = Collator.getInstance();
		readConfiguration();
	}

	private void initialize() {
		elementNum = getParentElementNum();
		totalElementCount = provider.getTableTree().getTable().getItemCount() - getVisibleChildrenCount();
		parentEntry = (LogEntry) entry.getParent(entry);
		if (isChild(entry)){
			setEntryChildren(parentEntry);
			resetChildIndex();
		}
	}
	
	private void resetChildIndex(){
		for (int i = 0; i<entryChildren.length; i++){
			if (entryChildren[i].getMessage().equals(entry.getMessage())
					&& entryChildren[i].getDate().equals(entry.getDate())){
				childIndex = i;
				break;
			}
		}
	}
	
	private void createImages(){
		imgCopyEnabled =
			PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_COPY).createImage(
				true);
		//imgNextDisabled. = PDERuntimePluginImages.DESC_NEXT_EVENT_DISABLED.createImage(true);
		//imgPrevDisabled = PDERuntimePluginImages.DESC_PREV_EVENT_DISABLED.createImage(true);
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
		if (sashWeights == null){
			int width = getSashForm().getClientArea().width;
			if (width - 100 > 0)
				width -= 100;
			else
				width = width/2;
			sashWeights = new int[]{width, getSashForm().getClientArea().width-width};
		}
		getSashForm().setWeights(sashWeights);
		return super.open();
	}
	
	public boolean close() {
		storeSettings();
		isOpen = false;
		imgCopyEnabled.dispose();
		imgNextEnabled.dispose();
		imgPrevEnabled.dispose();
		return super.close();
	}

	public void create() {
		super.create();
		
		// dialog location 
		if (dialogLocation != null)
			getShell().setLocation(dialogLocation);
		
		// dialog size
		if (dialogSize != null)
			getShell().setSize(dialogSize);
		else
			getShell().setSize(500,550);
		
		
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
		} else if (elementNum + 1 < totalElementCount){
				elementNum += 1;
			entry = (LogEntry) ((TableTreeViewer) provider).getElementAt(elementNum);
		} else { // at end of list but can branch into child elements - bug 58083
			setEntryChildren(entry);
			entry = entryChildren[0];
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
	
	public void resetButtons(){
		backButton.setEnabled(false);
		nextButton.setEnabled(false);
	}
	
	private void setEntrySelectionInTable(){
		ISelection selection = new StructuredSelection(entry);
		provider.setSelection(selection);
	}
	
	public void updateProperties() {	
		if (isChild(entry)){
			parentEntry = (LogEntry) entry.getParent(entry);
			setEntryChildren(parentEntry);
			resetChildIndex();
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
			stackTraceText.setText(PDERuntimePlugin.getResourceString(EVENT_NO_STACK));
		}
		LogSession session = entry.getSession();
		if (session != null && session.getSessionData() != null)
			sessionDataText.setText(session.getSessionData());

		updateButtons();
	}
	
	private void updateButtons(){
		boolean isAtEnd = elementNum == totalElementCount - 1;
		if (isChild(entry)){
			backButton.setEnabled(true);
			boolean isLastChild = childIndex == entryChildren.length-1;
			nextButton.setEnabled(!isLastChild || !isAtEnd || entry.hasChildren());
		} else {
			backButton.setEnabled(elementNum != 0);
			nextButton.setEnabled(!isAtEnd || entry.hasChildren());
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
		itemEntry = getRootEntry(itemEntry);
		
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
	private LogEntry getRootEntry(LogEntry entry){
		if (!isChild(entry))
			return entry;
		return getRootEntry((LogEntry)entry.getParent(entry));
	}
	public SashForm getSashForm(){
		return sashForm;
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
		createSashForm(container);
		createStackSection(getSashForm());
		createSessionSection(getSashForm());

		updateProperties();
		Dialog.applyDialogFont(container);
		return container;
	}

	private void createSashForm(Composite parent){
		sashForm = new SashForm(parent, SWT.VERTICAL);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		sashForm.setLayout(layout);
		sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));
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
		backButton.setToolTipText(PDERuntimePlugin.getResourceString(EVENT_PREVIOUS));
		backButton.setImage(imgPrevEnabled);
		
		nextButton = createButton(container, IDialogConstants.NEXT_ID, "", false);
		gd = new GridData();
		gd.horizontalSpan = 3;
		gd.verticalSpan = 1;
		nextButton.setLayoutData(gd);
		nextButton.setToolTipText(PDERuntimePlugin.getResourceString(EVENT_NEXT));
		nextButton.setImage(imgNextEnabled);
		
		copyButton = createButton(container, COPY_ID, "", false);
		gd = new GridData();
		gd.horizontalSpan = 3;
		gd.verticalSpan = 1;
		copyButton.setLayoutData(gd);
		copyButton.setImage(imgCopyEnabled);
		copyButton.setToolTipText(PDERuntimePlugin.getResourceString(EVENT_COPY));
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
		msgText = new Text(textContainer, SWT.MULTI | SWT.V_SCROLL | SWT.WRAP | SWT.BORDER);
		msgText.setEditable(false);
		gd = new GridData(GridData.FILL_BOTH | GridData.VERTICAL_ALIGN_BEGINNING | GridData.GRAB_VERTICAL);
		gd.horizontalSpan = 2;
		gd.verticalSpan = 8;
		gd.grabExcessVerticalSpace = true;
		msgText.setLayoutData(gd);
	}

	private void createStackSection(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 6;
		container.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 100;
		container.setLayoutData(gd);

		Label label = new Label(container, SWT.NULL);
		label.setText(PDERuntimePlugin.getResourceString("EventDetailsDialog.exception")); //$NON-NLS-1$
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		label.setLayoutData(gd);

		stackTraceText = new Text(container, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		gd = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL);
		gd.grabExcessHorizontalSpace = true;
		stackTraceText.setLayoutData(gd);
		stackTraceText.setEditable(false);
	}

	private void createSessionSection(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 6;
		container.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint = 100;
		container.setLayoutData(gd);

		Label line = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.widthHint = 1;
		line.setLayoutData(gd);
		
		Label label = new Label(container, SWT.NONE);
		label.setText(PDERuntimePlugin.getResourceString("EventDetailsDialog.session")); //$NON-NLS-1$
		gd = new GridData(GridData.FILL_HORIZONTAL);
		label.setLayoutData(gd);
		sessionDataText = new Text(container, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL );
		gd = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL);
		gd.grabExcessHorizontalSpace = true;
		sessionDataText.setLayoutData(gd);
		sessionDataText.setEditable(false);
	}
	
	//--------------- configuration handling --------------
	
	/**
	 * Stores the current state in the dialog settings.
	 * @since 2.0
	 */
	private void storeSettings() {
		writeConfiguration();
	}
	/**
	 * Returns the dialog settings object used to share state
	 * between several event detail dialogs.
	 * 
	 * @return the dialog settings to be used
	 */
	private IDialogSettings getDialogSettings() {
		IDialogSettings settings= PDERuntimePlugin.getDefault().getDialogSettings();
		dialogSettings= settings.getSection(getClass().getName());
		if (dialogSettings == null)
			dialogSettings= settings.addNewSection(getClass().getName());
		return dialogSettings;
	}

	/**
	 * Initializes itself from the dialog settings with the same state
	 * as at the previous invocation.
	 */
	private void readConfiguration() {
		IDialogSettings s= getDialogSettings();
		try {
			int x= s.getInt("x"); //$NON-NLS-1$
			int y= s.getInt("y"); //$NON-NLS-1$
			dialogLocation= new Point(x, y);
			
			x = s.getInt("width");
			y = s.getInt("height");
			dialogSize = new Point(x,y);
			
			sashWeights = new int[2];
			sashWeights[0] = s.getInt("sashWidth1");
			sashWeights[1] = s.getInt("sashWidth2");
			
		} catch (NumberFormatException e) {
			dialogLocation= null;
			dialogSize = null;
			sashWeights = null;
		}
	}
	
	private void writeConfiguration(){
		IDialogSettings s = getDialogSettings();
		Point location = getShell().getLocation();
		s.put("x", location.x);
		s.put("y", location.y);
		
		Point size = getShell().getSize();
		s.put("width", size.x);
		s.put("height", size.y);
		
		sashWeights = getSashForm().getWeights();
		s.put("sashWidth1", sashWeights[0]);
		s.put("sashWidth2", sashWeights[1]);
	}
}
