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
package org.eclipse.pde.internal.ui.parts;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.widgets.*;
/**
 * The helper class for creating entry fields with label and text. Optionally,
 * a button can be added after the text. The attached listener reacts to all
 * the events. Entring new text makes the entry 'dirty', but only when 'commit'
 * is called is 'valueChanged' method called (and only if 'dirty' flag is set).
 * This allows delayed commit.
 */
public class FormEntry {
	private Control label;
	private Text text;
	private Button browse;
	private String value=""; //$NON-NLS-1$
	private boolean dirty;
	boolean ignoreModify = false;
	private IFormEntryListener listener;
	/**
	 * The default constructor. Call 'createControl' to make it.
	 *  
	 */
	public FormEntry(Composite parent, FormToolkit toolkit, String labelText, int style) {
		createControl(parent, toolkit, labelText, style, null, false, 0, 0);
	}
	
	/**
	 * This constructor create all the controls right away.
	 * 
	 * @param parent
	 * @param toolkit
	 * @param labelText
	 * @param browseText
	 * @param linkLabel
	 */
	public FormEntry(Composite parent, FormToolkit toolkit, String labelText,
			String browseText, boolean linkLabel) {
		this(parent, toolkit, labelText, browseText, linkLabel, 0);
	}
	
	public FormEntry(Composite parent, FormToolkit toolkit, String labelText,
			String browseText, boolean linkLabel, int indent) {
		createControl(parent, toolkit, labelText, SWT.SINGLE, browseText, linkLabel, indent, 0);
	}
	
	public FormEntry(Composite parent, FormToolkit toolkit, String labelText,
			int indent, int tcolspan) {
		createControl(parent, toolkit, labelText, SWT.SINGLE, null, false, indent, tcolspan);
	}
	
	/**
	 * Create all the controls in the provided parent.
	 * 
	 * @param parent
	 * @param toolkit
	 * @param labelText
	 * @param span
	 * @param browseText
	 * @param linkLabel
	 */
	private void createControl(Composite parent, FormToolkit toolkit,
			String labelText, int style, String browseText, boolean linkLabel, int indent, int tcolspan) {
		if (linkLabel) {
			Hyperlink link = toolkit.createHyperlink(parent, labelText,
					SWT.NULL);
			label = link;
		} else {
			label = toolkit.createLabel(parent, labelText);
			label.setForeground(toolkit.getColors().getColor(FormColors.TITLE));
		}
		text = toolkit.createText(parent, "", style); //$NON-NLS-1$
		addListeners();
		if (browseText != null) {
			browse = toolkit.createButton(parent, browseText, SWT.PUSH);
			browse.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					if (listener != null)
						listener.browseButtonSelected(FormEntry.this);
				}
			});
		}
		fillIntoGrid(parent, indent, tcolspan);
	}
	public void setEditable(boolean editable) {
		text.setEditable(editable);
		if (browse!=null) 
			browse.setEnabled(editable);
	}
	private void fillIntoGrid(Composite parent, int indent, int tcolspan) {
		Layout layout = parent.getLayout();
		int tspan;
		if (layout instanceof GridLayout) {
			int span = ((GridLayout) layout).numColumns;
			if (tcolspan > 0)
				tspan = tcolspan;
			else
				tspan = browse != null ? span - 2 : span - 1;
			GridData gd;
			gd = new GridData(GridData.VERTICAL_ALIGN_CENTER);
			gd.horizontalIndent = indent;
			label.setLayoutData(gd);
			gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
			gd.horizontalSpan = tspan;
			gd.grabExcessHorizontalSpace = (tspan == 1);
			gd.widthHint = 10;
			text.setLayoutData(gd);
			if (browse != null) {
				gd = new GridData(GridData.VERTICAL_ALIGN_CENTER);
				browse.setLayoutData(gd);
			}
		} else if (layout instanceof TableWrapLayout) {
			int span = ((TableWrapLayout) layout).numColumns;
			if (tcolspan > 0)
				tspan = tcolspan;
			else
				tspan = browse != null ? span - 2 : span - 1;
			TableWrapData td;
			td = new TableWrapData();
			td.valign = TableWrapData.MIDDLE;
			td.indent = indent;
			label.setLayoutData(td);
			td = new TableWrapData(TableWrapData.FILL);
			td.colspan = tspan;
			td.grabHorizontal = (tspan == 1);
			td.valign = TableWrapData.MIDDLE;
			text.setLayoutData(td);
			if (browse != null) {
				td = new TableWrapData(TableWrapData.FILL);
				td.valign = TableWrapData.MIDDLE;
				browse.setLayoutData(td);
			}
		}
	}
	/**
	 * Attaches the listener for the entry.
	 * 
	 * @param listener
	 */
	public void setFormEntryListener(IFormEntryListener listener) {
		if (label instanceof Hyperlink) {
			if (this.listener!=null)
				((Hyperlink)label).removeHyperlinkListener(this.listener);
			if (listener!=null)
				((Hyperlink)label).addHyperlinkListener(listener);
		}
		this.listener = listener;
	}
	private void addListeners() {
		text.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				keyReleaseOccured(e);
			}
		});
		text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				editOccured(e);
			}
		});
		text.addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				if (listener != null)
					listener.focusGained(FormEntry.this);
			}
			public void focusLost(FocusEvent e) {
				if (dirty)
					commit();
			}
		});
	}
	/**
	 * If dirty, commits the text in the widget to the value and notifies the
	 * listener. This call clears the 'dirty' flag.
	 *  
	 */
	public void commit() {
		if (dirty) {
			value = text.getText();
			//if (value.length()==0)
				//value = null;
			//notify
			if (listener != null)
				listener.textValueChanged(this);
		}
		dirty = false;
	}
	public void cancelEdit() {
		dirty = false;
	}
	private void editOccured(ModifyEvent e) {
		if (ignoreModify)
			return;
		dirty = true;
		if (listener != null)
			listener.textDirty(this);
	}
	/**
	 * Returns the text control.
	 * 
	 * @return
	 */
	public Text getText() {
		return text;
	}
	
	/**
	 * Returns the browse button control.
	 * @return
	 */
	public Button getButton() {
		return browse;
	}
	/**
	 * Returns the current entry value. If the entry is dirty and was not
	 * commited, the value may be different from the text in the widget.
	 * 
	 * @return
	 */
	public String getValue() {
		return value.trim();
	}
	/**
	 * Returns true if the text has been modified.
	 * 
	 * @return
	 */
	public boolean isDirty() {
		return dirty;
	}
	private void keyReleaseOccured(KeyEvent e) {
		if (e.character == '\r') {
			// commit value
			if (dirty)
				commit();
		} else if (e.character == '\u001b') { // Escape character
			text.setText(value != null ? value : ""); // restore old //$NON-NLS-1$
			dirty = false;
		}	
		listener.selectionChanged(FormEntry.this);
	}
	/**
	 * Sets the value of this entry.
	 * 
	 * @param value
	 */
	public void setValue(String value) {
		if (text != null)
			text.setText(value != null ? value : ""); //$NON-NLS-1$
		this.value = (value != null) ? value : ""; //$NON-NLS-1$
	}
	/**
	 * Sets the value of this entry with the possibility to turn the
	 * notification off.
	 * 
	 * @param value
	 * @param blockNotification
	 */
	public void setValue(String value, boolean blockNotification) {
		ignoreModify = blockNotification;
		setValue(value);
		ignoreModify = false;
	}
}
