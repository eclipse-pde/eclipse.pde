/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.newparts;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
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
	private String value;
	private boolean dirty;
	boolean ignoreModify = false;
	private IFormEntryListener listener;
	/**
	 * The default constructor. Call 'createControl' to make it.
	 *  
	 */
	public FormEntry() {
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
		createControl(parent, toolkit, labelText, browseText, linkLabel);
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
	public void createControl(Composite parent, FormToolkit toolkit,
			String labelText, String browseText, boolean linkLabel) {
		if (linkLabel) {
			Hyperlink link = toolkit.createHyperlink(parent, labelText,
					SWT.NULL);
			label = link;
		} else {
			label = toolkit.createLabel(parent, labelText);
		}
		text = toolkit.createText(parent, "", SWT.SINGLE);
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
		fillIntoGrid(parent);
	}
	private void fillIntoGrid(Composite parent) {
		Layout layout = parent.getLayout();
		if (layout instanceof GridLayout) {
			GridData gd;
			int span = ((GridLayout) layout).numColumns;
			gd = new GridData(GridData.VERTICAL_ALIGN_CENTER);
			label.setLayoutData(gd);
			int tspan = browse != null ? span - 2 : span - 1;
			gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
			gd.horizontalSpan = tspan;
			gd.grabExcessHorizontalSpace=(tspan==1);
			text.setLayoutData(gd);
			if (browse != null) {
				gd = new GridData(GridData.VERTICAL_ALIGN_CENTER);
				browse.setLayoutData(gd);
			}
		} else if (layout instanceof TableWrapLayout) {
			TableWrapData td;
			int span = ((TableWrapLayout) layout).numColumns;
			td = new TableWrapData();
			td.valign = TableWrapData.MIDDLE;
			label.setLayoutData(td);
			int tspan = browse != null ? span - 2 : span - 1;
			td = new TableWrapData(TableWrapData.FILL);
			td.colspan = tspan;
			td.grabHorizontal = (tspan==1);
			text.setLayoutData(td);
			if (browse != null) {
				td = new TableWrapData();
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
			//notify
			if (listener != null)
				listener.textValueChanged(this);
		}
		dirty = false;
	}
	protected void editOccured(ModifyEvent e) {
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
	 * Returns the current entry value. If the entry is dirty and was not
	 * commited, the value may be different from the text in the widget.
	 * 
	 * @return
	 */
	public String getValue() {
		return value;
	}
	/**
	 * Returns true if the text has been modified.
	 * 
	 * @return
	 */
	public boolean isDirty() {
		return dirty;
	}
	protected void keyReleaseOccured(KeyEvent e) {
		if (e.character == '\r') {
			// commit value
			if (dirty)
				commit();
		} else if (e.character == '\u001b') { // Escape character
			text.setText(value != null ? value : ""); // restore old
			dirty = false;
		}
	}
	/**
	 * Sets the value of this entry.
	 * 
	 * @param value
	 */
	public void setValue(String value) {
		if (text != null)
			text.setText(value != null ? value : "");
		this.value = value;
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
