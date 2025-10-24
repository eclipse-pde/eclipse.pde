package org.eclipse.pde.spy.adapter.viewer;

import org.eclipse.jface.util.Policy;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ViewerRow;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;

public class ColumnViewerToolTipSupportCustom extends ColumnViewerToolTipSupport {

	private ColumnViewer viewer;
	private static final String VIEWER_CELL_KEY = Policy.JFACE
			+ "_VIEWER_CELL_KEY"; //$NON-NLS-1$

	private StyleRange [] ranges;
	protected ColumnViewerToolTipSupportCustom(ColumnViewer viewer, int style, boolean manualActivation) {
		super(viewer, style, manualActivation);
		this.viewer = viewer;
	}

	/**
	 * Enable ToolTip support for the viewer by creating an instance from this
	 * class. To get all necessary informations this support class consults the
	 * {@link CellLabelProvider}.
	 *
	 * @param viewer
	 *            the viewer the support is attached to
	 */
	public static void enableFor(ColumnViewer viewer) {
		new ColumnViewerToolTipSupportCustom(viewer, ToolTip.NO_RECREATE, false);
	}

	@Override
	protected boolean shouldCreateToolTip(Event event) {
		boolean rv = super.shouldCreateToolTip(event);
		if (!rv) {
			return false;
		}
		Point pt = new Point(event.x,event.y);
		ViewerRow row = viewer.getCell(pt).getViewerRow();
		Object element = row.getItem().getData();
		ColumnLabelProviderCustom customlabelProvider = (ColumnLabelProviderCustom) viewer.getLabelProvider(viewer.getCell(pt).getColumnIndex());		ranges = customlabelProvider.getToolTipStyleRanges(element);
		ranges = customlabelProvider.getToolTipStyleRanges(element);
		String txt = customlabelProvider.getToolTipText(element);
		return !txt.isEmpty();
	}

	
	@Override
	protected Composite createToolTipContentArea(Event event, Composite parent) {
		setData(VIEWER_CELL_KEY, null);
		String text = getText(event);
		Color bgColor = getBackgroundColor(event);
		StyledText styledText = new  StyledText(parent, SWT.NONE);
		if (text != null) {
			styledText.setText(text);
		}
		if ( ranges != null) {
			
			styledText.setStyleRanges(ranges);
		}
		if(bgColor != null) {
			styledText.setBackground(bgColor);
		}
		return styledText;
	}
	
}
