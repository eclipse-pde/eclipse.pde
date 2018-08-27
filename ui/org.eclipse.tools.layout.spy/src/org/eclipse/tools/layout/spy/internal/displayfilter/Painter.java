package org.eclipse.tools.layout.spy.internal.displayfilter;
/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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
 *******************************************************************************/


import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Layout;

/**
 * Painter.java
 */
public final class Painter extends Object {
	//
	// Static Fields
	//

	private static final int BUFFER_SIZE = 2048;

	private static final Color[] COLORS = new Color[] {
		Painter.getSystemColor(SWT.COLOR_RED),
		Painter.getSystemColor(SWT.COLOR_BLUE),
		Painter.getSystemColor(SWT.COLOR_YELLOW),
		Painter.getSystemColor(SWT.COLOR_GREEN),
		Painter.getSystemColor(SWT.COLOR_CYAN),
		Painter.getSystemColor(SWT.COLOR_MAGENTA),
		Painter.getSystemColor(SWT.COLOR_DARK_RED),
		Painter.getSystemColor(SWT.COLOR_DARK_BLUE),
		Painter.getSystemColor(SWT.COLOR_DARK_YELLOW),
		Painter.getSystemColor(SWT.COLOR_DARK_GREEN),
		Painter.getSystemColor(SWT.COLOR_DARK_CYAN),
		Painter.getSystemColor(SWT.COLOR_DARK_MAGENTA)
	};

	private static final String LINE_SEPARATOR = System.getProperty("line.separator");  //$NON-NLS-1$

	//
	// Static Methods
	//

	private static void createCompositeToolTip(StringBuilder buffer, Composite composite) {
		Layout layout = composite.getLayout();
		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("  layout=");  //$NON-NLS-1$

		if (layout instanceof GridLayout) {
			GridLayout gridLayout = (GridLayout) layout;
			Painter.createGridLayoutToolTip(buffer, gridLayout);
		} else if (layout instanceof FormLayout) {
			FormLayout formLayout = (FormLayout) layout;
			Painter.createFormLayoutToolTip(buffer, formLayout);
		} else if (layout instanceof FillLayout) {
			FillLayout fillLayout = (FillLayout) layout;
			Painter.createFillLayoutToolTip(buffer, fillLayout);
		} else if (layout instanceof RowLayout) {
			RowLayout rowLayout = (RowLayout) layout;
			Painter.createRowLayoutToolTip(buffer, rowLayout);
		} else {
			buffer.append(layout);
		}
	}

	private static void createFillLayoutToolTip(StringBuilder buffer, FillLayout layout) {
		buffer.append("FormLayout");  //$NON-NLS-1$

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    marginHeight=");  //$NON-NLS-1$
		buffer.append(layout.marginHeight);

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    marginWidth=");  //$NON-NLS-1$
		buffer.append(layout.marginWidth);

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    spacing=");  //$NON-NLS-1$
		buffer.append(layout.spacing);

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    type=");  //$NON-NLS-1$
		buffer.append(layout.type);
		buffer.append(" (");  //$NON-NLS-1$
		buffer.append(Painter.getLayoutTypeText(layout.type));
		buffer.append(')');
	}

	private static void createFormDataToolTip(StringBuilder buffer, FormData data) {
		buffer.append("FormData");  //$NON-NLS-1$

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    bottom=");  //$NON-NLS-1$
		buffer.append(data.bottom);

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    height=");  //$NON-NLS-1$
		buffer.append(data.height);

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    left=");  //$NON-NLS-1$
		buffer.append(data.left);

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    right=");  //$NON-NLS-1$
		buffer.append(data.right);

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    top=");  //$NON-NLS-1$
		buffer.append(data.top);

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    width=");  //$NON-NLS-1$
		buffer.append(data.width);
	}

