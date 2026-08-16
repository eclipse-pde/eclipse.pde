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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;

/**
 * Renders SWT style bits as constant names.
 * <p>
 * Style bits are not globally unique, the same bit means different things on
 * different widgets, for example {@code SWT.H_SCROLL} and {@code SWT.HORIZONTAL}
 * or {@code SWT.CENTER} and {@code SWT.EMBEDDED}. The names are therefore looked
 * up per widget class and bits without a known meaning are kept as hexadecimal.
 */
final class SwtStyles {

	private record Bit(int mask, String name) {
	}

	/** Bits that carry the same meaning on every widget. */
	private static final List<Bit> UNIVERSAL = List.of(bit(SWT.BORDER, "BORDER"), //$NON-NLS-1$
			bit(SWT.LEFT_TO_RIGHT, "LEFT_TO_RIGHT"), bit(SWT.RIGHT_TO_LEFT, "RIGHT_TO_LEFT"), //$NON-NLS-1$ //$NON-NLS-2$
			bit(SWT.MIRRORED, "MIRRORED"), bit(SWT.FLIP_TEXT_DIRECTION, "FLIP_TEXT_DIRECTION"), //$NON-NLS-1$ //$NON-NLS-2$
			bit(SWT.DOUBLE_BUFFERED, "DOUBLE_BUFFERED"), bit(SWT.NO_BACKGROUND, "NO_BACKGROUND"), //$NON-NLS-1$ //$NON-NLS-2$
			bit(SWT.NO_FOCUS, "NO_FOCUS"), bit(SWT.NO_REDRAW_RESIZE, "NO_REDRAW_RESIZE"), //$NON-NLS-1$ //$NON-NLS-2$
			bit(SWT.NO_MERGE_PAINTS, "NO_MERGE_PAINTS")); //$NON-NLS-1$

	private static final List<Bit> SCROLLABLE = List.of(bit(SWT.H_SCROLL, "H_SCROLL"), //$NON-NLS-1$
			bit(SWT.V_SCROLL, "V_SCROLL"), bit(SWT.NO_SCROLL, "NO_SCROLL")); //$NON-NLS-1$ //$NON-NLS-2$

	private static final List<Bit> ORIENTATION = List.of(bit(SWT.HORIZONTAL, "HORIZONTAL"), //$NON-NLS-1$
			bit(SWT.VERTICAL, "VERTICAL")); //$NON-NLS-1$

	private static final List<Bit> SHADOW = List.of(bit(SWT.SHADOW_IN, "SHADOW_IN"), //$NON-NLS-1$
			bit(SWT.SHADOW_OUT, "SHADOW_OUT"), bit(SWT.SHADOW_NONE, "SHADOW_NONE"), //$NON-NLS-1$ //$NON-NLS-2$
			bit(SWT.SHADOW_ETCHED_IN, "SHADOW_ETCHED_IN"), bit(SWT.SHADOW_ETCHED_OUT, "SHADOW_ETCHED_OUT")); //$NON-NLS-1$ //$NON-NLS-2$

	private static final List<Bit> ALIGNMENT = List.of(bit(SWT.LEFT, "LEFT"), bit(SWT.RIGHT, "RIGHT"), //$NON-NLS-1$ //$NON-NLS-2$
			bit(SWT.CENTER, "CENTER")); //$NON-NLS-1$

	private static final List<Bit> SELECTION = List.of(bit(SWT.SINGLE, "SINGLE"), bit(SWT.MULTI, "MULTI")); //$NON-NLS-1$ //$NON-NLS-2$

	/**
	 * Style names per widget class. The most specific class wins, the lookup walks
	 * up the class hierarchy.
	 */
	private static final Map<Class<?>, List<Bit>> BY_CLASS = byClass();

	private SwtStyles() {
	}

	private static Bit bit(int mask, String name) {
		return new Bit(mask, "SWT." + name); //$NON-NLS-1$
	}

