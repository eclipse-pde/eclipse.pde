/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.util.*;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;

public class TracingPropertySource {
	private IPluginModelBase fModel;
	private Vector fDescriptors;
	private Hashtable fTemplate;
	private Hashtable fValues;
	private Hashtable fDvalues;
	private static final String[] fBooleanChoices = {"false", "true"}; //$NON-NLS-1$ //$NON-NLS-2$
	private Properties fMasterOptions;
	private boolean fModified;
	private TracingBlock fBlock;

	private abstract class PropertyEditor {
		private String key;
		private String label;

		public PropertyEditor(String key, String label) {
			this.key = key;
			this.label = label;
		}

		public String getKey() {
			return key;
		}

		public String getLabel() {
			return label;
		}

		abstract void create(Composite parent, boolean enabled);

		abstract void initialize();

		protected void valueModified(Object value) {
			fValues.put(getKey(), value);
			fModified = true;
			fBlock.getTab().scheduleUpdateJob();
		}
	}

	private class BooleanEditor extends PropertyEditor {
		private Button checkbox;

		public BooleanEditor(String key, String label) {
			super(key, label);
		}

		public void create(Composite parent, boolean enabled) {
			checkbox = fBlock.getToolkit().createButton(parent, getLabel(), SWT.CHECK);
			TableWrapData td = new TableWrapData();
			td.colspan = 2;
			checkbox.setLayoutData(td);
			checkbox.setEnabled(enabled);
		}

		public void update() {
			Integer value = (Integer) fValues.get(getKey());
			checkbox.setSelection(value.intValue() == 1);
		}

		public void initialize() {
			update();
			checkbox.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					int value = checkbox.getSelection() ? 1 : 0;
					valueModified(new Integer(value));
				}
			});
		}
	}

	private class TextEditor extends PropertyEditor {
		private Text text;

		public TextEditor(String key, String label) {
			super(key, label);
		}

		public void create(Composite parent, boolean enabled) {
			Label label = fBlock.getToolkit().createLabel(parent, getLabel());
			label.setEnabled(enabled);
			TableWrapData td = new TableWrapData();
			td.valign = TableWrapData.MIDDLE;
			label.setLayoutData(td);
			text = fBlock.getToolkit().createText(parent, ""); //$NON-NLS-1$
			td = new TableWrapData(TableWrapData.FILL_GRAB);
			//gd.widthHint = 100;
			text.setLayoutData(td);
			text.setEnabled(enabled);
		}

		public void update() {
			String value = (String) fValues.get(getKey());
			text.setText(value);
		}

		public void initialize() {
			update();
			text.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					valueModified(text.getText());
				}
			});
		}
	}

	public TracingPropertySource(IPluginModelBase model, Properties masterOptions, Hashtable template, TracingBlock block) {
		fModel = model;
		fMasterOptions = masterOptions;
		fTemplate = template;
		fBlock = block;
		fValues = new Hashtable();
		fDvalues = new Hashtable();
	}

	public IPluginModelBase getModel() {
		return fModel;
	}

	private Object[] getSortedKeys(int size) {
		Object[] keyArray = new Object[size];
		int i = 0;
		for (Enumeration keys = fTemplate.keys(); keys.hasMoreElements();) {
			String key = (String) keys.nextElement();
			keyArray[i++] = key;
		}
		Arrays.sort(keyArray, new Comparator() {
			public int compare(Object o1, Object o2) {
				return compareKeys(o1, o2);
			}
		});
		return keyArray;
	}

	private int compareKeys(Object o1, Object o2) {
		String s1 = (String) o1;
		String s2 = (String) o2;
		// equal
		return s1.compareTo(s2);
	}

	public void createContents(Composite parent, boolean enabled) {
		fDescriptors = new Vector();
		TableWrapLayout layout = new TableWrapLayout();
		layout.numColumns = 2;
		parent.setLayout(layout);
		boolean bordersNeeded = false;
		Object[] sortedKeys = getSortedKeys(fTemplate.size());
		for (int i = 0; i < sortedKeys.length; i++) {
			String key = (String) sortedKeys[i];
			IPath path = new Path(key);
			path = path.removeFirstSegments(1);
			String shortKey = path.toString();
			String value = (String) fTemplate.get(key);
			String lvalue = null;
			String masterValue = fMasterOptions.getProperty(key);
			PropertyEditor editor;
			if (value != null)
				lvalue = value.toLowerCase(Locale.ENGLISH);
			if (lvalue != null && (lvalue.equals("true") || lvalue.equals("false"))) { //$NON-NLS-1$ //$NON-NLS-2$
				editor = new BooleanEditor(shortKey, shortKey);
				Integer dvalue = new Integer(lvalue.equals("true") ? 1 : 0); //$NON-NLS-1$
				fDvalues.put(shortKey, dvalue);
				if (masterValue != null) {
					Integer mvalue = new Integer(masterValue.equals("true") //$NON-NLS-1$
					? 1
							: 0);
					fValues.put(shortKey, mvalue);
				}
			} else {
				editor = new TextEditor(shortKey, shortKey);
				fDvalues.put(shortKey, value != null ? value : ""); //$NON-NLS-1$
				if (masterValue != null) {
					fValues.put(shortKey, masterValue);
				}
				bordersNeeded = true;
			}
			editor.create(parent, enabled);
			editor.initialize();
			fDescriptors.add(editor);
			if (bordersNeeded)
				fBlock.getToolkit().paintBordersFor(parent);
		}
	}

	/**
	 */
	public void save() {
		String pid = fModel.getPluginBase().getId();
		for (Enumeration keys = fValues.keys(); keys.hasMoreElements();) {
			String shortKey = (String) keys.nextElement();
			Object value = fValues.get(shortKey);
			String svalue = value.toString();
			if (value instanceof Integer)
				svalue = fBooleanChoices[((Integer) value).intValue()];
			IPath path = new Path(pid).append(shortKey);
			fMasterOptions.setProperty(path.toString(), svalue);
		}
		fModified = false;
	}

	public void dispose() {
	}

	public boolean isModified() {
		return fModified;
	}
}
