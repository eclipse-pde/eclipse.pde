/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Alena Laskavaia - Bug 453392 - No debug options help
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.util.*;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;

public class TracingPropertySource {
	private IPluginModelBase fModel;
	private Vector<PropertyEditor> fDescriptors;
	private Hashtable<?, ?> fTemplate;
	private Hashtable<String, Object> fValues;
	private static final String[] fBooleanChoices = {"false", "true"}; //$NON-NLS-1$ //$NON-NLS-2$
	private Properties fMasterOptions;
	private boolean fModified;
	
	// the flag fChanged is used to determine whether the model's content page
	// should be recreated
	private boolean fChanged;
	private TracingBlock fBlock;

	private abstract class PropertyEditor {
		private String key;
		private String label;
		private String comment;

		public PropertyEditor(String key, String label, String comment) {
			this.key = key;
			this.label = label;
			this.comment = comment;
		}

		public String getKey() {
			return key;
		}

		public String getLabel() {
			return label;
		}

		public String getComment() {
			return comment;
		}

		abstract void create(Composite parent, boolean enabled);

		abstract void initialize();

		protected void valueModified(Object value) {
			fValues.put(getKey(), value);
			fModified = true;
			fChanged = true;
			fBlock.getTab().scheduleUpdateJob();
		}

		/**
		 * Creates a comment decorator for the options, currently it is a
		 * tooltip, but technically it can be label or decorator on control
		 *
		 * @param target
		 *            - the control to be decorated
		 * @param enabled
		 */
		protected void createCommentDecorator(Control target, boolean enabled) {
			String commentText = getFormattedComment();
			if (!commentText.isEmpty()) {
				target.setToolTipText(commentText);
			}
		}

		/**
		 * Takes the comment lines prefixed by # and formats them. If two or
		 * more lines sequentially start with # without empty lines in between
		 * it will be joined. Empty line between comment sections will be
		 * preserved
		 *
		 * @return formatted comment
		 */
		protected String getFormattedComment() {
			String commentOrig = getComment();
			if (commentOrig == null || commentOrig.trim().isEmpty())
				return ""; //$NON-NLS-1$

			String lines[] = commentOrig.trim().split("\\r?\\n"); //$NON-NLS-1$
			StringBuilder commentBuilder = new StringBuilder();
			boolean needsSpace = false;
			for (String string : lines) {
				// remove leading hash and trim spaces around
				string = string.replaceFirst("^#", "").trim(); //$NON-NLS-1$ //$NON-NLS-2$
				if (string.isEmpty()) {
					commentBuilder.append("\n\n"); //$NON-NLS-1$
					needsSpace = false;
				} else {
					if (needsSpace)
						commentBuilder.append(" "); //$NON-NLS-1$
					commentBuilder.append(string);
					needsSpace = true;
				}
			}

			String processed = commentBuilder.toString().trim();
			int k = processed.lastIndexOf("\n\n"); //$NON-NLS-1$
			if (k > 0) { // multi section comment, just keep last section
				// since we trimmed the string k guaranteed to be >0 and <length-2 (unless -1)
				return processed.substring(k + 2);
			}
			return processed;
		}
	}

	private class BooleanEditor extends PropertyEditor {
		private Button checkbox;

		public BooleanEditor(String key, String label, String comment) {
			super(key, label, comment);
		}

		@Override
		public void create(Composite parent, boolean enabled) {
			checkbox = fBlock.getToolkit().createButton(parent, getLabel(), SWT.CHECK);
			TableWrapData td = new TableWrapData();
			td.colspan = 2;
			checkbox.setLayoutData(td);
			checkbox.setEnabled(enabled);
			createCommentDecorator(checkbox, enabled);
		}

		public void update() {
			Integer value = (Integer) fValues.get(getKey());
			checkbox.setSelection(value.intValue() == 1);
		}

		@Override
		public void initialize() {
			update();
			checkbox.addSelectionListener(widgetSelectedAdapter(e -> {
				int value = checkbox.getSelection() ? 1 : 0;
				valueModified(Integer.valueOf(value));
			}));
			int value = checkbox.getSelection() ? 1 : 0;
			valueModified(Integer.valueOf(value));
		}
	}

	private class TextEditor extends PropertyEditor {
		private Text text;

		public TextEditor(String key, String label, String comment) {
			super(key, label, comment);
		}

		@Override
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
			createCommentDecorator(label, enabled);
		}

		public void update() {
			String value = (String) fValues.get(getKey());
			text.setText(value);
		}

		@Override
		public void initialize() {
			update();
			text.addModifyListener(e -> valueModified(text.getText()));
			valueModified(text.getText());
		}
	}

	public TracingPropertySource(IPluginModelBase model, Properties masterOptions, Hashtable<?, ?> template, TracingBlock block) {
		fModel = model;
		fMasterOptions = masterOptions;
		fTemplate = template;
		fBlock = block;
		fValues = new Hashtable<>();
	}

	public IPluginModelBase getModel() {
		return fModel;
	}

	private Object[] getSortedKeys(int size) {
		Object[] keyArray = new Object[size];
		int i = 0;
		for (Enumeration<?> keys = fTemplate.keys(); keys.hasMoreElements();) {
			String key = (String) keys.nextElement();
			keyArray[i++] = key;
		}
		Arrays.sort(keyArray, (o1, o2) -> compareKeys(o1, o2));
		return keyArray;
	}

	private int compareKeys(Object o1, Object o2) {
		String s1 = (String) o1;
		String s2 = (String) o2;
		// equal
		return s1.compareTo(s2);
	}

	public void createContents(Composite parent, boolean enabled) {
		fDescriptors = new Vector<>();
		TableWrapLayout layout = new TableWrapLayout();
		layout.numColumns = 2;
		layout.rightMargin = 10;
		layout.leftMargin = 10;
		parent.setLayout(layout);
		boolean bordersNeeded = false;
		Object[] sortedKeys = getSortedKeys(fTemplate.size());
		for (Object keyObject : sortedKeys) {
			String key = (String) keyObject;
			IPath path = new Path(key);
			path = path.removeFirstSegments(1);
			String shortKey = path.toString();
			String value = (String) fTemplate.get(key);
			String lvalue = null;
			String masterValue = fMasterOptions.getProperty(key);
			String commentValue = fMasterOptions.getProperty("#" + key); //$NON-NLS-1$
			PropertyEditor editor;
			if (value != null)
				lvalue = value.toLowerCase(Locale.ENGLISH);
			if (lvalue != null && (lvalue.equals("true") || lvalue.equals("false"))) { //$NON-NLS-1$ //$NON-NLS-2$
				editor = new BooleanEditor(shortKey, shortKey, commentValue);
				if (masterValue != null) {
					Integer mvalue = Integer.valueOf(masterValue.equals("true") //$NON-NLS-1$
					? 1
							: 0);
					fValues.put(shortKey, mvalue);
				}
			} else {
				editor = new TextEditor(shortKey, shortKey, commentValue);
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
		for (Enumeration<String> keys = fValues.keys(); keys.hasMoreElements();) {
			String shortKey = keys.nextElement();
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

	public boolean isChanged() {
		return fChanged;
	}

	public void setChanged(boolean isChanged) {
		fChanged = isChanged;
	}
}