	private static Map<Class<?>, List<Bit>> byClass() {
		Map<Class<?>, List<Bit>> map = new LinkedHashMap<>();
		map.put(Button.class, concat(List.of(bit(SWT.PUSH, "PUSH"), bit(SWT.CHECK, "CHECK"), bit(SWT.RADIO, "RADIO"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				bit(SWT.TOGGLE, "TOGGLE"), bit(SWT.ARROW, "ARROW"), bit(SWT.FLAT, "FLAT"), bit(SWT.WRAP, "WRAP"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				bit(SWT.UP, "UP"), bit(SWT.DOWN, "DOWN")), ALIGNMENT)); //$NON-NLS-1$ //$NON-NLS-2$
		map.put(Label.class, concat(List.of(bit(SWT.SEPARATOR, "SEPARATOR"), bit(SWT.WRAP, "WRAP")), ORIENTATION, //$NON-NLS-1$ //$NON-NLS-2$
				SHADOW, ALIGNMENT));
		map.put(CLabel.class, concat(SHADOW, ALIGNMENT));
		map.put(Text.class, concat(List.of(bit(SWT.READ_ONLY, "READ_ONLY"), bit(SWT.WRAP, "WRAP"), //$NON-NLS-1$ //$NON-NLS-2$
				bit(SWT.PASSWORD, "PASSWORD"), bit(SWT.SEARCH, "SEARCH"), bit(SWT.ICON_SEARCH, "ICON_SEARCH"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				bit(SWT.ICON_CANCEL, "ICON_CANCEL")), SELECTION, ALIGNMENT)); //$NON-NLS-1$
		map.put(StyledText.class, concat(List.of(bit(SWT.READ_ONLY, "READ_ONLY"), bit(SWT.WRAP, "WRAP"), //$NON-NLS-1$ //$NON-NLS-2$
				bit(SWT.FULL_SELECTION, "FULL_SELECTION")), SELECTION, SCROLLABLE)); //$NON-NLS-1$
		map.put(Combo.class, List.of(bit(SWT.DROP_DOWN, "DROP_DOWN"), bit(SWT.READ_ONLY, "READ_ONLY"), //$NON-NLS-1$ //$NON-NLS-2$
				bit(SWT.SIMPLE, "SIMPLE"))); //$NON-NLS-1$
		map.put(Spinner.class, List.of(bit(SWT.READ_ONLY, "READ_ONLY"), bit(SWT.WRAP, "WRAP"))); //$NON-NLS-1$ //$NON-NLS-2$
		map.put(Link.class, List.of(bit(SWT.WRAP, "WRAP"))); //$NON-NLS-1$
		map.put(Tree.class, concat(itemStyles(), SELECTION, SCROLLABLE));
		map.put(Table.class, concat(itemStyles(), SELECTION, SCROLLABLE));
		map.put(org.eclipse.swt.widgets.List.class, concat(SELECTION, SCROLLABLE));
		map.put(ToolBar.class, concat(List.of(bit(SWT.FLAT, "FLAT"), bit(SWT.WRAP, "WRAP"), bit(SWT.RIGHT, "RIGHT"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				bit(SWT.SHADOW_OUT, "SHADOW_OUT")), ORIENTATION)); //$NON-NLS-1$
		map.put(CoolBar.class, concat(List.of(bit(SWT.FLAT, "FLAT")), ORIENTATION)); //$NON-NLS-1$
		map.put(TabFolder.class, List.of(bit(SWT.TOP, "TOP"), bit(SWT.BOTTOM, "BOTTOM"))); //$NON-NLS-1$ //$NON-NLS-2$
		map.put(CTabFolder.class, concat(List.of(bit(SWT.TOP, "TOP"), bit(SWT.BOTTOM, "BOTTOM"), bit(SWT.FLAT, "FLAT"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				bit(SWT.CLOSE, "CLOSE")), SELECTION)); //$NON-NLS-1$
		map.put(Group.class, SHADOW);
		map.put(Sash.class, concat(ORIENTATION, List.of(bit(SWT.SMOOTH, "SMOOTH")))); //$NON-NLS-1$
		map.put(SashForm.class, ORIENTATION);
		map.put(Slider.class, ORIENTATION);
		map.put(Scale.class, ORIENTATION);
		map.put(ProgressBar.class, concat(ORIENTATION,
				List.of(bit(SWT.SMOOTH, "SMOOTH"), bit(SWT.INDETERMINATE, "INDETERMINATE")))); //$NON-NLS-1$ //$NON-NLS-2$
		map.put(Shell.class, List.of(bit(SWT.CLOSE, "CLOSE"), bit(SWT.TITLE, "TITLE"), bit(SWT.MIN, "MIN"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				bit(SWT.MAX, "MAX"), bit(SWT.RESIZE, "RESIZE"), bit(SWT.ON_TOP, "ON_TOP"), bit(SWT.TOOL, "TOOL"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				bit(SWT.NO_TRIM, "NO_TRIM"), bit(SWT.NO_MOVE, "NO_MOVE"), bit(SWT.SHEET, "SHEET"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				bit(SWT.PRIMARY_MODAL, "PRIMARY_MODAL"), bit(SWT.APPLICATION_MODAL, "APPLICATION_MODAL"))); //$NON-NLS-1$ //$NON-NLS-2$
		map.put(ToolItem.class, List.of(bit(SWT.PUSH, "PUSH"), bit(SWT.CHECK, "CHECK"), bit(SWT.RADIO, "RADIO"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				bit(SWT.SEPARATOR, "SEPARATOR"), bit(SWT.DROP_DOWN, "DROP_DOWN"))); //$NON-NLS-1$ //$NON-NLS-2$
		map.put(Canvas.class, SCROLLABLE);
		map.put(Composite.class, concat(SCROLLABLE, List.of(bit(SWT.NO_RADIO_GROUP, "NO_RADIO_GROUP")))); //$NON-NLS-1$
		return map;
	}

	private static List<Bit> itemStyles() {
		return List.of(bit(SWT.CHECK, "CHECK"), bit(SWT.FULL_SELECTION, "FULL_SELECTION"), //$NON-NLS-1$ //$NON-NLS-2$
				bit(SWT.HIDE_SELECTION, "HIDE_SELECTION"), bit(SWT.VIRTUAL, "VIRTUAL")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@SafeVarargs
	private static List<Bit> concat(List<Bit>... parts) {
		List<Bit> all = new ArrayList<>();
		for (List<Bit> part : parts) {
			all.addAll(part);
		}
		return all;
	}

	/**
	 * Returns the style of the control as SWT constant names followed by the raw
	 * value, for example {@code SWT.BORDER | SWT.V_SCROLL (0x22000a00)}.
	 */
	static String describe(Control control) {
		return describe(control.getClass(), control.getStyle());
	}

	static String describe(Class<?> widgetClass, int style) {
		StringBuilder builder = new StringBuilder();
		int remaining = style;

		List<Bit> known = concat(namesFor(widgetClass), UNIVERSAL);
		for (Bit bit : known) {
			if (bit.mask() != 0 && (remaining & bit.mask()) == bit.mask()) {
				append(builder, bit.name());
				remaining &= ~bit.mask();
			}
		}
		if (remaining != 0) {
			append(builder, "0x" + Integer.toHexString(remaining)); //$NON-NLS-1$
		}
		if (builder.length() == 0) {
			append(builder, "SWT.NONE"); //$NON-NLS-1$
		}
		return builder + " (0x" + Integer.toHexString(style) + ")"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static List<Bit> namesFor(Class<?> widgetClass) {
		for (Class<?> current = widgetClass; current != null; current = current.getSuperclass()) {
			List<Bit> bits = BY_CLASS.get(current);
			if (bits != null) {
				return bits;
			}
		}
		return List.of();
	}

	private static void append(StringBuilder builder, String name) {
		if (builder.length() > 0) {
			builder.append(" | "); //$NON-NLS-1$
		}
		builder.append(name);
	}

	/**
	 * Returns the SWT constant name for an orientation value as used by
	 * {@code FillLayout.type} and {@code RowLayout.type}.
	 */
	static String describeOrientation(int value) {
		if (value == SWT.HORIZONTAL) {
			return "SWT.HORIZONTAL (" + value + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (value == SWT.VERTICAL) {
			return "SWT.VERTICAL (" + value + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return Integer.toString(value);
	}

	/**
	 * Returns the constant name for a {@link org.eclipse.swt.layout.GridData}
	 * alignment. The deprecated {@code GridData} constants are numerically
	 * different from the {@code SWT} ones, which is why JFace falls back to
	 * printing them as plain numbers.
	 */
	static String describeAlignment(int value) {
		return switch (value) {
		case SWT.BEGINNING -> "SWT.BEGINNING"; //$NON-NLS-1$
		case SWT.FILL -> "SWT.FILL"; //$NON-NLS-1$
		case SWT.CENTER -> "SWT.CENTER"; //$NON-NLS-1$
		case SWT.END -> "SWT.END"; //$NON-NLS-1$
		case SWT.TOP -> "SWT.TOP"; //$NON-NLS-1$
		case SWT.BOTTOM -> "SWT.BOTTOM"; //$NON-NLS-1$
		case 2 -> "GridData.CENTER"; //$NON-NLS-1$
		case 3 -> "GridData.END"; //$NON-NLS-1$
		default -> Integer.toString(value);
		};
	}
}
