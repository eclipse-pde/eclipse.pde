/*******************************************************************************
 * Copyright (c) 2016 Google Inc and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stefan Xenos (Google) - initial API and implementation
 *     Patrik Suzzi <psuzzi@gmail.com> - Bug 499226
 *******************************************************************************/
package org.eclipse.tools.layout.spy.internal.dialogs;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.eclipse.core.databinding.observable.sideeffect.ISideEffectFactory;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.WidgetSideEffects;
import org.eclipse.jface.databinding.swt.typed.WidgetProperties;
import org.eclipse.jface.databinding.viewers.IViewerObservableValue;
import org.eclipse.jface.databinding.viewers.typed.ViewerProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.LayoutConstants;
import org.eclipse.jface.util.Geometry;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Scrollable;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.tools.layout.spy.internal.displayfilter.LayoutIssuesDebugFilter;

/**
 * Implementation of the "layout spy" dialog, a diagnostic tool for fixing bugs
 * related to control positioning and the implementation of SWT {@link Control}s
 * and {@link Layout}s.
 */
public class LayoutSpyDialog {
	private static final int EDGE_SIZE = 4;
	/**
	 * Value used to indicate an unknown hint value
	 */
	private static final int UNKNOWN = -2;
	/** The shell owned by the standalone dialog, or {@code null} when hosted in a part. */
	private Shell shell;

	// Controls
	private TreeViewer widgetTree;
	private Text details;
	private Button selectWidgetButton;
	private Button findClassButton;
	private Text modelInfo;
	private Shell overlay;

	// Model
	private final WritableValue<Boolean> controlSelectorOpen = new WritableValue<>(Boolean.FALSE, null);
	private IViewerObservableValue<@Nullable Control> selectedControl;
	private final Color parentRectangleColor = new Color(255, 0, 0);
	private final Color childRectangleColor = new Color(255, 255, 0);
	private Region region;
	private ISWTObservableValue<Boolean> overlayEnabled;
	private Text diagnostics;
	private Button showColoringButton;

	private static class LayoutSpyLabelProvider extends ColumnLabelProvider {
		@Override
		public String getText(Object element) {
			if (element instanceof Control control && control.isDisposed()) {
				return "<disposed>"; //$NON-NLS-1$
			}
			return super.getText(element);
		}

		@Override
		public Color getForeground(Object element) {
			Control child = (Control) element;
			if (child == null || child.isDisposed()) {
				return null;
			}
			if (!child.isVisible()) {
				return child.getDisplay().getSystemColor(SWT.COLOR_WIDGET_DISABLED_FOREGROUND);
			}
			return null;
		}

	}

