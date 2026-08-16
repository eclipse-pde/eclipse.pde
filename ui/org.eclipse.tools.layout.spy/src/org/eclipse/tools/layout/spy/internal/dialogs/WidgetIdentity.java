/*******************************************************************************
 * Copyright (c) 2026 Vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lars Vogel <Lars.Vogel@vogella.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.tools.layout.spy.internal.dialogs;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.Widget;

/**
 * Describes what a widget is, as opposed to where it sits: its text, the e4
 * model element it belongs to, its CSS identity and the items it owns.
 */
final class WidgetIdentity {

	/** Matches the identity hash that {@link Object#toString()} appends. */
	private static final Pattern IDENTITY_HASH = Pattern.compile("([\\w.$]+)@[0-9a-fA-F]+"); //$NON-NLS-1$

	private static final String CSS_CLASS_KEY = "org.eclipse.e4.ui.css.CssClassName"; //$NON-NLS-1$
	private static final String CSS_ID_KEY = "org.eclipse.e4.ui.css.id"; //$NON-NLS-1$
	private static final String OWNING_MODEL_ELEMENT_KEY = "modelElement"; //$NON-NLS-1$

	/** Longest text taken from a widget before it is truncated. */
	private static final int MAX_TEXT = 60;

	private WidgetIdentity() {
	}

	/**
	 * Returns the text of the widget in quotes, or an empty string when it has
	 * none. The content of a {@link Text} is deliberately not read, it can hold a
	 * password or an entire editor buffer.
	 */
	static String text(Widget widget) {
		if (widget instanceof Text) {
			return ""; //$NON-NLS-1$
		}
		// Widget.toString() renders as "Label {the text}" for every widget that has one
		String rendered = widget.toString();
		int start = rendered.indexOf('{');
		if (start < 0) {
			return ""; //$NON-NLS-1$
		}
		// Composite.toString() appends "[layout=...]" and a layout renders braces of its
		// own, so the closing brace has to be matched rather than searched for
		int end = matchingBrace(rendered, start);
		if (end <= start + 1) {
			return ""; //$NON-NLS-1$
		}
		return " \"" + truncate(rendered.substring(start + 1, end)) + "\""; //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static int matchingBrace(String text, int openingBrace) {
		int depth = 0;
		for (int i = openingBrace; i < text.length(); i++) {
			char character = text.charAt(i);
			if (character == '{') {
				depth++;
			} else if (character == '}') {
				depth--;
				if (depth == 0) {
					return i;
				}
			}
		}
		return -1;
	}

	/**
	 * Returns the e4 and CSS identity of the control, or an empty string when it
	 * carries none. Only the control that actually owns the model element is
	 * described, otherwise every descendant would repeat the same part id.
	 */
	static String context(Control control) {
		List<String> parts = new ArrayList<>();
		if (control.getData(OWNING_MODEL_ELEMENT_KEY) instanceof MUIElement element) {
			String id = element.getElementId();
			parts.add("model=" + modelTypeName(element) + (id == null || id.isEmpty() ? "" : "#" + id)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		if (control.getData(CSS_CLASS_KEY) instanceof String cssClass && !cssClass.isBlank()) {
			parts.add("css-class=" + cssClass); //$NON-NLS-1$
		}
		if (control.getData(CSS_ID_KEY) instanceof String cssId && !cssId.isBlank()) {
			parts.add("css-id=" + cssId); //$NON-NLS-1$
		}
		if (parts.isEmpty()) {
			return ""; //$NON-NLS-1$
		}
		return " [" + String.join(", ", parts) + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/**
	 * Appends the items of the widget. Items are not {@link Control}s and are
	 * therefore invisible in a walk over the control hierarchy, although they carry
	 * most of the structure of a tab folder or a tool bar.
	 */
	static void appendItems(StringBuilder builder, Widget widget) {
		Item[] items = itemsOf(widget);
		if (items.length == 0) {
			return;
		}
		builder.append("items = ").append(items.length).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
		Item selected = selectionOf(widget);
		for (int i = 0; i < items.length; i++) {
			Item item = items[i];
			builder.append("  [").append(i).append("] ").append(item.getClass().getSimpleName()); //$NON-NLS-1$ //$NON-NLS-2$
			builder.append(text(item));
			if (item == selected) {
				builder.append(" (selected)"); //$NON-NLS-1$
			}
			if (item instanceof ToolItem toolItem) {
				builder.append(" ").append(toolItem.getBounds()); //$NON-NLS-1$
				builder.append(" style = ").append(SwtStyles.describe(ToolItem.class, toolItem.getStyle())); //$NON-NLS-1$
			} else if (item instanceof CTabItem tabItem) {
				builder.append(" ").append(tabItem.getBounds()); //$NON-NLS-1$
			}
			builder.append("\n"); //$NON-NLS-1$
		}
	}

	private static Item[] itemsOf(Widget widget) {
		if (widget instanceof CTabFolder folder) {
			return folder.getItems();
		}
		if (widget instanceof TabFolder folder) {
			return folder.getItems();
		}
		if (widget instanceof ToolBar toolBar) {
			return toolBar.getItems();
		}
		if (widget instanceof CoolBar coolBar) {
			return coolBar.getItems();
		}
		if (widget instanceof Tree tree) {
			return tree.getColumns();
		}
		if (widget instanceof Table table) {
			return table.getColumns();
		}
		return new Item[0];
	}

	private static @Nullable Item selectionOf(Widget widget) {
		if (widget instanceof CTabFolder folder) {
			return folder.getSelection();
		}
		if (widget instanceof TabFolder folder) {
			int index = folder.getSelectionIndex();
			return index < 0 ? null : folder.getItem(index);
		}
		return null;
	}

	/** Returns the number of rows a tree or table holds, or an empty string. */
	static String itemCount(Widget widget) {
		if (widget instanceof Tree tree) {
			return "itemCount = " + tree.getItemCount() + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (widget instanceof Table table) {
			return "itemCount = " + table.getItemCount() + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return ""; //$NON-NLS-1$
	}

	/**
	 * Removes the identity hashes that {@link Object#toString()} appends, so that
	 * two reports taken from different sessions can be compared with a diff.
	 */
	static String withoutIdentityHashes(String text) {
		return IDENTITY_HASH.matcher(text).replaceAll("$1"); //$NON-NLS-1$
	}

	private static String modelTypeName(MUIElement element) {
		for (Class<?> type : element.getClass().getInterfaces()) {
			if (type.getName().startsWith("org.eclipse.e4.ui.model.")) { //$NON-NLS-1$
				return type.getSimpleName();
			}
		}
		return element.getClass().getSimpleName();
	}

	private static String truncate(String text) {
		String singleLine = text.replace('\n', ' ').replace('\r', ' ');
		if (singleLine.length() <= MAX_TEXT) {
			return singleLine;
		}
		return singleLine.substring(0, MAX_TEXT) + "..."; //$NON-NLS-1$
	}
}