	private static void createFormLayoutToolTip(StringBuilder buffer, FormLayout layout) {
		buffer.append("FormLayout");  //$NON-NLS-1$

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    marginBottom=");  //$NON-NLS-1$
		buffer.append(layout.marginBottom);

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    marginHeight=");  //$NON-NLS-1$
		buffer.append(layout.marginHeight);

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    marginLeft=");  //$NON-NLS-1$
		buffer.append(layout.marginLeft);

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    marginRight=");  //$NON-NLS-1$
		buffer.append(layout.marginRight);

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    marginTop=");  //$NON-NLS-1$
		buffer.append(layout.marginTop);

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    marginWidth=");  //$NON-NLS-1$
		buffer.append(layout.marginWidth);

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    spacing=");  //$NON-NLS-1$
		buffer.append(layout.spacing);
	}

	private static void createGridDataToolTip(StringBuilder buffer, GridData gridData) {
		buffer.append("GridData");  //$NON-NLS-1$

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    exclude=");  //$NON-NLS-1$
		buffer.append(gridData.exclude);

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    grabExcessHorizontalSpace=");  //$NON-NLS-1$
		buffer.append(gridData.grabExcessHorizontalSpace);

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    grabExcessVerticalSpace=");  //$NON-NLS-1$
		buffer.append(gridData.grabExcessVerticalSpace);

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    heightHint=");  //$NON-NLS-1$
		buffer.append(gridData.heightHint);

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    horizontalAlignment=");  //$NON-NLS-1$
		buffer.append(Painter.getAlignmentText(gridData.horizontalAlignment));

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    horizontalIndent=");  //$NON-NLS-1$
		buffer.append(gridData.horizontalIndent);

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    horizontalSpan=");  //$NON-NLS-1$
		buffer.append(gridData.horizontalSpan);

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    minimumHeight=");  //$NON-NLS-1$
		buffer.append(gridData.minimumHeight);

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    minimumWidth=");  //$NON-NLS-1$
		buffer.append(gridData.minimumWidth);

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    verticalAlignment=");  //$NON-NLS-1$
		buffer.append(Painter.getAlignmentText(gridData.verticalAlignment));

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    verticalIndent=");  //$NON-NLS-1$
		buffer.append(gridData.verticalIndent);

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    verticalSpan=");  //$NON-NLS-1$
		buffer.append(gridData.verticalSpan);

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    widthHint=");  //$NON-NLS-1$
		buffer.append(gridData.widthHint);
	}

	private static void createGridLayoutToolTip(StringBuilder buffer, GridLayout layout) {
		buffer.append("GridLayout");  //$NON-NLS-1$

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    horizontalSpacing=");  //$NON-NLS-1$
		buffer.append(layout.horizontalSpacing);

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    makeColumnsEqualWidth=");  //$NON-NLS-1$
		buffer.append(layout.makeColumnsEqualWidth);

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    marginBottom=");  //$NON-NLS-1$
		buffer.append(layout.marginBottom);

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    marginHeight=");  //$NON-NLS-1$
		buffer.append(layout.marginHeight);

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    marginLeft=");  //$NON-NLS-1$
		buffer.append(layout.marginLeft);

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    marginRight=");  //$NON-NLS-1$
		buffer.append(layout.marginRight);

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    marginTop=");  //$NON-NLS-1$
		buffer.append(layout.marginBottom);

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    marginWidth=");  //$NON-NLS-1$
		buffer.append(layout.marginWidth);

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    numColumns=");  //$NON-NLS-1$
		buffer.append(layout.numColumns);

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    verticalSpacing=");  //$NON-NLS-1$
		buffer.append(layout.verticalSpacing);
	}

	private static void createRowDataToolTip(StringBuilder buffer, RowData data) {
		buffer.append("RowData");  //$NON-NLS-1$

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    exclude=");  //$NON-NLS-1$
		buffer.append(data.exclude);

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    height=");  //$NON-NLS-1$
		buffer.append(data.height);

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    width=");  //$NON-NLS-1$
		buffer.append(data.width);
	}

