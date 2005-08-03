/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
	private TreeViewer provider;
	private int elementNum, totalElementCount;
	private LogEntry[] entryChildren;
	private int childIndex = 0;
	private boolean isOpen;
	private boolean isLastChild;
	private boolean isAtEndOfLog;
	
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
	private Comparator comparator = null;
	private Collator collator;
	
	// location configuration
	private Point dialogLocation;
	private Point dialogSize;
	private int[] sashWeights;
	
	/**
	 * 
	 * @param parentShell shell in which dialog is displayed
	 * @param selection entry initially selected and to be displayed
	 * @param provider viewer
	 * @param comparator comparator used to order all entries
	 */
	protected EventDetailsDialog(Shell parentShell, IAdaptable selection, ISelectionProvider provider, Comparator comparator) {
		super(parentShell);
		labelProvider = new LogViewLabelProvider();
		this.provider = (TreeViewer) provider;
		this.entry = (LogEntry)selection;
		this.comparator = comparator;
		setShellStyle(SWT.MODELESS | SWT.MIN | SWT.MAX | SWT.RESIZE | SWT.CLOSE | SWT.BORDER | SWT.TITLE);
		clipboard = new Clipboard(parentShell.getDisplay());
		initialize();
		createImages();
		collator = Collator.getInstance();
		readConfiguration();
		isLastChild = false;
		isAtEndOfLog = false;
	}

	private void initialize() {
		elementNum = getParentElementNum();
		resetTotalElementCount();
		parentEntry = (LogEntry) entry.getParent(entry);
		if (isChild(entry)){
			setEntryChildren(parentEntry);
			resetChildIndex();
		}
		isLastChild = false;
		isAtEndOfLog = false;
	}
	
	private void resetChildIndex() {
		for (int i = 0; i < entryChildren.length; i++) {
			if (equal(entryChildren[i].getMessage(), entry.getMessage())
					&& equal(entryChildren[i].getDate(), entry.getDate())
					&& equal(entryChildren[i].getPluginId(), entry
							.getPluginId())
					&& entryChildren[i].getSeverity() == entry.getSeverity()
					&& equal(entryChildren[i].getSeverityText(), entry
							.getSeverityText())) {
				childIndex = i;
				break;
			}
		}
	}
	
	private boolean equal(String str1, String str2) {
		if (str1 == null) {
			return str1 == str2;
		}
		return str1.equals(str2);
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
				if (isLastChild){
					setEntryChildren(parentEntry);
					isLastChild = false;
				}
				childIndex--;
				entry = entryChildren[childIndex];
			} else
				entry = parentEntry;
		} else {
			if (elementNum - 1 >= 0)
				elementNum -= 1;
			entry = entryChildren[elementNum];
		}
		setEntrySelectionInTable();
	}

	protected void nextPressed() {
		if (isChild(entry) && childIndex < entryChildren.length-1) {
			childIndex++;
			entry = entryChildren[childIndex];
			isLastChild = childIndex == entryChildren.length - 1;
		} else if (isChild(entry) && isLastChild && !isAtEndOfLog){
			findNextSelectedChild(entry);
		} else if (elementNum + 1 < totalElementCount){
			if (isLastChild){
				setEntryChildren();
				isLastChild = false;
			}
			elementNum += 1;
			entry = entryChildren[elementNum];
		} else { // at end of list but can branch into child elements - bug 58083
			setEntryChildren(entry);
			entry = entryChildren[0];
			isAtEndOfLog = entryChildren.length == 0;
			isLastChild = entryChildren.length == 0;
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

	public void setComparator(Comparator comparator){
		this.comparator = comparator;
		updateProperties();
	}
	private void setComparator(byte sortType, final int sortOrder){
		if (sortType == LogView.DATE){
			comparator = new Comparator(){
				public int compare(Object e1, Object e2) {
					  try {
	                    	SimpleDateFormat formatter = new SimpleDateFormat(
	                    	"yyyy-MM-dd HH:mm:ss.SSS"); //$NON-NLS-1$
	                        Date date1 = formatter.parse(((LogEntry) e1).getDate());
	                        Date date2 = formatter.parse(((LogEntry) e2).getDate());
	                        if (sortOrder == LogView.ASCENDING)
	                            return date1.before(date2) ? -1 : 1;
	                        return date1.after(date2) ? -1 : 1;
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
		if (entry.equals(selectedEntry) &&
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
			if (childIndex == entryChildren.length - 1)
				isLastChild = true;
		}

		resetTotalElementCount();
		
		dateLabel.setText(entry.getDate() != null ? entry.getDate() : ""); //$NON-NLS-1$
		severityImageLabel.setImage(labelProvider.getColumnImage(entry, 0));
		severityLabel.setText(entry.getSeverityText());
		msgText.setText(entry.getMessage() != null ? entry.getMessage() : ""); //$NON-NLS-1$
		String stack = entry.getStack();
		if (stack != null) {
			stackTraceText.setText(stack);
		} else {
			stackTraceText.setText(PDERuntimeMessages.EventDetailsDialog_noStack);
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
			nextButton.setEnabled(nextChildExists(entry, parentEntry, entryChildren) ||  !isLastChild || !isAtEnd || entry.hasChildren());
		} else {
			backButton.setEnabled(elementNum != 0);
			nextButton.setEnabled(!isAtEnd || entry.hasChildren());
		}
	}
	
	private void findNextSelectedChild(LogEntry originalEntry){
		if (isChild (parentEntry)){
			// we're at the end of the child list; find next parent
			// to select.  If the parent is a child at the end of the child
			// list, find its next parent entry to select, etc.
			
			entry = parentEntry;
			setEntryChildren((LogEntry)parentEntry.getParent(parentEntry));
			parentEntry = (LogEntry)parentEntry.getParent(parentEntry);
			resetChildIndex();
			isLastChild = childIndex == entryChildren.length-1;
			if (isLastChild){
				findNextSelectedChild(originalEntry);
			} else {
				nextPressed();
			}
		} else {
			entry = originalEntry;
			isAtEndOfLog = true;
			nextPressed();
		}
	}
	
	private boolean nextChildExists(LogEntry originalEntry, LogEntry originalParent, LogEntry[] originalEntries){
		if (isChild (parentEntry)){
			// we're at the end of the child list; find next parent
			// to select.  If the parent is a child at the end of the child
			// list, find its next parent entry to select, etc.
			
			entry = parentEntry;
			setEntryChildren((LogEntry)parentEntry.getParent(parentEntry));
			parentEntry = (LogEntry)parentEntry.getParent(parentEntry);
			resetChildIndex();
			if (childIndex == entryChildren.length-1){
				nextChildExists(originalEntry, originalParent, originalEntries);
			} else {
				entry = originalEntry;
				parentEntry = originalParent;
				entryChildren = originalEntries;
				resetChildIndex();
				return true;
			}
		} 
		entry = originalEntry;
		parentEntry = originalParent;
		entryChildren = originalEntries;
		resetChildIndex();
		return false;
		
	}
	private void setEntryChildren(){
		Object[] children = ((LogViewContentProvider)provider.getContentProvider()).getElements(null);

		if (comparator != null)
			Arrays.sort(children, comparator);
		entryChildren = new LogEntry[children.length];
		
		System.arraycopy(children,0,entryChildren,0,children.length);
	}
	
	private void resetTotalElementCount(){
		totalElementCount = provider.getTree().getItemCount();
	}
	
	private void setEntryChildren(LogEntry parent){
		if (parent == null){
			setEntryChildren();
			return;
		}
		Object[] children = parent.getChildren(parent);
		if (comparator != null)
			Arrays.sort(children, comparator);
		entryChildren = new LogEntry[children.length];
		
		System.arraycopy(children,0,entryChildren,0,children.length);
	}

	private int getParentElementNum(){
		LogEntry itemEntry = (LogEntry)((IStructuredSelection)provider.getSelection()).getFirstElement();
		itemEntry = getRootEntry(itemEntry);
		
		setEntryChildren();
		for (int i = 0; i<provider.getTree().getItemCount(); i++){
			try {
				LogEntry littleEntry = entryChildren[i];
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

		backButton = createButton(container, IDialogConstants.BACK_ID, "", false); //$NON-NLS-1$
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		gd.verticalSpan = 1;
		backButton.setLayoutData(gd);
		backButton.setToolTipText(PDERuntimeMessages.EventDetailsDialog_previous);
		backButton.setImage(imgPrevEnabled);
		
		nextButton = createButton(container, IDialogConstants.NEXT_ID, "", false); //$NON-NLS-1$
		gd = new GridData();
		gd.horizontalSpan = 3;
		gd.verticalSpan = 1;
		nextButton.setLayoutData(gd);
		nextButton.setToolTipText(PDERuntimeMessages.EventDetailsDialog_next);
		nextButton.setImage(imgNextEnabled);
		
		copyButton = createButton(container, COPY_ID, "", false); //$NON-NLS-1$
		gd = new GridData();
		gd.horizontalSpan = 3;
		gd.verticalSpan = 1;
		copyButton.setLayoutData(gd);
		copyButton.setImage(imgCopyEnabled);
		copyButton.setToolTipText(PDERuntimeMessages.EventDetailsDialog_copy);
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
		label.setText(PDERuntimeMessages.EventDetailsDialog_date); 
		dateLabel = new Label(textContainer, SWT.NULL);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		dateLabel.setLayoutData(gd);

		label = new Label(textContainer, SWT.NONE);
		label.setText(PDERuntimeMessages.EventDetailsDialog_severity); 
		severityImageLabel = new Label(textContainer, SWT.NULL);
		severityLabel = new Label(textContainer, SWT.NULL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		severityLabel.setLayoutData(gd);

		label = new Label(textContainer, SWT.NONE);
		label.setText(PDERuntimeMessages.EventDetailsDialog_message); 
		gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		label.setLayoutData(gd);
		msgText = new Text(textContainer, SWT.MULTI | SWT.V_SCROLL | SWT.WRAP | SWT.BORDER);
		msgText.setEditable(false);
		gd = new GridData(GridData.FILL_BOTH | GridData.VERTICAL_ALIGN_BEGINNING | GridData.GRAB_VERTICAL);
		gd.horizontalSpan = 2;
		gd.heightHint = 44;
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
		label.setText(PDERuntimeMessages.EventDetailsDialog_exception); 
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
		label.setText(PDERuntimeMessages.EventDetailsDialog_session); 
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
		IDialogSettings dialogSettings = settings.getSection(getClass().getName());
		if (dialogSettings == null)
			dialogSettings = settings.addNewSection(getClass().getName());
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
			
			x = s.getInt("width"); //$NON-NLS-1$
			y = s.getInt("height"); //$NON-NLS-1$
			dialogSize = new Point(x,y);
			
			sashWeights = new int[2];
			sashWeights[0] = s.getInt("sashWidth1"); //$NON-NLS-1$
			sashWeights[1] = s.getInt("sashWidth2"); //$NON-NLS-1$
			
		} catch (NumberFormatException e) {
			dialogLocation= null;
			dialogSize = null;
			sashWeights = null;
		}
	}
	
	private void writeConfiguration(){
		IDialogSettings s = getDialogSettings();
		Point location = getShell().getLocation();
		s.put("x", location.x); //$NON-NLS-1$
		s.put("y", location.y); //$NON-NLS-1$
		
		Point size = getShell().getSize();
		s.put("width", size.x); //$NON-NLS-1$
		s.put("height", size.y); //$NON-NLS-1$
		
		sashWeights = getSashForm().getWeights();
		s.put("sashWidth1", sashWeights[0]); //$NON-NLS-1$
		s.put("sashWidth2", sashWeights[1]); //$NON-NLS-1$
	}
}