	/**
	 * Provides the SWT control hierarchy to the widget tree: the shells of the
	 * display are the roots and the children of a {@link Composite} are its SWT
	 * children.
	 */
	private static class WidgetTreeContentProvider implements ITreeContentProvider {
		@Override
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof Display display) {
				return display.getShells();
			}
			return new Object[0];
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof Composite composite && !composite.isDisposed()) {
				return composite.getChildren();
			}
			return new Object[0];
		}

		@Override
		public @Nullable Object getParent(Object element) {
			if (element instanceof Control control && !control.isDisposed()) {
				return control.getParent();
			}
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return element instanceof Composite composite && !composite.isDisposed()
					&& composite.getChildren().length > 0;
		}
	}

	/**
	 * Creates the layout spy in its own shell but does not make it visible. Used
	 * by the standalone command so the spy can still be opened on top of blocking
	 * dialogs.
	 *
	 * @param parentShell
	 *            the parent shell
	 */
	public LayoutSpyDialog(Shell parentShell) {
		shell = new Shell(parentShell, SWT.SHELL_TRIM);
		shell.setText(Messages.LayoutSpyDialog_shell_text);
		createContents(shell);
		openControl(parentShell);
	}

	/**
	 * Creates the layout spy inside the given composite, for example a part
	 * hosted in the PDE spy window. The spy does not own the surrounding shell in
	 * this case.
	 *
	 * @param parent
	 *            the composite the spy contents are built into
	 */
	public LayoutSpyDialog(Composite parent) {
		createContents(parent);
	}

	private void createContents(Composite container) {
		overlay = new Shell(SWT.ON_TOP | SWT.NO_TRIM);
		{
			overlay.addPaintListener(this::paintOverlay);
			region = new Region();
			overlay.addDisposeListener(e -> region.dispose());
			overlay.setRegion(region);
		}

		Composite infoRegion = new Composite(container, SWT.NONE);
		{
			Label treeLabel = new Label(infoRegion, SWT.NONE);
			treeLabel.setText(Messages.LayoutSpyDialog_label_widget_tree);

			Label detailsLabel = new Label(infoRegion, SWT.NONE);
			detailsLabel.setText(Messages.LayoutSpyDialog_label_layout);

			widgetTree = new TreeViewer(infoRegion, SWT.BORDER | SWT.SINGLE);
			GridDataFactory.fillDefaults().hint(300, 300).grab(true, true).applyTo(widgetTree.getControl());

			details = new Text(infoRegion, SWT.READ_ONLY | SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
			GridDataFactory.fillDefaults().hint(300, 300).grab(true, true).applyTo(details);

			GridLayoutFactory.fillDefaults().numColumns(2).equalWidth(true).generateLayout(infoRegion);
		}

		diagnostics = new Text(container, SWT.READ_ONLY | SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		GridDataFactory.fillDefaults().hint(300, 300).grab(true, true).applyTo(diagnostics);

		Button showOverlayButton = new Button(container, SWT.CHECK);
		showOverlayButton.setText(Messages.LayoutSpyDialog_button_show_overlay);

		showColoringButton = new Button(container, SWT.CHECK);
		showColoringButton.setText(Messages.LayoutSpyDialog_button_show_coloring);
		showColoringButton.addSelectionListener(widgetSelectedAdapter(e-> {
			LayoutIssuesDebugFilter.activate(showColoringButton.getSelection(), true, 0);
		}));
		showColoringButton.addDisposeListener((e -> LayoutIssuesDebugFilter.activate(false, true, 0)));

		Composite buttonBar = new Composite(container, SWT.NONE);
		{
			selectWidgetButton = new Button(buttonBar, SWT.PUSH);
			selectWidgetButton.setText(Messages.LayoutSpyDialog_button_select_control);
			findClassButton = new Button(buttonBar, SWT.PUSH);
			findClassButton.setText(Messages.LayoutSpyDialog_button_find_class);
			Button refreshButton = new Button(buttonBar, SWT.PUSH);
			refreshButton.setText(Messages.LayoutSpyDialog_button_refresh);
			refreshButton.addListener(SWT.Selection, event -> refreshTree());

			GridLayoutFactory.fillDefaults().numColumns(3).generateLayout(buttonBar);
		}
		GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.CENTER).applyTo(buttonBar);

		// Result of "Find Class": model element and implementing class of a clicked control.
		Label modelLabel = new Label(container, SWT.NONE);
		modelLabel.setText(Messages.LayoutSpyDialog_label_model_element);
		modelInfo = new Text(container, SWT.READ_ONLY | SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		modelInfo.setText(Messages.LayoutSpyDialog_model_prompt);
		GridDataFactory.fillDefaults().hint(300, 90).grab(true, false).applyTo(modelInfo);

		GridLayoutFactory.fillDefaults().margins(LayoutConstants.getMargins()).generateLayout(container);

		// Attach listeners
		container.addDisposeListener(event -> disposed());
		selectWidgetButton.addListener(SWT.Selection, event -> selectControl());
		findClassButton.addListener(SWT.Selection, event -> findClass());

		// Set up the model
		widgetTree.setContentProvider(new WidgetTreeContentProvider());
		widgetTree.setLabelProvider(new LayoutSpyLabelProvider());
		widgetTree.setInput(container.getDisplay());
		widgetTree.addDoubleClickListener(event -> {
			Object element = widgetTree.getStructuredSelection().getFirstElement();
			if (element != null) {
				widgetTree.expandToLevel(element, 1);
			}
		});
		selectedControl = ViewerProperties.singleSelection(Control.class).observe(widgetTree);
		overlayEnabled = WidgetProperties.buttonSelection().observe(showOverlayButton);
		createContextMenu();
		ISideEffectFactory sideEffectFactory = WidgetSideEffects.createFactory(container);
		sideEffectFactory.create(this::computeLayoutInfo, details::setText);
		sideEffectFactory.create(this::computeControlInfo, diagnostics::setText);
		sideEffectFactory.create(this::updateOverlay);


		// ignore controls to the layout spy from coloring
		container.setData(LayoutIssuesDebugFilter.IGNORE_BY_LAYOUT_ISSUES_DEBUG_FILTER);
		setChildrenColoring(container);
	}

	/**
	 * Adds a context menu to the widget tree that copies the diagnostics of the
	 * selected node and its children to the clipboard.
	 */
	private void createContextMenu() {
		MenuManager menuManager = new MenuManager();
		menuManager.setRemoveAllWhenShown(true);
		menuManager.addMenuListener(manager -> {
			Action copyAction = new Action(Messages.LayoutSpyDialog_menu_copy_widget_info) {
				@Override
				public void run() {
					copySelectionToClipboard();
				}
			};
			copyAction.setEnabled(getSelectedControl() != null);
			manager.add(copyAction);
		});
		Menu menu = menuManager.createContextMenu(widgetTree.getControl());
		widgetTree.getControl().setMenu(menu);
	}

	private void setChildrenColoring(Control control) {
		control.setData(LayoutIssuesDebugFilter.IGNORE_BY_LAYOUT_ISSUES_DEBUG_FILTER);
		if (control instanceof Composite c) {
			for (Control child : c.getChildren()) {
				setChildrenColoring(child);
			}
		}
	}


	/**
	 * Opens the dialog box, revealing it to the user.
	 */
	public void open() {
		this.shell.pack();
		this.shell.open();
	}

	/**
	 * Disposes the dialog box.
	 */
	public void close() {
		this.shell.dispose();
	}

	/**
	 * Invoked as a callback when the main shell is disposed.
	 */
	private void disposed() {
		showColoringButton.dispose();
		selectedControl.dispose();

		overlay.dispose();
	}

	/**
	 * Re-reads the SWT control hierarchy into the widget tree, keeping the
	 * current selection if it still exists.
	 */
	private void refreshTree() {
		if (widgetTree.getControl().isDisposed()) {
			return;
		}
		Control selected = getSelectedControl();
		widgetTree.refresh();
		if (selected != null && !selected.isDisposed()) {
			widgetTree.setSelection(new StructuredSelection(selected), true);
		}
	}

	/**
	 * Returns the control currently selected in the widget tree or null if none.
	 */
	private @Nullable Control getSelectedControl() {
		return selectedControl.getValue();
	}

	/**
	 * Selects and reveals the given control in the widget tree.
	 */
	private void openControl(Control control) {
		if (widgetTree.getControl().isDisposed()) {
			return;
		}
		widgetTree.refresh();
		widgetTree.setSelection(new StructuredSelection(control), true);
	}

	/**
	 * Returns the composite whose bounds should be drawn as the parent rectangle
	 * in the overlay, that is the selected control's parent or the selected
	 * control itself when it is a shell.
	 */
	private @Nullable Composite overlayParent() {
		Control selected = getSelectedControl();
		if (selected == null || selected.isDisposed()) {
			return null;
		}
		Composite parent = selected.getParent();
		if (parent != null) {
			return parent;
		}
		return selected instanceof Composite composite ? composite : null;
	}

	/**
	 * Returns the control that should be drawn as the child rectangle in the
	 * overlay, or null when the selection is a shell with no parent.
	 */
	private @Nullable Control overlayChild() {
		Control selected = getSelectedControl();
		if (selected == null || selected.isDisposed() || selected.getParent() == null) {
			return null;
		}
		return selected;
	}

	// Overlay management
	// -----------------------------------------------------------------

	/**
	 * This callback is used to update the bounds and visible region for the
	 * overlay shell. It is used as part of a side-effect, so if it makes use of
	 * any tracked getters, it will automatically be invoked again whenever one
	 * of those tracked getters changes state.
	 *
	 * @TrackedGetter
	 */
	public void updateOverlay() {
		@Nullable
		Composite parent = overlayParent();

		boolean enabled = Boolean.TRUE.equals(overlayEnabled.getValue());

		overlay.setVisible(parent != null && !controlSelectorOpen.getValue() && enabled);
		if (parent == null) {
			return;
		}
		Shell shell = parent.getShell();
		Rectangle outerBounds = Geometry.copy(shell.getBounds());
		overlay.setBounds(outerBounds);
		Rectangle parentBoundsWrtDisplay = GeometryUtil.getDisplayBounds(parent);

		Rectangle parentBoundsWrtOverlay = Geometry.toControl(overlay, parentBoundsWrtDisplay);
		Rectangle innerBoundsWrtOverlay = Geometry.copy(parentBoundsWrtOverlay);
		Geometry.expand(innerBoundsWrtOverlay, -EDGE_SIZE, -EDGE_SIZE, -EDGE_SIZE, -EDGE_SIZE);
		region.dispose();
		region = new Region();
		@Nullable
		Control child = overlayChild();
		if (child != null) {
			Rectangle childBoundsWrtOverlay = Geometry.toControl(overlay, GeometryUtil.getDisplayBounds(child));
			Rectangle childInnerBoundsWrtOverlay = Geometry.copy(childBoundsWrtOverlay);
			Geometry.expand(childInnerBoundsWrtOverlay, -EDGE_SIZE, -EDGE_SIZE, -EDGE_SIZE, -EDGE_SIZE);
			region.add(parentBoundsWrtOverlay);
			int distanceToTop = childBoundsWrtOverlay.y - innerBoundsWrtOverlay.y;
			subtractRect(region, GeometryUtil.extrudeEdge(innerBoundsWrtOverlay, distanceToTop, SWT.TOP));
			int distanceToLeft = childBoundsWrtOverlay.x - innerBoundsWrtOverlay.x;
			subtractRect(region, GeometryUtil.extrudeEdge(innerBoundsWrtOverlay, distanceToLeft, SWT.LEFT));
			int distanceToRight = GeometryUtil.getRight(innerBoundsWrtOverlay) - GeometryUtil.getRight(childBoundsWrtOverlay);
			subtractRect(region, GeometryUtil.extrudeEdge(innerBoundsWrtOverlay, distanceToRight, SWT.RIGHT));
			int distanceToBottom = GeometryUtil.getBottom(innerBoundsWrtOverlay) - GeometryUtil.getBottom(childBoundsWrtOverlay);
			subtractRect(region, GeometryUtil.extrudeEdge(innerBoundsWrtOverlay, distanceToBottom, SWT.BOTTOM));

			subtractRect(region, childInnerBoundsWrtOverlay);
		} else {
			region.add(parentBoundsWrtOverlay);
			region.subtract(innerBoundsWrtOverlay);
		}

		overlay.redraw();
		overlay.setRegion(region);
	}

	/**
	 * Paint callback for the overlay shell. This draws rectangles around the
	 * selected layout and the selected child.
	 */
	protected void paintOverlay(PaintEvent e) {
		@Nullable
		Composite parent = overlayParent();
		if (parent == null) {
			return;
		}
		int halfSize = EDGE_SIZE / 2;
		Rectangle parentDisplayBounds = GeometryUtil.getDisplayBounds(parent);
		Rectangle parentBoundsWrtOverlay = Geometry.toControl(overlay, parentDisplayBounds);
		Geometry.expand(parentBoundsWrtOverlay, -halfSize, -halfSize, -halfSize, -halfSize);

		@Nullable
		Control child = overlayChild();
		e.gc.setLineWidth(EDGE_SIZE);
		e.gc.setForeground(parentRectangleColor);
		e.gc.drawRectangle(parentBoundsWrtOverlay.x, parentBoundsWrtOverlay.y, parentBoundsWrtOverlay.width,
				parentBoundsWrtOverlay.height);

		if (child != null) {
			Rectangle childBoundsWrtOverlay = Geometry.toControl(overlay, GeometryUtil.getDisplayBounds(child));
			Geometry.expand(childBoundsWrtOverlay, -halfSize, -halfSize, -halfSize, -halfSize);
			e.gc.setForeground(childRectangleColor);
			e.gc.drawRectangle(childBoundsWrtOverlay.x, childBoundsWrtOverlay.y, childBoundsWrtOverlay.width,
					childBoundsWrtOverlay.height);
		}
	}

	// User gesture callbacks
	// -----------------------------------------------------------

	/**
	 * Invoked when the user clicks the "select control" button. It opens some
	 * UI that allows the user to select a new input control for the layout spy.
	 */
	private void selectControl() {
		this.controlSelectorOpen.setValue(true);
		// Only hide our own dialog; as a part this shell is the workbench window.
		boolean ownsShell = shell != null;
		if (ownsShell) {
			shell.setVisible(false);
		}
		new ControlSelector((@Nullable Control control) -> {
			if (control != null) {
				openControl(control);
			}
			this.controlSelectorOpen.setValue(false);
			if (ownsShell) {
				shell.setVisible(true);
			}
		});
	}

	/**
	 * Hides the spy, lets the user click a control and then shows the owning
	 * application-model element and its implementing class.
	 */
	private void findClass() {
		this.controlSelectorOpen.setValue(true);
		// Only hide our own dialog; as a part this shell is the workbench window.
		boolean ownsShell = shell != null;
		if (ownsShell) {
			shell.setVisible(false);
		}
		new ControlSelector((@Nullable Control control) -> {
			if (control != null && !modelInfo.isDisposed()) {
				modelInfo.setText(ModelElementResolver.describe(control));
			}
			this.controlSelectorOpen.setValue(false);
			if (ownsShell) {
				shell.setVisible(true);
			}
		});
	}

	/**
	 * Copies the diagnostic information of the selected control and all of its
	 * descendants to the clipboard as text, for pasting into bug reports.
	 */
	private void copySelectionToClipboard() {
		Control selected = getSelectedControl();
		if (selected == null || selected.isDisposed()) {
			return;
		}
		StringBuilder builder = new StringBuilder();
		appendControlSubtree(selected, builder, 0);

		Clipboard clipboard = new Clipboard(widgetTree.getControl().getDisplay());
		try {
			clipboard.setContents(new Object[] { builder.toString() }, new Transfer[] { TextTransfer.getInstance() });
		} finally {
			clipboard.dispose();
		}
	}

	/**
	 * Appends the description of the given control, and recursively of its
	 * children, to the builder, indented by tree depth.
	 */
	private void appendControlSubtree(Control control, StringBuilder builder, int depth) {
		String indent = "  ".repeat(depth); //$NON-NLS-1$
		builder.append(indent).append(control.getClass().getName());
		builder.append(NLS.bind(" {0}", control.getBounds())); //$NON-NLS-1$
		builder.append("\n"); //$NON-NLS-1$

		StringBuilder node = new StringBuilder();
		describeControlGeometry(node, control);
		node.append("Layout:\n"); //$NON-NLS-1$
		describeControlLayout(node, control);
		for (String line : node.toString().split("\n")) { //$NON-NLS-1$
			if (line.isEmpty()) {
				builder.append("\n"); //$NON-NLS-1$
			} else {
				builder.append(indent).append("  ").append(line).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		builder.append("\n"); //$NON-NLS-1$

		if (control instanceof Composite composite && !composite.isDisposed()) {
			for (Control child : composite.getChildren()) {
				appendControlSubtree(child, builder, depth + 1);
			}
		}
	}

	// Utility functions -----------------------------------------------------

	/**
	 * Subtracts the given rectangle from the given region unless the rectangle
	 * is empty.
	 */
	private static void subtractRect(Region region, Rectangle rect) {
		if (rect.isEmpty()) {
			return;
		}
		region.subtract(rect);
	}

	private String getWarningMessage(String string) {
		return NLS.bind(Messages.LayoutSpyDialog_warning_prefix, string);
	}

	private static String printHint(int hint) {
		if (hint == SWT.DEFAULT) {
			return "SWT.DEFAULT"; //$NON-NLS-1$
		}
		return Integer.toString(hint);
	}

	private static String printPoint(Point toPrint) {
		return NLS.bind("({0}, {1})", new Object[] { toPrint.x, toPrint.y }); //$NON-NLS-1$
	}

	private static String printPixels(double pixels) {
		if (pixels == Math.rint(pixels)) {
			return Integer.toString((int) pixels);
		}
		return NLS.bind("{0}", Double.valueOf(Math.round(pixels * 100.0) / 100.0)); //$NON-NLS-1$
	}

	// Control classification ------------------------------------------------

	private static boolean isHorizontallyScrollable(Control child) {
		return (child.getStyle() & SWT.H_SCROLL) != 0;
	}

	private static boolean isVerticallyScrollable(Control child) {
		return (child.getStyle() & SWT.V_SCROLL) != 0;
	}

	/**
	 * Computes the values that should be subtracted off the width and height
	 * hints from computeSize on the given control.
	 */
	private static Point computeHintAdjustment(Control control) {
		int widthAdjustment;
		int heightAdjustment;
		if (control instanceof Scrollable composite) {
			// For composites, subtract off the trim size
			Rectangle trim = composite.computeTrim(0, 0, 0, 0);

			widthAdjustment = trim.width;
			heightAdjustment = trim.height;
		} else {
			// For non-composites, subtract off 2 * the border size
			widthAdjustment = control.getBorderWidth() * 2;
			heightAdjustment = widthAdjustment;
		}

		return new Point(widthAdjustment, heightAdjustment);
	}

	/**
	 * Returns true if the given control is a composite which can expand in the
	 * given dimension. Returns false if the control either cannot expand in the
	 * given dimension or if its growable characteristics cannot be computed in
	 * that dimension.
	 */
	private static boolean isGrowableLayout(Control control, boolean horizontal) {
		if (control instanceof Composite composite) {
			Layout theLayout = composite.getLayout();
			if (theLayout instanceof GridLayout) {
				Control[] children = composite.getChildren();
				for (Control child : children) {
					GridData data = (GridData) child.getLayoutData();

					if (data != null) {
						if (horizontal) {
							if (data.grabExcessHorizontalSpace) {
								return true;
							}
						} else {
							if (data.grabExcessVerticalSpace) {
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}

	/**
	 * Returns true iff another visible widget in the same shell overlaps the
	 * given control.
	 */
	private static boolean overlapsSibling(Control toFind) {
		Composite parent = toFind.getParent();
		Control current = toFind;
		Rectangle displayBounds = GeometryUtil.getDisplayBounds(toFind);

		while (parent != null && !(parent instanceof Shell)) {
			for (Control nextSibling : parent.getChildren()) {
				if (nextSibling == current) {
					continue;
				}
				if (!nextSibling.isVisible()) {
					continue;
				}
				Rectangle nextSiblingBounds = GeometryUtil.getDisplayBounds(nextSibling);
				if (nextSiblingBounds.intersects(displayBounds)) {
					return true;
				}
			}
			current = parent;
			parent = parent.getParent();
		}
		return false;
	}

	/**
	 * Appends the monitor and scaling information for a shell. The shell's
	 * monitor zoom is the authoritative render scale for everything inside it,
	 * so this is shown once per shell rather than repeated on every widget.
	 */
	private void appendMonitorInfo(StringBuilder builder, Control control) {
		Monitor monitor = control.getMonitor();
		Point dpi = control.getDisplay().getDPI();
		builder.append(NLS.bind("monitor zoom = {0}%, display DPI = ({1}, {2})", //$NON-NLS-1$
				new Object[] { monitor.getZoom(), dpi.x, dpi.y }));
		builder.append("\n"); //$NON-NLS-1$
		builder.append(NLS.bind("monitor bounds = {0}", monitor.getBounds())); //$NON-NLS-1$
		builder.append("\n"); //$NON-NLS-1$
	}

	/**
	 * Appends the per-widget HiDPI information: the size in device pixels (using
	 * the shell's monitor zoom as the render scale), the font and the style.
	 * Warns when the size does not map to whole device pixels, or when the
	 * widget sits on a monitor whose zoom differs from its shell.
	 */
	private void appendHiDpiInfo(StringBuilder builder, Control control, Rectangle boundsInPoints) {
		int shellZoom = control.getShell().getMonitor().getZoom();
		double scale = shellZoom / 100.0;
		double widthInPixels = boundsInPoints.width * scale;
		double heightInPixels = boundsInPoints.height * scale;
		builder.append(NLS.bind("device pixels at {0}% = {1} x {2}", //$NON-NLS-1$
				new Object[] { shellZoom, printPixels(widthInPixels), printPixels(heightInPixels) }));
		builder.append("\n"); //$NON-NLS-1$
		if (widthInPixels != Math.rint(widthInPixels) || heightInPixels != Math.rint(heightInPixels)) {
			builder.append(getWarningMessage(Messages.LayoutSpyDialog_warning_non_integer_device_pixels));
		}

		if (control.getMonitor().getZoom() != shellZoom) {
			builder.append(getWarningMessage(Messages.LayoutSpyDialog_warning_monitor_zoom_mismatch));
		}

		Font font = control.getFont();
		if (font != null && !font.isDisposed()) {
			FontData[] fontData = font.getFontData();
			if (fontData.length > 0) {
				FontData first = fontData[0];
				builder.append(NLS.bind("font = {0}, height {1}, style {2}", //$NON-NLS-1$
						new Object[] { first.getName(), first.getHeight(), first.getStyle() }));
				builder.append("\n"); //$NON-NLS-1$
			}
		}

		builder.append(NLS.bind("style = 0x{0}, enabled = {1}", //$NON-NLS-1$
				new Object[] { Integer.toHexString(control.getStyle()), control.isEnabled() }));
		builder.append("\n"); //$NON-NLS-1$
	}

	/**
	 * Computes the string that will be shown in the text box which displays
	 * information about the selected control. This is a tracked getter -- if it
	 * reads from a databinding observable, the text box will automatically
	 * refresh in response to changes in that observable.
	 *
	 * @TrackedGetter
	 */
	private String computeControlInfo() {
		Control child = getSelectedControl();
		if (child == null) {
			return ""; //$NON-NLS-1$
		}
		StringBuilder builder = new StringBuilder();
		builder.append(child.getClass().getName());
		builder.append("\n\n"); //$NON-NLS-1$
		describeControlGeometry(builder, child);
		return builder.toString();
	}

	/**
	 * Appends the geometry, sizing and HiDPI description of the given control,
	 * without the leading class name, so it can be reused both for the
	 * diagnostics panel and for the clipboard export.
	 */
	private void describeControlGeometry(StringBuilder builder, Control child) {
		Object data = child.getData();
		if (data != null) {
			builder.append("getData() == " + data + "\n\n"); //$NON-NLS-1$//$NON-NLS-2$
		}

		int widthHintFromLayoutData = UNKNOWN;
		int heightHintFromLayoutData = UNKNOWN;
		Object layoutData = child.getLayoutData();
		if (layoutData == null) {
			builder.append("getLayoutData() == null\n"); //$NON-NLS-1$
		} else if (layoutData instanceof GridData grid) {
			builder.append(GridDataFactory.createFrom(grid));
			widthHintFromLayoutData = grid.widthHint;
			heightHintFromLayoutData = grid.heightHint;

			if (!grid.grabExcessHorizontalSpace) {
				if (isHorizontallyScrollable(child) || isGrowableLayout(child, true)) {
					builder.append(getWarningMessage(
							Messages.LayoutSpyDialog_warning_grab_horizontally_scrolling));
				}
			}
			if (!grid.grabExcessVerticalSpace) {
				if (isVerticallyScrollable(child) || isGrowableLayout(child, false)) {
					builder.append(getWarningMessage(
							Messages.LayoutSpyDialog_warning_grab_vertical_scrolling));
				}
			}
		} else if (layoutData instanceof FormData formData) {
			widthHintFromLayoutData = formData.width;
			heightHintFromLayoutData = formData.height;
		} else {
			describeObject(builder, "data", layoutData); //$NON-NLS-1$
		}

		if (isHorizontallyScrollable(child)) {
			if (widthHintFromLayoutData == SWT.DEFAULT) {
				builder.append(getWarningMessage(Messages.LayoutSpyDialog_warning_hint_for_horizontally_scrollable));
			}
		}

		if (isVerticallyScrollable(child)) {
			if (heightHintFromLayoutData == SWT.DEFAULT) {
				builder.append(getWarningMessage(Messages.LayoutSpyDialog_warning_hint_for_vertically_scrollable));
			}
		}

		builder.append("\n"); //$NON-NLS-1$

		// Print the current dimensions
		Rectangle bounds = child.getBounds();
		builder.append(NLS.bind("getBounds() = {0}", bounds.toString())); //$NON-NLS-1$
		builder.append("\n"); //$NON-NLS-1$

		if (child instanceof Shell) {
			appendMonitorInfo(builder, child);
		}
		appendHiDpiInfo(builder, child, bounds);

		Point adjustment = computeHintAdjustment(child);

		int widthHint = Math.max(0, bounds.width - adjustment.x);
		int heightHint = Math.max(0, bounds.height - adjustment.y);

		builder.append(NLS.bind("widthAdjustment = {0}, heightAdjustment = {1}", //$NON-NLS-1$
				new Object[] { adjustment.x, adjustment.y }));
		builder.append("\n\n"); //$NON-NLS-1$

		// Print the default size
		Point defaultSize = child.computeSize(SWT.DEFAULT, SWT.DEFAULT, false);
		builder.append(NLS.bind("computeSize(SWT.DEFAULT, SWT.DEFAULT, false) = {0}", printPoint(defaultSize))); //$NON-NLS-1$
		builder.append("\n"); //$NON-NLS-1$

		// Print the preferred horizontally-wrapped size:
		Point hWrappedSize = child.computeSize(widthHint, SWT.DEFAULT, false);
		builder.append(NLS.bind("computeSize({0} - widthAdjustment, SWT.DEFAULT, false) = {1}", //$NON-NLS-1$
				new Object[] { bounds.width, printPoint(hWrappedSize) }));
		builder.append("\n"); //$NON-NLS-1$

		// Print the preferred vertically-wrapped size:
		Point vWrappedSize = child.computeSize(SWT.DEFAULT, heightHint, false);
		builder.append(NLS.bind("computeSize(SWT.DEFAULT, {0} - heightAdjustment, false) = {1}", //$NON-NLS-1$
				new Object[] { bounds.height, printPoint(vWrappedSize) }));
		builder.append("\n"); //$NON-NLS-1$

		// Check for warnings
		Point noOpSize = child.computeSize(widthHint, heightHint, false);
		if (noOpSize.x != bounds.width || noOpSize.y != bounds.height) {
			builder.append(getWarningMessage(NLS.bind(Messages.LayoutSpyDialog_warning_unexpected_compute_size,
					printHint(widthHint), printHint(heightHint), printPoint(noOpSize))));
		}

		if (bounds.height < hWrappedSize.y) {
			builder.append(
					getWarningMessage(Messages.LayoutSpyDialog_warning_shorter_than_preferred_size));
		}

		printReasonControlIsInvisible(builder, child);
	}

	/**
	 * If the control cannot be seen by the user, this method adds a warning
	 * message to the given builder explaining the reason why the control cannot
	 * be seen.
	 */
	private void printReasonControlIsInvisible(StringBuilder builder, Control control) {
		if (!control.isVisible()) {
			builder.append(getWarningMessage("isVisible() == false")); //$NON-NLS-1$
			return;
		}

		Rectangle bounds = control.getBounds();
		if (bounds.isEmpty()) {
			builder.append(getWarningMessage(Messages.LayoutSpyDialog_warning_zero_size));
			return;
		}

		Rectangle displayBounds = GeometryUtil.getDisplayBounds(control);

		Composite parent = control.getParent();
		if (parent != null) {
			Rectangle parentDisplayBounds = GeometryUtil.getDisplayBounds(parent);

			Rectangle intersection = displayBounds.intersection(parentDisplayBounds);
			if (intersection.isEmpty()) {
				builder.append(getWarningMessage(Messages.LayoutSpyDialog_warning_bounds_outside_parent));
				return;
			}

			if (intersection.width < bounds.width || intersection.height < bounds.height) {
				builder.append(getWarningMessage(Messages.LayoutSpyDialog_warning_control_partially_clipped));
				return;
			}

			if (overlapsSibling(control)) {
				builder.append(getWarningMessage(Messages.LayoutSpyDialog_warning_control_overlaps_siblings));
				return;
			}
		}
	}

	/**
	 * Computes the string that will be shown in the text box which displays
	 * information about the selected control's layout. This is a tracked getter:
	 * if it reads from an observable, the text box will update automatically when
	 * the observable changes.
	 *
	 * @TrackedGetter
	 */
	private String computeLayoutInfo() {
		@Nullable
		Control selected = getSelectedControl();

		if (selected == null) {
			return Messages.LayoutSpyDialog_label_no_parent_control_selected;
		}

		StringBuilder builder = new StringBuilder();
		builder.append(selected.getClass().getName());
		builder.append("\n\n"); //$NON-NLS-1$
		describeControlLayout(builder, selected);
		return builder.toString();
	}

	/**
	 * Appends the layout description of the given control, without the leading
	 * class name, so it can be reused both for the layout panel and for the
	 * clipboard export.
	 */
	private void describeControlLayout(StringBuilder builder, Control selected) {
		if (!(selected instanceof Composite parent)) {
			builder.append(Messages.LayoutSpyDialog_label_not_a_composite);
			return;
		}

		Rectangle parentBounds = GeometryUtil.getDisplayBounds(parent);
		Layout layout = parent.getLayout();

		if (layout != null) {
			if (layout instanceof GridLayout grid) {
				builder.append(GridLayoutFactory.createFrom(grid));

				boolean hasVerticallyTruncadeControls = false;
				boolean hasHorizontallyTruncadeControls = false;

				boolean hasHorizontalGrab = false;
				boolean hasVerticalGrab = false;
				for (Control next : parent.getChildren()) {
					@Nullable
					GridData data = (GridData) next.getLayoutData();
					if (data == null) {
						continue;
					}

					Rectangle childBounds = GeometryUtil.getDisplayBounds(parent);
					Rectangle intersection = childBounds.intersection(parentBounds);

					if (intersection.width < childBounds.width) {
						hasHorizontallyTruncadeControls = true;
					}

					if (intersection.height < childBounds.height) {
						hasVerticallyTruncadeControls = true;
					}

					hasHorizontalGrab = hasHorizontalGrab || data.grabExcessHorizontalSpace;
					hasVerticalGrab = hasVerticalGrab || data.grabExcessVerticalSpace;
				}

				if (hasHorizontallyTruncadeControls && !hasHorizontalGrab) {
					builder.append(getWarningMessage(
							Messages.LayoutSpyDialog_warning_not_grabbing_horizontally));
				}

				if (hasVerticallyTruncadeControls && !hasVerticalGrab) {
					builder.append(getWarningMessage(
							Messages.LayoutSpyDialog_warning_not_grabbing_vertically));
				}
			} else {
				describeObject(builder, "layout", layout); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Uses reflection to print the values of the given object's public fields.
	 */
	void describeObject(StringBuilder result, String variableName, Object toDescribe) {
		@SuppressWarnings("rawtypes")
		Class clazz = toDescribe.getClass();
		result.append(clazz.getName());
		result.append(" "); //$NON-NLS-1$
		result.append(variableName);
		result.append(";\n"); //$NON-NLS-1$
		Field[] fields = clazz.getFields();

		for (Field nextField : fields) {
			int modifiers = nextField.getModifiers();
			if (!Modifier.isPublic(modifiers)) {
				continue;
			}
			try {
				String next = variableName + "." + nextField.getName() + " = " + nextField.get(toDescribe) + ";"; //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
				result.append(next);
				result.append("\n"); //$NON-NLS-1$
			} catch (IllegalArgumentException | IllegalAccessException e) {
				// Don't care
			}
		}
	}
}