	private static void createRowLayoutToolTip(StringBuilder buffer, RowLayout layout) {
		buffer.append("RowLayout");  //$NON-NLS-1$

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    fill=");  //$NON-NLS-1$
		buffer.append(layout.fill);

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    justify=");  //$NON-NLS-1$
		buffer.append(layout.justify);

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    marginBottom=");  //$NON-NLS-1$
		buffer.append(layout.marginBottom);

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    marginHeight=");  //$NON-NLS-1$
		buffer.append(layout.marginHeight);

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    marginLeft=");  //$NON-NLS-1$
		buffer.append(layout.marginLeft);

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    marginRight=");  //$NON-NLS-1$
		buffer.append(layout.marginRight);

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    marginTop=");  //$NON-NLS-1$
		buffer.append(layout.marginTop);

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    marginWidth=");  //$NON-NLS-1$
		buffer.append(layout.marginWidth);

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    pack=");  //$NON-NLS-1$
		buffer.append(layout.pack);

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    spacing=");  //$NON-NLS-1$
		buffer.append(layout.spacing);

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    type=");  //$NON-NLS-1$
		buffer.append(layout.type);
		buffer.append(" (");  //$NON-NLS-1$
		buffer.append(Painter.getLayoutTypeText(layout.type));
		buffer.append(')');

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("    wrap=");  //$NON-NLS-1$
		buffer.append(layout.wrap);
	}

	private static void createToolTip(StringBuilder buffer, Control control, int childIndex) {
		if (childIndex != 0) {
			buffer.append(childIndex);
			buffer.append(". ");  //$NON-NLS-1$
		}

		buffer.append(control);
		buffer.append(Painter.getHashCodeText(control));

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("  parent=");  //$NON-NLS-1$
		Control parent = control.getParent();
		buffer.append(parent);

		if (parent != null) {
			buffer.append(Painter.getHashCodeText(parent));
		}

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("  border width=");  //$NON-NLS-1$
		buffer.append(control.getBorderWidth());

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("  bounds=");  //$NON-NLS-1$
		buffer.append(control.getBounds());

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("  data=");  //$NON-NLS-1$
		buffer.append(control.getData());

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("  font=");  //$NON-NLS-1$
		Font font = control.getFont();
		FontData[] fontData = font.getFontData();
		buffer.append(fontData [ 0 ]);  // Always just one on Windows.

		if (control instanceof Composite) {
			Composite composite = (Composite) control;
			Painter.createCompositeToolTip(buffer, composite);
		}

		Object layoutData = control.getLayoutData();
		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("  layout data=");  //$NON-NLS-1$

		if (layoutData instanceof GridData) {
			GridData gridData = (GridData) layoutData;
			Painter.createGridDataToolTip(buffer, gridData);
		} else if (layoutData instanceof FormData) {
			FormData formData = (FormData) layoutData;
			Painter.createFormDataToolTip(buffer, formData);
		} else if (layoutData instanceof RowData) {
			RowData rowData = (RowData) layoutData;
			Painter.createRowDataToolTip(buffer, rowData);
		} else {
			buffer.append(layoutData);
		}

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("  location=");  //$NON-NLS-1$
		buffer.append(control.getLocation());

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("  size=");  //$NON-NLS-1$
		buffer.append(control.getSize());

		buffer.append(Painter.LINE_SEPARATOR);
		buffer.append("  style=");  //$NON-NLS-1$
		buffer.append(control.getStyle());
	}


	public static void decorate(Control control) {
		Painter.decorate(control, true);
	}

	public static void decorate(Control control, boolean toolTip) {
		Painter.decorate(control, 0, toolTip);
	}

	public static void decorate(Control control, int extraCompositeMargin, boolean toolTip) {
		if (control == null)
		 {
			throw new IllegalArgumentException("The argument 'control' must not be null");  //$NON-NLS-1$
		}
		if (!LayoutIssuesDebugFilter.IGNORE_BY_LAYOUT_ISSUES_DEBUG_FILTER.equals(control.getData())) {
			StringBuilder buffer = new StringBuilder(Painter.BUFFER_SIZE);
			Painter.decorate(buffer, control, extraCompositeMargin, 0, 0, toolTip);
		}
	}

	private static void decorate(StringBuilder buffer, Control control, int extraCompositeMargin, int colorIndex, int childIndex, boolean toolTip) {
		int count = Painter.COLORS.length;
		int index = colorIndex == count ? 0 : colorIndex;

		Color color = Painter.COLORS [ index ];
		control.setBackground(color);

		if (toolTip == true) {
			Painter.createToolTip(buffer, control, childIndex);
			String tip = Painter.getBufferValue(buffer);
			control.setToolTipText(tip);
		}

		if (control instanceof Composite) {
			Composite composite = (Composite) control;
			Painter.padComposite(composite, extraCompositeMargin);
			Control[] children = composite.getChildren();
			Control child;

			for (int i = 0; i < children.length; i++) {
				child = children [ i ];
				Painter.decorate(buffer, child, extraCompositeMargin, index + 1, i + 1, toolTip);
			}
		}
	}

