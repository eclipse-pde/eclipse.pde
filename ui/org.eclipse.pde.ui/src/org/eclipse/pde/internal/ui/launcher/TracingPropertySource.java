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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;

public class TracingPropertySource {
	private final IPluginModelBase fModel;
	private final Map<String, String> fTemplate;
	private final Map<String, Object> fValues = new HashMap<>();
	private final Map<String, String> fMasterOptions;
	private boolean fModified;

	// the flag fChanged is used to determine whether the model's content page
	// should be recreated
	private boolean fChanged;
	private final TracingBlock fBlock;

	private abstract class PropertyEditor {
		private final String key;
		private final String label;
		private final String comment;

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
		protected void createCommentDecorator(Control target) {
			String commentText = getFormattedComment();
			if (!commentText.isEmpty()) {
				target.setToolTipText(commentText);
			}
		}

		private static final Pattern NEW_LINE = Pattern.compile("\\r?\\n"); //$NON-NLS-1$

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
			if (commentOrig == null || commentOrig.isEmpty()) {
				return ""; //$NON-NLS-1$
			}
			String[] lines = NEW_LINE.split(commentOrig);
			StringBuilder commentBuilder = new StringBuilder();
			boolean needsSpace = false;
			for (String string : lines) {
				// remove leading hash and trim spaces around
				string = (string.startsWith("#") ? string.substring(1) : string).trim(); //$NON-NLS-1$
				if (string.isEmpty()) {
					commentBuilder.append("\n\n"); //$NON-NLS-1$
					needsSpace = false;
				} else {
					if (needsSpace) {
						commentBuilder.append(" "); //$NON-NLS-1$
					}
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
			createCommentDecorator(checkbox);
		}

		@Override
		public void initialize() {
			boolean value = (Boolean) fValues.get(getKey());
			checkbox.setSelection(value);
			checkbox.addSelectionListener(widgetSelectedAdapter(e -> valueModified(checkbox.getSelection())));
			valueModified(value);
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
			createCommentDecorator(label);
		}

		@Override
		public void initialize() {
			String value = (String) fValues.get(getKey());
			text.setText(value);
			text.addModifyListener(e -> valueModified(text.getText()));
			valueModified(value);
		}
	}

	public TracingPropertySource(IPluginModelBase model, Map<String, String> masterOptions,
			Map<String, String> template, TracingBlock block) {
		fModel = model;
		fMasterOptions = masterOptions;
		fTemplate = template;
		fBlock = block;
	}

	public IPluginModelBase getModel() {
		return fModel;
	}

	public void createContents(Composite parent, boolean enabled) {
		TableWrapLayout layout = new TableWrapLayout();
		layout.numColumns = 2;
		layout.rightMargin = 10;
		layout.leftMargin = 10;
		parent.setLayout(layout);
		boolean bordersNeeded = false;
		Iterable<String> sortedKeys = fTemplate.keySet().stream().sorted()::iterator;
		for (String key : sortedKeys) {
			String shortKey = IPath.fromOSString(key).removeFirstSegments(1).toString();
			String value = fTemplate.get(key);
			String masterValue = fMasterOptions.get(key);
			String commentValue = fMasterOptions.get("#" + key); //$NON-NLS-1$
			PropertyEditor editor;
			if (value != null && (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false"))) { //$NON-NLS-1$ //$NON-NLS-2$
				editor = new BooleanEditor(shortKey, shortKey, commentValue);
				if (masterValue != null) {
					fValues.put(shortKey, Boolean.valueOf(masterValue));
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
			if (bordersNeeded) {
				fBlock.getToolkit().paintBordersFor(parent);
			}
		}
	}

	public void save() {
		String pid = fModel.getPluginBase().getId();
		fValues.forEach((key, value) -> {
			IPath path = IPath.fromOSString(pid).append(key);
			fMasterOptions.put(path.toString(), value.toString());
		});
		fModified = false;
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
