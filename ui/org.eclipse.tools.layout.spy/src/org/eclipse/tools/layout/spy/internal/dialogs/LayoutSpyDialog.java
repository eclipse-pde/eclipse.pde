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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
import org.eclipse.swt.custom.StackLayout;
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
import org.eclipse.swt.layout.FillLayout;
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
	/** Address of the node the report starts at. Children append their index. */
	private static final String ROOT_ADDRESS = "@"; //$NON-NLS-1$
	/** Matches the alignment arguments in the factory code JFace renders. */
	private static final Pattern ALIGN_CALL = Pattern.compile("\\.align\\(([^,]+), ([^)]+)\\)"); //$NON-NLS-1$

	/**
	 * A remark about one control. A warning states a problem, a note states that
	 * something that looks like a problem is intended and is not counted.
	 */
	private record Finding(String key, String message, String evidence, boolean warning) {
		String text() {
			return evidence.isEmpty() ? message : message + " (" + evidence + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		}

		String render() {
			return NLS.bind(
					warning ? Messages.LayoutSpyDialog_warning_prefix : Messages.LayoutSpyDialog_note_prefix, text());
		}
	}

	/** Accumulates the widget tree and its findings while the report is built. */
	private static final class Report {
		final StringBuilder tree = new StringBuilder();
		final Map<String, List<Finding>> findingsByAddress = new LinkedHashMap<>();
		int controlCount;
		int maxDepth;
	}
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

	private static class LayoutSpyLabelProvider extends ColumnLabelProvider {
		@Override
		public String getText(Object element) {
			if (element instanceof Control control && control.isDisposed()) {
				return "<disposed>"; //$NON-NLS-1$
			}
			return super.getText(element);
		}

		@Override
		public @Nullable Color getForeground(Object element) {
			if (element instanceof Control control && !control.isDisposed() && !control.isVisible()) {
				return control.getDisplay().getSystemColor(SWT.COLOR_WIDGET_DISABLED_FOREGROUND);
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

		Button showColoringButton = new Button(container, SWT.CHECK);
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
		Report report = new Report();
		appendControlSubtree(selected, report, 0, ROOT_ADDRESS);

		StringBuilder builder = new StringBuilder();
		builder.append("Layout Spy report\n=================\n"); //$NON-NLS-1$
		EnvironmentReport.append(builder, selected.getDisplay());
		builder.append("\n"); //$NON-NLS-1$
		appendSummary(builder, selected, report);
		builder.append("\nWidget tree\n===========\n"); //$NON-NLS-1$
		builder.append(report.tree);

		Clipboard clipboard = new Clipboard(widgetTree.getControl().getDisplay());
		try {
			clipboard.setContents(new Object[] { builder.toString() }, new Transfer[] { TextTransfer.getInstance() });
		} finally {
			clipboard.dispose();
		}
	}

	/**
	 * Appends the counts and the list of warnings, so that a reader does not have
	 * to search a report of several hundred lines for them.
	 */
	private static void appendSummary(StringBuilder builder, Control root, Report report) {
		builder.append("Root: ").append(root.getClass().getName()).append(WidgetIdentity.text(root)); //$NON-NLS-1$
		builder.append(" ").append(ROOT_ADDRESS).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
		builder.append("Controls: ").append(report.controlCount); //$NON-NLS-1$
		builder.append(", maximum depth: ").append(report.maxDepth).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$

		Map<String, Integer> counts = new LinkedHashMap<>();
		Map<String, String> labels = new HashMap<>();
		int total = 0;
		for (List<Finding> findings : report.findingsByAddress.values()) {
			for (Finding finding : findings) {
				if (!finding.warning()) {
					continue;
				}
				total++;
				counts.merge(finding.key(), 1, Integer::sum);
				labels.putIfAbsent(finding.key(), finding.message());
			}
		}

		builder.append("Warnings: ").append(total).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
		counts.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
				.forEach(entry -> builder.append("  ").append(entry.getValue()).append("x ") //$NON-NLS-1$ //$NON-NLS-2$
						.append(labels.get(entry.getKey())).append("\n")); //$NON-NLS-1$

		if (total == 0) {
			return;
		}
		builder.append("Warnings by node:\n"); //$NON-NLS-1$
		report.findingsByAddress.forEach((address, findings) -> {
			for (Finding finding : findings) {
				if (finding.warning()) {
					builder.append("  ").append(address).append("  ").append(finding.text()).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
			}
		});
	}

	/**
	 * Appends the description of the given control, and recursively of its
	 * children, to the report. Every node carries its address so that the warnings
	 * in the summary can point at it.
	 */
	private void appendControlSubtree(Control control, Report report, int depth, String address) {
		StringBuilder builder = report.tree;
		report.controlCount++;
		report.maxDepth = Math.max(report.maxDepth, depth);

		String indent = "  ".repeat(depth); //$NON-NLS-1$
		builder.append(indent).append(address).append("  ").append(control.getClass().getName()); //$NON-NLS-1$
		builder.append(WidgetIdentity.text(control));
		builder.append(NLS.bind(" {0}", control.getBounds())); //$NON-NLS-1$
		builder.append(WidgetIdentity.context(control));
		builder.append("\n"); //$NON-NLS-1$

		List<Finding> findings = new ArrayList<>();
		StringBuilder node = new StringBuilder();
		describeControlGeometry(node, control, findings);
		// Only composites can carry a layout, so the section stays out of the tree for leaf controls
		if (control instanceof Composite) {
			node.append("Layout:\n"); //$NON-NLS-1$
			describeControlLayout(node, control, findings);
		}
		ensureNewline(node);
		for (Finding finding : findings) {
			node.append(finding.render());
		}
		if (!findings.isEmpty()) {
			report.findingsByAddress.put(address, findings);
		}

		for (String line : node.toString().split("\n")) { //$NON-NLS-1$
			if (line.isEmpty()) {
				builder.append("\n"); //$NON-NLS-1$
			} else {
				builder.append(indent).append("  ").append(line).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		builder.append("\n"); //$NON-NLS-1$

		if (control instanceof Composite composite && !composite.isDisposed()) {
			Control[] children = composite.getChildren();
			for (int i = 0; i < children.length; i++) {
				appendControlSubtree(children[i], report, depth + 1, address + "." + i); //$NON-NLS-1$
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

	private static void ensureNewline(StringBuilder builder) {
		if (builder.length() > 0 && builder.charAt(builder.length() - 1) != '\n') {
			builder.append("\n"); //$NON-NLS-1$
		}
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
	 * Returns another visible widget in the same shell that overlaps the given
	 * control, or null if there is none.
	 */
	private static @Nullable Control findOverlappingSibling(Control toFind) {
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
					return nextSibling;
				}
			}
			current = parent;
			parent = parent.getParent();
		}
		return null;
	}

	/**
	 * Appends the font and style of the given control. The font is inherited from
	 * the parent in almost every case, so only a deviation is worth a line.
	 */
	private static void appendWidgetInfo(StringBuilder builder, Control control) {
		FontData font = firstFontData(control);
		Composite parent = control.getParent();
		FontData parentFont = parent == null ? null : firstFontData(parent);
		if (font != null && !font.equals(parentFont)) {
			builder.append(NLS.bind("font = {0}, height {1}, style {2}", //$NON-NLS-1$
					new Object[] { font.getName(), font.getHeight(), fontStyleName(font.getStyle()) }));
			builder.append("\n"); //$NON-NLS-1$
		}

		builder.append("style = ").append(SwtStyles.describe(control)).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
		if (!control.isEnabled()) {
			builder.append("enabled = false\n"); //$NON-NLS-1$
		}
	}

	private static @Nullable FontData firstFontData(Control control) {
		Font font = control.getFont();
		if (font == null || font.isDisposed()) {
			return null;
		}
		FontData[] fontData = font.getFontData();
		return fontData.length > 0 ? fontData[0] : null;
	}

	private static String fontStyleName(int style) {
		if (style == SWT.NORMAL) {
			return "NORMAL"; //$NON-NLS-1$
		}
		StringBuilder builder = new StringBuilder();
		if ((style & SWT.BOLD) != 0) {
			builder.append("BOLD"); //$NON-NLS-1$
		}
		if ((style & SWT.ITALIC) != 0) {
			builder.append(builder.length() == 0 ? "ITALIC" : "|ITALIC"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return builder.length() == 0 ? Integer.toString(style) : builder.toString();
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
		if (child.isDisposed()) {
			return Messages.LayoutSpyDialog_label_control_disposed;
		}
		StringBuilder builder = new StringBuilder();
		builder.append(child.getClass().getName());
		builder.append(WidgetIdentity.text(child));
		builder.append(WidgetIdentity.context(child));
		builder.append("\n\n"); //$NON-NLS-1$
		List<Finding> findings = new ArrayList<>();
		describeControlGeometry(builder, child, findings);
		ensureNewline(builder);
		for (Finding finding : findings) {
			builder.append(finding.render());
		}
		return builder.toString();
	}

	/**
	 * Appends the geometry and sizing description of the given control,
	 * without the leading class name, so it can be reused both for the
	 * diagnostics panel and for the clipboard export.
	 */
	private static void describeControlGeometry(StringBuilder builder, Control child, List<Finding> findings) {
		Object data = child.getData();
		if (data != null) {
			builder.append("getData() == ") //$NON-NLS-1$
					.append(WidgetIdentity.withoutIdentityHashes(String.valueOf(data))).append("\n\n"); //$NON-NLS-1$
		}

		int widthHintFromLayoutData = UNKNOWN;
		int heightHintFromLayoutData = UNKNOWN;
		Object layoutData = child.getLayoutData();
		if (layoutData == null) {
			// Only worth stating when the layout of the parent would actually read it
			if (parentLayoutUsesLayoutData(child)) {
				builder.append("getLayoutData() == null\n"); //$NON-NLS-1$
			}
		} else if (layoutData instanceof GridData grid) {
			appendGridData(builder, grid);
			widthHintFromLayoutData = grid.widthHint;
			heightHintFromLayoutData = grid.heightHint;

			if (!grid.grabExcessHorizontalSpace
					&& (isHorizontallyScrollable(child) || isGrowableLayout(child, true))) {
				findings.add(warning("grab-horizontal", //$NON-NLS-1$
						Messages.LayoutSpyDialog_warning_grab_horizontally_scrolling));
			}
			if (!grid.grabExcessVerticalSpace && (isVerticallyScrollable(child) || isGrowableLayout(child, false))) {
				findings.add(
						warning("grab-vertical", Messages.LayoutSpyDialog_warning_grab_vertical_scrolling)); //$NON-NLS-1$
			}
		} else if (layoutData instanceof FormData formData) {
			widthHintFromLayoutData = formData.width;
			heightHintFromLayoutData = formData.height;
			describeObject(builder, "data", formData, null); //$NON-NLS-1$
		} else {
			describeObject(builder, "data", layoutData, null); //$NON-NLS-1$
		}

		if (isHorizontallyScrollable(child) && widthHintFromLayoutData == SWT.DEFAULT) {
			findings.add(warning("hint-horizontal", //$NON-NLS-1$
					Messages.LayoutSpyDialog_warning_hint_for_horizontally_scrollable));
		}
		if (isVerticallyScrollable(child) && heightHintFromLayoutData == SWT.DEFAULT) {
			findings.add(
					warning("hint-vertical", Messages.LayoutSpyDialog_warning_hint_for_vertically_scrollable)); //$NON-NLS-1$
		}

		builder.append("\n"); //$NON-NLS-1$

		// Print the current dimensions
		Rectangle bounds = child.getBounds();
		builder.append(NLS.bind("getBounds() = {0}", bounds.toString())); //$NON-NLS-1$
		builder.append("\n"); //$NON-NLS-1$

		appendWidgetInfo(builder, child);
		builder.append(WidgetIdentity.itemCount(child));
		WidgetIdentity.appendItems(builder, child);

		Point adjustment = computeHintAdjustment(child);

		int widthHint = Math.max(0, bounds.width - adjustment.x);
		int heightHint = Math.max(0, bounds.height - adjustment.y);

		if (adjustment.x != 0 || adjustment.y != 0) {
			builder.append(NLS.bind("widthAdjustment = {0}, heightAdjustment = {1}", //$NON-NLS-1$
					new Object[] { adjustment.x, adjustment.y }));
			builder.append("\n"); //$NON-NLS-1$
		}
		builder.append("\n"); //$NON-NLS-1$

		Point defaultSize = child.computeSize(SWT.DEFAULT, SWT.DEFAULT, false);
		Point hWrappedSize = child.computeSize(widthHint, SWT.DEFAULT, false);
		Point vWrappedSize = child.computeSize(SWT.DEFAULT, heightHint, false);

		// The comparison of the preferred size against the assigned size is the point of
		// the whole report, so it only collapses when all three agree with the bounds
		if (matchesBounds(defaultSize, bounds) && matchesBounds(hWrappedSize, bounds)
				&& matchesBounds(vWrappedSize, bounds)) {
			builder.append("computeSize == getBounds()\n"); //$NON-NLS-1$
		} else {
			builder.append(NLS.bind("computeSize(SWT.DEFAULT, SWT.DEFAULT, false) = {0}", printPoint(defaultSize))); //$NON-NLS-1$
			builder.append("\n"); //$NON-NLS-1$
			builder.append(NLS.bind("computeSize({0} - widthAdjustment, SWT.DEFAULT, false) = {1}", //$NON-NLS-1$
					new Object[] { bounds.width, printPoint(hWrappedSize) }));
			builder.append("\n"); //$NON-NLS-1$
			builder.append(NLS.bind("computeSize(SWT.DEFAULT, {0} - heightAdjustment, false) = {1}", //$NON-NLS-1$
					new Object[] { bounds.height, printPoint(vWrappedSize) }));
			builder.append("\n"); //$NON-NLS-1$
		}

		// A control the layout deliberately gives no size to fails every size check
		boolean sized = explainHidden(child) == null;

		Point noOpSize = child.computeSize(widthHint, heightHint, false);
		if (sized && (noOpSize.x != bounds.width || noOpSize.y != bounds.height)) {
			findings.add(warning("compute-size", Messages.LayoutSpyDialog_warning_compute_size_not_idempotent, //$NON-NLS-1$
					NLS.bind(Messages.LayoutSpyDialog_warning_unexpected_compute_size, printHint(widthHint),
							printHint(heightHint), printPoint(noOpSize))));
		}

		// A scrollable control is smaller than its content by definition
		if (sized && bounds.height < hWrappedSize.y && !isVerticallyScrollable(child)) {
			findings.add(warning("shorter-than-preferred", //$NON-NLS-1$
					Messages.LayoutSpyDialog_warning_shorter_than_preferred_size));
		}

		collectVisibilityFindings(child, findings);
	}

	private static boolean matchesBounds(Point size, Rectangle bounds) {
		return size.x == bounds.width && size.y == bounds.height;
	}

	/**
	 * Returns whether the layout of the parent reads the layout data of its
	 * children. A {@link FillLayout} or {@link StackLayout} ignores it, so a null
	 * layout data says nothing there.
	 */
	private static boolean parentLayoutUsesLayoutData(Control control) {
		Composite parent = control.getParent();
		if (parent == null) {
			return false;
		}
		Layout layout = parent.getLayout();
		return layout != null && !(layout instanceof FillLayout) && !(layout instanceof StackLayout);
	}

	/**
	 * Appends the grid data as JFace factory code, with the alignments that JFace
	 * cannot name printed as constants and a minimum size of zero, which is no
	 * minimum at all, left out.
	 */
	private static void appendGridData(StringBuilder builder, GridData grid) {
		String rendered = GridDataFactory.createFrom(grid).toString();
		rendered = ALIGN_CALL.matcher(rendered)
				.replaceAll(match -> ".align(" + alignmentName(match.group(1)) + ", " //$NON-NLS-1$ //$NON-NLS-2$
						+ alignmentName(match.group(2)) + ")"); //$NON-NLS-1$
		rendered = rendered.replace("    .minSize(0, 0)\n", ""); //$NON-NLS-1$ //$NON-NLS-2$
		builder.append(rendered);
	}

	private static String alignmentName(String rendered) {
		try {
			return SwtStyles.describeAlignment(Integer.parseInt(rendered.trim()));
		} catch (NumberFormatException e) {
			// JFace already named it
			return rendered;
		}
	}

	/**
	 * Collects the reasons why the control cannot be seen by the user. All
	 * applicable reasons are reported, a control can be both clipped and
	 * overlapped.
	 */
	private static void collectVisibilityFindings(Control control, List<Finding> findings) {
		Composite parent = control.getParent();
		if (!control.isVisible()) {
			// isVisible() is transitive, so a single hidden ancestor would otherwise
			// stamp this warning onto every one of its descendants
			if (parent != null && !parent.isVisible()) {
				return;
			}
			String intended = explainHidden(control);
			if (intended == null) {
				findings.add(warning("not-visible", "isVisible() == false")); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				findings.add(note("hidden-by-design", Messages.LayoutSpyDialog_note_hidden_by_design, intended)); //$NON-NLS-1$
			}
			return;
		}

		Rectangle bounds = control.getBounds();
		if (bounds.isEmpty()) {
			String intended = explainHidden(control);
			if (intended == null) {
				findings.add(warning("zero-size", Messages.LayoutSpyDialog_warning_zero_size)); //$NON-NLS-1$
			} else {
				findings.add(note("zero-size-by-design", Messages.LayoutSpyDialog_note_zero_size_by_design, intended)); //$NON-NLS-1$
			}
			return;
		}

		if (parent == null) {
			return;
		}
		Rectangle displayBounds = GeometryUtil.getDisplayBounds(control);
		Rectangle parentDisplayBounds = GeometryUtil.getDisplayBounds(parent);
		Rectangle intersection = displayBounds.intersection(parentDisplayBounds);
		if (intersection.isEmpty()) {
			findings.add(warning("outside-parent", Messages.LayoutSpyDialog_warning_bounds_outside_parent, //$NON-NLS-1$
					NLS.bind("control at {0}, parent at {1}", displayBounds, parentDisplayBounds))); //$NON-NLS-1$
		} else if (intersection.width < bounds.width || intersection.height < bounds.height) {
			findings.add(warning("clipped", Messages.LayoutSpyDialog_warning_control_partially_clipped, //$NON-NLS-1$
					NLS.bind("{0} of {1}x{2} visible", //$NON-NLS-1$
							new Object[] { intersection.width + "x" + intersection.height, bounds.width, //$NON-NLS-1$
									bounds.height })));
		}

		Control sibling = findOverlappingSibling(control);
		if (sibling != null) {
			Rectangle overlap = displayBounds.intersection(GeometryUtil.getDisplayBounds(sibling));
			findings.add(warning("overlaps-sibling", Messages.LayoutSpyDialog_warning_control_overlaps_siblings, //$NON-NLS-1$
					NLS.bind("{0}{1} at {2}", new Object[] { sibling.getClass().getSimpleName(), //$NON-NLS-1$
							WidgetIdentity.text(sibling), overlap })));
		}
	}

	/**
	 * Returns why the control is hidden or has no size on purpose, or null when
	 * nothing explains it.
	 */
	private static @Nullable String explainHidden(Control control) {
		if (control.getLayoutData() instanceof GridData data && data.exclude) {
			return "GridData.exclude == true"; //$NON-NLS-1$
		}
		Composite parent = control.getParent();
		if (parent != null && parent.getLayout() instanceof StackLayout stack && stack.topControl != control) {
			return "not the topControl of the parent StackLayout"; //$NON-NLS-1$
		}
		return null;
	}

	private static Finding warning(String key, String message) {
		return new Finding(key, message, "", true); //$NON-NLS-1$
	}

	private static Finding warning(String key, String message, String evidence) {
		return new Finding(key, message, evidence, true);
	}

	private static Finding note(String key, String message, String evidence) {
		return new Finding(key, message, evidence, false);
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
			return Messages.LayoutSpyDialog_label_no_control_selected;
		}
		if (selected.isDisposed()) {
			return Messages.LayoutSpyDialog_label_control_disposed;
		}

		StringBuilder builder = new StringBuilder();
		builder.append(selected.getClass().getName());
		builder.append("\n\n"); //$NON-NLS-1$
		List<Finding> findings = new ArrayList<>();
		describeControlLayout(builder, selected, findings);
		ensureNewline(builder);
		for (Finding finding : findings) {
			builder.append(finding.render());
		}
		return builder.toString();
	}

	/**
	 * Appends the layout description of the given control, without the leading
	 * class name, so it can be reused both for the layout panel and for the
	 * clipboard export.
	 */
	private static void describeControlLayout(StringBuilder builder, Control selected, List<Finding> findings) {
		if (!(selected instanceof Composite parent)) {
			builder.append(Messages.LayoutSpyDialog_label_not_a_composite);
			return;
		}

		Layout layout = parent.getLayout();
		if (layout == null) {
			builder.append(Messages.LayoutSpyDialog_label_no_layout);
		} else if (layout instanceof GridLayout grid) {
			describeGridLayout(builder, parent, grid, findings);
		} else {
			describeObject(builder, "layout", layout, parent); //$NON-NLS-1$
		}
		ensureNewline(builder);
		appendContentFit(builder, parent, findings);
	}

	/**
	 * Compares the room the children ask for with the room they are given. A layout
	 * error shows up here before it shows up on any single control, and it says
	 * which composite to look at rather than which widget came out wrong.
	 */
	private static void appendContentFit(StringBuilder builder, Composite parent, List<Finding> findings) {
		Rectangle content = null;
		int narrowChildren = 0;
		int shortChildren = 0;
		int widthDeficit = 0;
		int heightDeficit = 0;

		for (Control child : parent.getChildren()) {
			// A child the layout deliberately leaves out does not compete for the space
			if (!child.isVisible() || explainHidden(child) != null) {
				continue;
			}
			Rectangle bounds = child.getBounds();
			content = content == null ? bounds : content.union(bounds);
			Point preferred = child.computeSize(SWT.DEFAULT, SWT.DEFAULT, false);
			if (preferred.x > bounds.width) {
				narrowChildren++;
				widthDeficit += preferred.x - bounds.width;
			}
			if (preferred.y > bounds.height) {
				shortChildren++;
				heightDeficit += preferred.y - bounds.height;
			}
		}
		if (content == null) {
			return;
		}

		Rectangle client = parent.getClientArea();
		Rectangle visible = content.intersection(client);
		boolean overflows = visible.width < content.width || visible.height < content.height;
		if (!overflows && narrowChildren == 0 && shortChildren == 0) {
			return;
		}

		builder.append(NLS.bind("content = {0} in client area {1}", content, client)); //$NON-NLS-1$
		builder.append("\n"); //$NON-NLS-1$
		if (narrowChildren > 0 || shortChildren > 0) {
			builder.append(NLS.bind("children below their preferred size: {0} narrower by {1}, {2} shorter by {3}", //$NON-NLS-1$
					new Object[] { narrowChildren, widthDeficit, shortChildren, heightDeficit }));
			builder.append("\n"); //$NON-NLS-1$
		}

		// Content larger than the client area is what a scrollable composite is for
		if (overflows && !isHorizontallyScrollable(parent) && !isVerticallyScrollable(parent)) {
			findings.add(warning("content-overflow", Messages.LayoutSpyDialog_warning_content_does_not_fit, //$NON-NLS-1$
					NLS.bind("content {0}, client area {1}", content, client))); //$NON-NLS-1$
		}
	}

	private static void describeGridLayout(StringBuilder builder, Composite parent, GridLayout grid,
			List<Finding> findings) {
		builder.append(GridLayoutFactory.createFrom(grid));

		Rectangle parentBounds = GeometryUtil.getDisplayBounds(parent);
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

			Rectangle childBounds = GeometryUtil.getDisplayBounds(next);
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
			findings.add(warning("no-horizontal-grab", //$NON-NLS-1$
					Messages.LayoutSpyDialog_warning_not_grabbing_horizontally));
		}

		if (hasVerticallyTruncadeControls && !hasVerticalGrab) {
			findings.add(warning("no-vertical-grab", Messages.LayoutSpyDialog_warning_not_grabbing_vertically)); //$NON-NLS-1$
		}
	}

	/**
	 * Uses reflection to print the values of the given object's public instance
	 * fields. The owner, when given, is the composite whose layout is described and
	 * lets references to its children be printed as a child index.
	 */
	static void describeObject(StringBuilder result, String variableName, Object toDescribe, @Nullable Composite owner) {
		Class<?> clazz = toDescribe.getClass();
		result.append(clazz.getName());
		result.append(" "); //$NON-NLS-1$
		result.append(variableName);
		result.append(";\n"); //$NON-NLS-1$

		for (Field nextField : clazz.getFields()) {
			int modifiers = nextField.getModifiers();
			// Static fields are the constants of the class, not the state of this instance
			if (!Modifier.isPublic(modifiers) || Modifier.isStatic(modifiers)) {
				continue;
			}
			try {
				result.append(variableName).append(".").append(nextField.getName()).append(" = "); //$NON-NLS-1$ //$NON-NLS-2$
				result.append(describeFieldValue(nextField.getName(), nextField.get(toDescribe), owner));
				result.append(";\n"); //$NON-NLS-1$
			} catch (IllegalArgumentException | IllegalAccessException e) {
				// Don't care
			}
		}
	}

	private static String describeFieldValue(String fieldName, @Nullable Object value, @Nullable Composite owner) {
		if (value instanceof Control control) {
			return describeControlReference(control, owner);
		}
		if (value instanceof Integer orientation && "type".equals(fieldName)) { //$NON-NLS-1$
			return SwtStyles.describeOrientation(orientation);
		}
		return WidgetIdentity.withoutIdentityHashes(String.valueOf(value));
	}

	/**
	 * Describes a reference to another control. A bare class name is ambiguous when
	 * a composite has several children of the same type, so the child index is used
	 * where it is known.
	 */
	private static String describeControlReference(Control control, @Nullable Composite owner) {
		String name = control.getClass().getSimpleName() + WidgetIdentity.text(control);
		if (owner == null) {
			return name;
		}
		Control[] children = owner.getChildren();
		for (int i = 0; i < children.length; i++) {
			if (children[i] == control) {
				return "child[" + i + "] " + name; //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return name;
	}
}