	private static String getAlignmentText(int alignment) {
		String text = null;

		switch (alignment) {
			case GridData.CENTER:
				text = "GridData.CENTER";  //$NON-NLS-1$
				break;
			case GridData.END:
				text = "GridData.END";  //$NON-NLS-1$
				break;
			case SWT.BEGINNING:  // Same value as GridData.BEGINNING
				text = "SWT.BEGINNING";  //$NON-NLS-1$
				break;
			case SWT.BOTTOM:
				text = "SWT.BOTTOM";  //$NON-NLS-1$
				break;
			case SWT.CENTER:
				text = "SWT.CENTER";  //$NON-NLS-1$
				break;
			case SWT.END:
				text = "SWT.END";  //$NON-NLS-1$
				break;
			case SWT.FILL:  // Same value as GridData.FILL
				text = "SWT.FILL";  //$NON-NLS-1$
				break;
			case SWT.LEFT:
				text = "SWT.LEFT";  //$NON-NLS-1$
				break;
			case SWT.RIGHT:
				text = "SWT.RIGHT";  //$NON-NLS-1$
				break;
			case SWT.TOP:
				text = "SWT.TOP";  //$NON-NLS-1$
				break;
			default:
				text = Integer.toString(alignment);
				break;
		}

		return text;
	}

	private static String getBufferValue(StringBuilder buffer) {
		String value = buffer.toString();
		buffer.setLength(0);
		return value;
	}

	private static String getHashCodeText(Object object) {
		long hashCode = object.hashCode();
		String hexString = Long.toHexString(hashCode);

		StringBuilder buffer = new StringBuilder(15);
		buffer.append('(');
		buffer.append(hexString);
		buffer.append(')');

		String result = Painter.getBufferValue(buffer);
		return result;
	}

	private static String getLayoutTypeText(int type) {
		String text;

		switch (type) {
			case SWT.HORIZONTAL:
				text = "SWT.HORIZONTAL";  //$NON-NLS-1$
				break;
			case SWT.VERTICAL:
				text = "SWT.VERTICAL";  //$NON-NLS-1$
				break;
			default:
				text = Integer.toString(type);
				break;
		}

		return text;
	}

	private static Color getSystemColor(int id) {
		Device device = Display.getDefault();
		Color color = device.getSystemColor(id);
		return color;
	}

	private static void padComposite(Composite composite, int extraCompositeMargin) {
		Layout compositeLayout = composite.getLayout();
		if (compositeLayout == null)
		 {
			return;  // Early return.
		}

		if (extraCompositeMargin < 0)
		 {
			return; // Early return.
		}

		if (compositeLayout instanceof GridLayout) {
			GridLayout layout = (GridLayout) compositeLayout;
			layout.marginWidth += extraCompositeMargin;
			layout.marginHeight += extraCompositeMargin;
		} else if (compositeLayout instanceof RowLayout) {
			RowLayout layout = (RowLayout) compositeLayout;
			layout.marginWidth += extraCompositeMargin;
			layout.marginHeight += extraCompositeMargin;
		} else if (compositeLayout instanceof FillLayout) {
			FillLayout layout = (FillLayout) compositeLayout;
			layout.marginWidth += extraCompositeMargin;
			layout.marginHeight += extraCompositeMargin;
		} else if (compositeLayout instanceof FormLayout) {
			FormLayout layout = (FormLayout) compositeLayout;
			layout.marginWidth += extraCompositeMargin;
			layout.marginHeight += extraCompositeMargin;
		}
	}

	public static void setBackground(Control control, int id) {
		Color color = Painter.getSystemColor(id);
		control.setBackground(color);
	}

	//
	// Constructors
	//

	private Painter() {
		// not allowed to construct
	}
}
