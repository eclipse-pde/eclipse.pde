/*******************************************************************************
 * Copyright (c) 2011, 2021 Manumitting Technologies, Inc.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Brian de Alwis (MT) - initial API and implementation
 *     Olivier Prouvost <olivier.prouvost@opcoach.com> - Bug 428903
 *******************************************************************************/
package org.eclipse.pde.spy.css;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.css.core.dom.CSSStylableElement;
import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.css.swt.dom.WidgetElement;
import org.eclipse.e4.ui.css.swt.engine.CSSSWTEngineImpl;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.FocusCellOwnerDrawHighlighter;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TableViewerEditor;
import org.eclipse.jface.viewers.TableViewerFocusCellManager;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.Widget;
import org.w3c.css.sac.CSSParseException;
import org.w3c.css.sac.SelectorList;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.css.CSSRule;
import org.w3c.dom.css.CSSRuleList;
import org.w3c.dom.css.CSSStyleDeclaration;
import org.w3c.dom.css.CSSStyleRule;
import org.w3c.dom.css.CSSStyleSheet;
import org.w3c.dom.css.CSSValue;
import org.w3c.dom.css.DocumentCSS;
import org.w3c.dom.stylesheets.StyleSheet;
import org.w3c.dom.stylesheets.StyleSheetList;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@SuppressWarnings("restriction")
public class CssSpyPart {

	// Per-widget-type sets of meaningful SWT style bit names for CSS debugging.
	// Each set includes the universal bits (BORDER, LEFT_TO_RIGHT, RIGHT_TO_LEFT).
	private static final Set<String> STYLE_BITS_SHELL = Set.of(
			"SWT.BORDER", "SWT.LEFT_TO_RIGHT", "SWT.RIGHT_TO_LEFT", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			"SWT.NO_TRIM", "SWT.RESIZE", "SWT.TITLE", "SWT.CLOSE", "SWT.MIN", "SWT.MAX", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
			"SWT.ON_TOP", "SWT.TOOL", "SWT.SHEET", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			"SWT.MODELESS", "SWT.PRIMARY_MODAL", "SWT.APPLICATION_MODAL", "SWT.SYSTEM_MODAL"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

	private static final Set<String> STYLE_BITS_TOOLBAR = Set.of(
			"SWT.BORDER", "SWT.LEFT_TO_RIGHT", "SWT.RIGHT_TO_LEFT", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			"SWT.FLAT", "SWT.WRAP", "SWT.RIGHT", "SWT.HORIZONTAL", "SWT.VERTICAL", "SWT.SHADOW_OUT"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

	private static final Set<String> STYLE_BITS_BUTTON = Set.of(
			"SWT.BORDER", "SWT.LEFT_TO_RIGHT", "SWT.RIGHT_TO_LEFT", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			"SWT.PUSH", "SWT.CHECK", "SWT.RADIO", "SWT.TOGGLE", "SWT.ARROW", "SWT.FLAT", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
			"SWT.LEFT", "SWT.RIGHT", "SWT.CENTER", "SWT.UP", "SWT.DOWN"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

	private static final Set<String> STYLE_BITS_TEXT = Set.of(
			"SWT.BORDER", "SWT.LEFT_TO_RIGHT", "SWT.RIGHT_TO_LEFT", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			"SWT.SINGLE", "SWT.MULTI", "SWT.READ_ONLY", "SWT.WRAP", "SWT.PASSWORD", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			"SWT.SEARCH", "SWT.ICON_SEARCH", "SWT.ICON_CANCEL"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

	private static final Set<String> STYLE_BITS_CTABFOLDER = Set.of(
			"SWT.BORDER", "SWT.LEFT_TO_RIGHT", "SWT.RIGHT_TO_LEFT", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			"SWT.TOP", "SWT.BOTTOM", "SWT.FLAT", "SWT.SINGLE", "SWT.MULTI", "SWT.CLOSE"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

	private static final Set<String> STYLE_BITS_COMBO = Set.of(
			"SWT.BORDER", "SWT.LEFT_TO_RIGHT", "SWT.RIGHT_TO_LEFT", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			"SWT.SIMPLE", "SWT.DROP_DOWN", "SWT.READ_ONLY"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

	private static final Set<String> STYLE_BITS_LABEL = Set.of(
			"SWT.BORDER", "SWT.LEFT_TO_RIGHT", "SWT.RIGHT_TO_LEFT", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			"SWT.LEFT", "SWT.RIGHT", "SWT.CENTER", "SWT.WRAP", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			"SWT.SEPARATOR", "SWT.HORIZONTAL", "SWT.VERTICAL", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			"SWT.SHADOW_IN", "SWT.SHADOW_OUT", "SWT.SHADOW_NONE"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

	private static final Set<String> STYLE_BITS_TABLE_TREE = Set.of(
			"SWT.BORDER", "SWT.LEFT_TO_RIGHT", "SWT.RIGHT_TO_LEFT", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			"SWT.SINGLE", "SWT.MULTI", "SWT.CHECK", "SWT.FULL_SELECTION", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			"SWT.VIRTUAL", "SWT.HIDE_SELECTION", "SWT.H_SCROLL", "SWT.V_SCROLL"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

	/** Fallback for Composite subclasses not matched above. */
	private static final Set<String> STYLE_BITS_COMPOSITE = Set.of(
			"SWT.BORDER", "SWT.LEFT_TO_RIGHT", "SWT.RIGHT_TO_LEFT", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			"SWT.H_SCROLL", "SWT.V_SCROLL", "SWT.NO_SCROLL", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			"SWT.NO_FOCUS", "SWT.NO_BACKGROUND", "SWT.DOUBLE_BUFFERED"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

	/** Fallback for non-Composite widgets (ToolItem, CTabItem, etc.). */
	private static final Set<String> STYLE_BITS_GENERIC = Set.of(
			"SWT.BORDER", "SWT.LEFT_TO_RIGHT", "SWT.RIGHT_TO_LEFT"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

	private static Set<String> getRelevantStyleBits(Widget widget) {
		// Order matters: more specific types before their supertypes
		if (widget instanceof Shell) return STYLE_BITS_SHELL;
		if (widget instanceof ToolBar) return STYLE_BITS_TOOLBAR;
		if (widget instanceof Button) return STYLE_BITS_BUTTON;
		if (widget instanceof Text) return STYLE_BITS_TEXT;
		if (widget instanceof CTabFolder) return STYLE_BITS_CTABFOLDER;
		if (widget instanceof Combo) return STYLE_BITS_COMBO;
		if (widget instanceof Label) return STYLE_BITS_LABEL;
		if (widget instanceof Tree || widget instanceof Table) return STYLE_BITS_TABLE_TREE;
		if (widget instanceof Composite) return STYLE_BITS_COMPOSITE;
		return STYLE_BITS_GENERIC;
	}

	private static final Pattern RGB_PATTERN = Pattern
			.compile("rgb\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\)"); //$NON-NLS-1$

	private static final Pattern HEX_COLOR_PATTERN = Pattern
			.compile("^#([0-9a-fA-F]{2})([0-9a-fA-F]{2})([0-9a-fA-F]{2})$"); //$NON-NLS-1$

	@Inject
	@Named(IServiceConstants.ACTIVE_SHELL)
	private Shell activeShell;

	/**
	 * @return the CSS element corresponding to the argument, or null if none
	 */
	public static CSSStylableElement getCSSElement(Object o) {
		if (o instanceof CSSStylableElement) {
			return (CSSStylableElement) o;
		} else {
			CSSEngine engine = getCSSEngine(o);
			if (engine != null) {
				return (CSSStylableElement) engine.getElement(o);
			}
		}
		return null;
	}

	/** @return the CSS engine governing the argument, or null if none */
	public static CSSEngine getCSSEngine(Object o) {
		CSSEngine engine = null;
		if (o instanceof CSSStylableElement element) {
			engine = WidgetElement.getEngine((Widget) element.getNativeWidget());
		}
		if (engine == null && o instanceof Widget) {
			if (((Widget) o).isDisposed()) {
				return null;
			}
			engine = WidgetElement.getEngine((Widget) o);
		}
		if (engine == null && Display.getCurrent() != null) {
			engine = new CSSSWTEngineImpl(Display.getCurrent());
		}
		return engine;
	}

	@Inject
	private Display display;

	private Widget specimen; // specimen (can be reused if reopened)

	@Inject
	IEclipseContext mpartContext; // Used to remember of last specimen.

	private Widget shown;

	private TreeViewer widgetTreeViewer;
	private WidgetTreeProvider widgetTreeProvider;
	private Button followSelection;
	private Button showAllShells;
	private TableViewer cssPropertiesViewer;
	private Text cssRules;

	private final List<Shell> highlights = new LinkedList<>();
	private final List<Region> highlightRegions = new LinkedList<>();
	private Text cssSearchBox;
	private Button showUnsetProperties;
	private Button showCssFragment;
	private Button copyHierarchyToClipboard;

	protected ViewerFilter unsetPropertyFilter = new ViewerFilter() {

		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (element instanceof CSSPropertyProvider) {
				try {
					return ((CSSPropertyProvider) element).getValue() != null;
				} catch (Exception e) {
					return false;
				}
			}
			return false;
		}
	};

	private Composite outer;

	public Widget getSpecimen() {
		return specimen;
	}

	private boolean isLive() {
		return specimen == null;
	}

	public void setSpecimen(Widget specimen) {
		this.specimen = specimen;
		update();
	}

	private Widget getActiveSpecimen() {
		if (specimen != null) {
			return specimen;
		}
		return display.getCursorControl();
	}

	protected void update() {
		if (activeShell == null) {
			return;
		}
		Widget current = getActiveSpecimen();
		if (shown == current) {
			return;
		}
		shown = current;

		CSSEngine engine = getCSSEngine(shown);
		CSSStylableElement element = (CSSStylableElement) engine.getElement(shown);
		if (element == null) {
			return;
		}

		updateWidgetTreeInput();
		revealAndSelect(Collections.singletonList(shown));
	}

	private <T> void revealAndSelect(List<T> elements) {
		widgetTreeViewer.setSelection(new StructuredSelection(elements), true);
	}

	private void updateForWidgetSelection(ISelection sel) {
		disposeHighlights();
		if (sel.isEmpty()) {
			return;
		}
		StructuredSelection selection = (StructuredSelection) sel;
		for (Object s : selection.toList()) {
			if (s instanceof Widget) {
				highlightWidget((Widget) s);
			}
		}
		populate(selection.size() == 1 && selection.getFirstElement() instanceof Widget
				? (Widget) selection.getFirstElement() : null);
	}

	private void updateWidgetTreeInput() {
		if (showAllShells.getSelection()) {
			widgetTreeViewer.setInput(display);
		} else {
			widgetTreeViewer.setInput(new Object[] { shown instanceof Control c ? c.getShell() : shown });
		}
		performCSSSearch(new NullProgressMonitor());
	}

	protected void populate(Widget selected) {
		if (selected == null) {
			cssPropertiesViewer.setInput(null);
			cssRules.setText(""); //$NON-NLS-1$
			return;
		}
		if (selected.isDisposed()) {
			cssPropertiesViewer.setInput(null);
			cssRules.setText(Messages.CssSpyPart_DISPOSED);
			return;
		}

		CSSStylableElement element = getCSSElement(selected);
		if (element == null) {
			cssPropertiesViewer.setInput(null);
			cssRules.setText(Messages.CssSpyPart_Not_a_stylable_element);
			return;
		}

		cssPropertiesViewer.setInput(selected);

		StringBuilder sb = new StringBuilder();
		CSSEngine engine = getCSSEngine(element);
		CSSStyleDeclaration decl = engine.getViewCSS().getComputedStyle(element, null);

		if (element.getCSSStyle() != null) {
			sb.append(MessageFormat.format("\n{0}\n  ", Messages.CssSpyPart_CSS_Inline_Styles)); //$NON-NLS-1$
			Util.join(sb, element.getCSSStyle().split(";"), ";\n  "); //$NON-NLS-1$ //$NON-NLS-2$
		}

		if (decl != null) {
			sb.append(MessageFormat.format("\n\n{0}\n", Messages.CssSpyPart_CSS_Properties_)); //$NON-NLS-1$
			try {
				if (decl != null) {
					sb.append(decl.getCssText());
				}
			} catch (Exception e) {
				sb.append(e);
			}
		}
		if (element.getStaticPseudoInstances().length > 0) {
			sb.append(MessageFormat.format("\n\n{0}\n  ", Messages.CssSpyPart_Static_Pseudoinstances)); //$NON-NLS-1$
			Util.join(sb, element.getStaticPseudoInstances(), "\n  "); //$NON-NLS-1$
		}

		if (element.getCSSClass() != null) {
			sb.append(MessageFormat.format("\n\n{0}\n  ", Messages.CssSpyPart_CSS_Classes)); //$NON-NLS-1$
			Util.join(sb, element.getCSSClass().split(" +"), "\n  "); //$NON-NLS-1$ //$NON-NLS-2$
		}

		if (element.getCSSId() != null) {
			sb.append(MessageFormat.format("\n\n{0}\n  ", Messages.CssSpyPart_CSS_ID_)); //$NON-NLS-1$
			Util.join(sb, element.getCSSId().split(" +"), "\n  "); //$NON-NLS-1$ //$NON-NLS-2$
		}

		if (element.getAttribute("style") != null) { //$NON-NLS-1$
			sb.append(MessageFormat.format("\n\n{0}\n  ", Messages.CssSpyPart_SWT_Style_Bits)); //$NON-NLS-1$
			Util.join(sb, element.getAttribute("style").split(" +"), "\n  "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		sb.append(MessageFormat.format("\n\n{0}\n  ", Messages.CssSpyPart_CSS_Class_Element)).append(element.getClass().getName()); //$NON-NLS-1$

		// this is useful for diagnosing issues
		if (element.getNativeWidget() instanceof Shell && ((Shell) element.getNativeWidget()).getParent() != null) {
			Shell nw = (Shell) element.getNativeWidget();
			sb.append(MessageFormat.format("\n\n{0} ", Messages.CssSpyPart_Shell_parent)).append(nw.getParent()); //$NON-NLS-1$
		}
		if (element.getNativeWidget() instanceof Composite) {
			Composite nw = (Composite) element.getNativeWidget();
			sb.append(MessageFormat.format("\n\n{0} ", Messages.CssSpyPart_SWT_Layout)).append(nw.getLayout()); //$NON-NLS-1$
		}
		Rectangle bounds = getBounds(selected);
		if (bounds != null) {
			sb.append(MessageFormat.format("\n{0} ", Messages.CssSpyPart_Bounds)).append("x=").append(bounds.x).append(" y=").append(bounds.y); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			sb.append(" h=").append(bounds.height).append(" w=").append(bounds.width); //$NON-NLS-1$ //$NON-NLS-2$
		}

		if (element.getNativeWidget() instanceof Widget) {
			Widget w = (Widget) element.getNativeWidget();
			if (w.getData() != null) {
				sb.append(MessageFormat.format("\n{0} ", Messages.CssSpyPart_Widget_data)).append(w.getData()); //$NON-NLS-1$
			}
			if (w.getData(SWT.SKIN_ID) != null) {
				sb.append(MessageFormat.format("\n{0} ", MessageFormat.format(Messages.CssSpyPart_Widget_Skin_ID, SWT.SKIN_ID))).append(w.getData(SWT.SKIN_ID)); //$NON-NLS-1$
			}
			if (w.getData(SWT.SKIN_CLASS) != null) {
				sb.append(MessageFormat.format("\n{0} ", MessageFormat.format(Messages.CssSpyPart_Widget_Skin_Class, SWT.SKIN_CLASS))).append(w.getData(SWT.SKIN_CLASS)); //$NON-NLS-1$
			}
		}

		cssRules.setText(sb.toString().trim());

		disposeHighlights();
		highlightWidget(selected);
	}

	private Shell getShell(Widget widget) {
		if (widget instanceof Control) {
			return ((Control) widget).getShell();
		}
		return null;
	}

	/** Add a highlight-rectangle for the selected widget */
	private void highlightWidget(Widget selected) {
		if (selected == null || selected.isDisposed()) {
			return;
		}

		Rectangle bounds = getBounds(selected); // relative to absolute display,
												// not the widget
		if (bounds == null /* || bounds.height == 0 || bounds.width == 0 */) {
			return;
		}
		// emulate a transparent background as per SWT Snippet180
		Shell selectedShell = getShell(selected);
		// create the highlight; want it to appear on top
		Shell highlight = new Shell(selectedShell, SWT.NO_TRIM | SWT.MODELESS | SWT.NO_FOCUS | SWT.ON_TOP);
		highlight.setBackground(display.getSystemColor(SWT.COLOR_RED));
		// set CSS ID for the dark theme
		highlight.setData("org.eclipse.e4.ui.css.id", "css-spy"); //$NON-NLS-1$ //$NON-NLS-2$
		Region highlightRegion = new Region();
		highlightRegion.add(0, 0, 1, bounds.height + 2);
		highlightRegion.add(0, 0, bounds.width + 2, 1);
		highlightRegion.add(bounds.width + 1, 0, 1, bounds.height + 2);
		highlightRegion.add(0, bounds.height + 1, bounds.width + 2, 1);
		highlight.setRegion(highlightRegion);
		highlight.setBounds(bounds.x - 1, bounds.y - 1, bounds.width + 2, bounds.height + 2);
		highlight.setEnabled(false);
		highlight.setVisible(true); // not open(): setVisible() prevents taking
									// focus

		highlights.add(highlight);
		highlightRegions.add(highlightRegion);
	}

	private void disposeHighlights() {
		for (Shell highlight : highlights) {
			highlight.dispose();
		}
		highlights.clear();
		for (Region region : highlightRegions) {
			region.dispose();
		}
		highlightRegions.clear();
	}

	private Rectangle getBounds(Widget widget) {
		if (widget instanceof Shell) {
			// Shell bounds are already in display coordinates
			return ((Shell) widget).getBounds();
		} else if (widget instanceof Control control) {
			Rectangle bounds = control.getBounds();
			return control.getDisplay().map(control.getParent(), null, bounds);
		} else if (widget instanceof ToolItem item) {
			Rectangle bounds = item.getBounds();
			return item.getDisplay().map(item.getParent(), null, bounds);
		} else if (widget instanceof CTabItem item) {
			Rectangle bounds = item.getBounds();
			return item.getDisplay().map(item.getParent(), null, bounds);
		}
		// FIXME: figure out how to map items to a position
		return null;
	}

	/**
	 * Create contents of the spy.
	 */
	@PostConstruct
	protected Control createDialogArea(Composite parent, IEclipseContext ctx) {
		outer = parent;
		outer.setLayout(new GridLayout());
		outer.setLayoutData(new GridData(GridData.FILL_BOTH));

		Composite top = new Composite(outer, SWT.NONE);
		GridLayoutFactory.swtDefaults().numColumns(3).applyTo(top);
		cssSearchBox = new Text(top, SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL);
		cssSearchBox.setMessage(Messages.CssSpyPart_CSS_Selector);
		cssSearchBox.setToolTipText(Messages.CssSpyPart_Highlight_matching_widgets);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(cssSearchBox);

		followSelection = new Button(top, SWT.CHECK);
		followSelection.setSelection(true);
		followSelection.setText(Messages.CssSpyPart_Follow_UI_Selection);
		GridDataFactory.swtDefaults().applyTo(followSelection);

		showAllShells = new Button(top, SWT.CHECK);
		showAllShells.setText(Messages.CssSpyPart_All_shells);
		GridDataFactory.swtDefaults().applyTo(showAllShells);

		GridDataFactory.fillDefaults().applyTo(top);

		SashForm sashForm = new SashForm(outer, SWT.VERTICAL);
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		// / THE WIDGET TREE
		Composite widgetsComposite = new Composite(sashForm, SWT.NONE);

		widgetTreeViewer = new TreeViewer(widgetsComposite, SWT.BORDER | SWT.MULTI);
		widgetTreeProvider = new WidgetTreeProvider();
		widgetTreeViewer.setContentProvider(widgetTreeProvider);
		widgetTreeViewer.setAutoExpandLevel(0);
		widgetTreeViewer.getTree().setLinesVisible(true);
		widgetTreeViewer.getTree().setHeaderVisible(true);
		ColumnViewerToolTipSupport.enableFor(widgetTreeViewer);

		TreeViewerColumn widgetTypeColumn = new TreeViewerColumn(widgetTreeViewer, SWT.NONE);
		widgetTypeColumn.getColumn().setWidth(100);
		widgetTypeColumn.getColumn().setText(Messages.CssSpyPart_Widget);
		widgetTypeColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object item) {
				CSSStylableElement element = CssSpyPart.getCSSElement(item);
				return element.getLocalName() + " " + MessageFormat.format(Messages.CssSpyPart_NamespaceURI, element.getNamespaceURI()); //$NON-NLS-1$
			}
		});

		TreeViewerColumn widgetClassColumn = new TreeViewerColumn(widgetTreeViewer, SWT.NONE);
		widgetClassColumn.getColumn().setText(Messages.CssSpyPart_CSS_Class);
		widgetClassColumn.getColumn().setWidth(100);
		widgetClassColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object item) {
				CSSStylableElement element = CssSpyPart.getCSSElement(item);
				if (element.getCSSClass() == null) {
					return null;
				}
				String classes[] = element.getCSSClass().split(" +"); //$NON-NLS-1$
				return classes.length <= 1 ? classes[0] : classes[0] + " " + MessageFormat.format(Messages.CssSpyPart_plus_others, (classes.length - 1)); //$NON-NLS-1$
			}

			@Override
			public String getToolTipText(Object item) {
				CSSStylableElement element = CssSpyPart.getCSSElement(item);
				if (element == null) {
					return null;
				}
				StringBuilder sb = new StringBuilder();
				sb.append(element.getLocalName()).append(" ").append(MessageFormat.format(Messages.CssSpyPart_NamespaceURI, element.getNamespaceURI())); //$NON-NLS-1$
				if (element.getCSSClass() != null) {
					sb.append(MessageFormat.format("\n{0}\n  ", Messages.CssSpyPart_Classes)); //$NON-NLS-1$
					Util.join(sb, element.getCSSClass().split(" +"), "\n  "); //$NON-NLS-1$ //$NON-NLS-2$
				}
				return sb.toString();
			}
		});

		TreeViewerColumn widgetIdColumn = new TreeViewerColumn(widgetTreeViewer, SWT.NONE);
		widgetIdColumn.getColumn().setWidth(100);
		widgetIdColumn.getColumn().setText(Messages.CssSpyPart_CSS_ID);
		widgetIdColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object item) {
				CSSStylableElement element = CssSpyPart.getCSSElement(item);
				return element.getCSSId();
			}
		});

		TreeColumnLayout widgetsTableLayout = new TreeColumnLayout();
		widgetsTableLayout.setColumnData(widgetTypeColumn.getColumn(), new ColumnWeightData(50));
		widgetsTableLayout.setColumnData(widgetIdColumn.getColumn(), new ColumnWeightData(40));
		widgetsTableLayout.setColumnData(widgetClassColumn.getColumn(), new ColumnWeightData(40));
		widgetsComposite.setLayout(widgetsTableLayout);

		// / HEADERS
		Composite container = new Composite(sashForm, SWT.NONE);
		container.setLayout(new GridLayout(2, true));

		Label lblCssProperties = new Label(container, SWT.NONE);
		lblCssProperties.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));
		lblCssProperties.setText(Messages.CssSpyPart_CSS_Properties);

		Label lblCssRules = new Label(container, SWT.NONE);
		lblCssRules.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));
		lblCssRules.setText(Messages.CssSpyPart_CSS_Rules);

		// // THE CSS PROPERTIES TABLE
		Composite propsComposite = new Composite(container, SWT.BORDER);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		gridData.minimumHeight = 50;
		propsComposite.setLayoutData(gridData);

		cssPropertiesViewer = new TableViewer(propsComposite,
				SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
		cssPropertiesViewer.setContentProvider(new CSSPropertiesContentProvider());
		cssPropertiesViewer.getTable().setLinesVisible(true);
		cssPropertiesViewer.getTable().setHeaderVisible(true);
		cssPropertiesViewer.setComparator(new ViewerComparator());

		final TextCellEditor textCellEditor = new TextCellEditor(cssPropertiesViewer.getTable());
		TableViewerEditor.create(cssPropertiesViewer,
				new TableViewerFocusCellManager(cssPropertiesViewer,
						new FocusCellOwnerDrawHighlighter(cssPropertiesViewer)),
				new ColumnViewerEditorActivationStrategy(cssPropertiesViewer),
				ColumnViewerEditor.TABBING_HORIZONTAL | ColumnViewerEditor.TABBING_MOVE_TO_ROW_NEIGHBOR
						| ColumnViewerEditor.TABBING_VERTICAL | ColumnViewerEditor.KEYBOARD_ACTIVATION);

		TableViewerColumn propName = new TableViewerColumn(cssPropertiesViewer, SWT.NONE);
		propName.getColumn().setWidth(100);
		propName.getColumn().setText(Messages.CssSpyPart_Property);
		propName.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((CSSPropertyProvider) element).getPropertyName();
			}
		});

		TableViewerColumn propValue = new TableViewerColumn(cssPropertiesViewer, SWT.NONE);
		propValue.getColumn().setWidth(100);
		propValue.getColumn().setText(Messages.CssSpyPart_Value);
		propValue.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				try {
					return ((CSSPropertyProvider) element).getValue();
				} catch (Exception e) {
					System.err.println(MessageFormat.format(Messages.CssSpyPart_Error_fetching_property, element, e));
					return null;
				}
			}
		});
		propValue.setEditingSupport(new EditingSupport(cssPropertiesViewer) {
			@Override
			protected CellEditor getCellEditor(Object element) {
				// do the fancy footwork here to return an appropriate
				// editor to
				// the value-type
				return textCellEditor;
			}

			@Override
			protected boolean canEdit(Object element) {
				return true;
			}

			@Override
			protected Object getValue(Object element) {
				try {
					String value = ((CSSPropertyProvider) element).getValue();
					return value == null ? "" : value; //$NON-NLS-1$
				} catch (Exception e) {
					return ""; //$NON-NLS-1$
				}
			}

			@Override
			protected void setValue(Object element, Object value) {
				try {
					if (value == null || ((String) value).trim().length() == 0) {
						return;
					}
					CSSPropertyProvider provider = (CSSPropertyProvider) element;
					provider.setValue((String) value);
				} catch (Exception e) {
					MessageDialog.openError(activeShell, Messages.CssSpyPart_Error, MessageFormat.format("{0}\n\n", Messages.CssSpyPart_Unable_to_set_property) + e.getMessage()); //$NON-NLS-1$
				}
				cssPropertiesViewer.update(element, null);
			}
		});

		TableColumnLayout propsTableLayout = new TableColumnLayout();
		propsTableLayout.setColumnData(propName.getColumn(), new ColumnWeightData(50));
		propsTableLayout.setColumnData(propValue.getColumn(), new ColumnWeightData(50));
		propsComposite.setLayout(propsTableLayout);

		// / THE CSS RULES
		cssRules = new Text(container, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI);
		cssRules.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		// / THE CSS PROPERTIES TABLE (again)
		showUnsetProperties = new Button(container, SWT.CHECK);
		showUnsetProperties.setText(Messages.CssSpyPart_Show_unset_properties);
		showCssFragment = new Button(container, SWT.PUSH);
		showCssFragment.setText(Messages.CssSpyPart_Show_CSS_fragment);
		showCssFragment.setToolTipText(Messages.CssSpyPart_Generates_CSS_rule_block_for_the_selected_widget);

		copyHierarchyToClipboard = new Button(container, SWT.PUSH);
		copyHierarchyToClipboard.setText(Messages.CssSpyPart_Copy_widget_info);
		copyHierarchyToClipboard.setToolTipText(Messages.CssSpyPart_Copy_widget_hierarchy_tooltip);
		copyHierarchyToClipboard.setEnabled(false);

		// / The listeners

		cssSearchBox.addModifyListener(new ModifyListener() {
			private Runnable updater;
			private IProgressMonitor monitor;

			@Override
			public void modifyText(ModifyEvent e) {
				if (monitor != null) {
					monitor.setCanceled(false);
				}
				display.timerExec(200, updater = new Runnable() {
					@Override
					public void run() {
						if (updater == this) {
							performCSSSearch(monitor = new NullProgressMonitor());
						}
					}
				});
			}
		});
		cssSearchBox.addKeyListener(KeyListener.keyPressedAdapter(e -> {
			if (e.keyCode == SWT.ARROW_DOWN && (e.stateMask & SWT.MODIFIER_MASK) == 0) {
				widgetTreeViewer.getControl().setFocus();
			}
		}));

		widgetTreeViewer.addSelectionChangedListener(event -> {
			updateForWidgetSelection(event.getSelection());
			showCssFragment.setEnabled(!event.getSelection().isEmpty());
			copyHierarchyToClipboard.setEnabled(!event.getSelection().isEmpty());
		});
		if (isLive()) {
			container.addMouseMoveListener(e -> {
				if (followSelection.getSelection()) {
					update();
				}
			});
		}

		showAllShells.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> updateWidgetTreeInput()));

		outer.addDisposeListener(e -> dispose());

		showUnsetProperties.setSelection(true);
		showUnsetProperties.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			if (showUnsetProperties.getSelection()) {
				cssPropertiesViewer.removeFilter(unsetPropertyFilter);
			} else {
				cssPropertiesViewer.addFilter(unsetPropertyFilter);
			}
		}));

		showCssFragment.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				showCssFragment();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});

		copyHierarchyToClipboard.addSelectionListener(
				SelectionListener.widgetSelectedAdapter(e -> copyWidgetHierarchyToClipboard()));

		sashForm.setWeights(50, 50);
		widgetTreeViewer.getControl().setFocus();

		return outer;
	}

	/**
	 * This method listen to current part and adapt the contents of spy part.
	 */
	@Inject
	protected void reactOnActivate(@Named(IServiceConstants.ACTIVE_PART) MPart p, MPart cssPart,
			@Named(IServiceConstants.ACTIVE_SHELL) Shell s) {
		if (followSelection == null || !followSelection.getSelection()) {
			return;
		}
		if (outer == null) {
			// Do nothing if no UI created.
			return;
		}
		activeShell = s;

		// Check if control is in the css spy part shell.
		Control control = display.getCursorControl();

		Shell controlShell = (control == null) ? display.getActiveShell() : control.getShell();
		Shell spyPartShell = outer.getShell();

		if (p != cssPart) {
			// Must remove the highlights if selected
			disposeHighlights();

		} else if (spyPartShell != controlShell) {
			// A widget has been selected in another shell.. We can display the
			// corresponding control as a specimen
			shown = null;
			setSpecimen(control);
		}

	}

	protected void showCssFragment() {
		if (!(widgetTreeViewer.getSelection() instanceof IStructuredSelection)
				|| widgetTreeViewer.getSelection().isEmpty()) {
			return;
		}

		StringBuilder sb = new StringBuilder();
		for (Object o : ((IStructuredSelection) widgetTreeViewer.getSelection()).toArray()) {
			if (o instanceof Widget) {
				if (sb.length() > 0) {
					sb.append('\n');
				}
				addCssFragment((Widget) o, sb);
			}
		}
		TextPopupDialog tpd = new TextPopupDialog(widgetTreeViewer.getControl().getShell(), Messages.CssSpyPart_CSS, sb.toString(), true,
				Messages.CssSpyPart_Escape_to_dismiss);
		tpd.open();
	}

	private void addCssFragment(Widget w, StringBuilder sb) {
		CSSStylableElement element = getCSSElement(w);
		if (element == null) {
			return;
		}

		sb.append(element.getLocalName());
		if (element.getCSSId() != null) {
			sb.append("#").append(element.getCSSId()); //$NON-NLS-1$
		}
		sb.append(" {"); //$NON-NLS-1$

		CSSEngine engine = getCSSEngine(element);
		// we first check the viewCSS and then the property values
		CSSStyleDeclaration decl = engine.getViewCSS().getComputedStyle(element, null);

		List<String> propertyNames = new ArrayList<>(engine.getCSSProperties(element));
		Collections.sort(propertyNames);

		int count = 0;

		// First list the generated properties
		for (Iterator<String> iter = propertyNames.iterator(); iter.hasNext();) {
			String propertyName = iter.next();
			String genValue = trim(engine.retrieveCSSProperty(element, propertyName, "")); //$NON-NLS-1$
			String declValue = null;

			if (genValue == null) {
				continue;
			}

			if (decl != null) {
				CSSValue cssValue = decl.getPropertyCSSValue(propertyName);
				if (cssValue != null) {
					declValue = trim(cssValue.getCssText());
				}
			}
			if (count == 0) {
				sb.append(MessageFormat.format("\n  /* {0} */", Messages.CssSpyPart_actual_values)); //$NON-NLS-1$
			}
			sb.append("\n  ").append(propertyName).append(": ").append(genValue).append(";"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			if (declValue != null) {
				sb.append(MessageFormat.format("\t/* {0} */", MessageFormat.format(Messages.CssSpyPart_declared_in_CSS, declValue))); //$NON-NLS-1$
			}
			count++;
			iter.remove(); // remove so we don't re-report below
		}

		// then list any declared properties; generated properties already
		// removed
		if (decl != null) {
			int declCount = 0;
			for (String propertyName : propertyNames) {
				String declValue = null;
				CSSValue cssValue = decl.getPropertyCSSValue(propertyName);
				if (cssValue != null) {
					declValue = trim(cssValue.getCssText());
				}
				if (declValue == null) {
					continue;
				}
				if (declCount == 0) {
					sb.append(MessageFormat.format("\n\n  /* {0} */", Messages.CssSpyPart_declared_in_CSS_rules)); //$NON-NLS-1$
				}
				sb.append("\n  ").append(propertyName).append(": ").append(declValue).append(";"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				count++;
				declCount++;
			}
		}
		sb.append(count > 0 ? "\n}" : "}"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/** Trim the string; return null if empty */
	private String trim(String s) {
		if (s == null) {
			return null;
		}
		s = s.trim();
		return s.length() > 0 ? s : null;
	}

	protected void performCSSSearch(IProgressMonitor progress) {
		List<Widget> widgets = new ArrayList<>();
		performCSSSearch(progress, cssSearchBox.getText(), widgets);
		if (!progress.isCanceled()) {
			revealAndSelect(widgets);
		}
	}

	private void performCSSSearch(IProgressMonitor monitor, String text, Collection<Widget> results) {

		if (text.trim().isEmpty()) {
			return;
		}
		widgetTreeViewer.collapseAll();
		Object[] roots = widgetTreeProvider.getElements(widgetTreeViewer.getInput());
		SubMonitor subMonitor = SubMonitor.convert(monitor, MessageFormat.format(Messages.CssSpyPart_Searching_for, text), roots.length * 10);
		for (Object root : roots) {
			if (monitor.isCanceled()) {
				return;
			}

			CSSStylableElement element = getCSSElement(root);
			if (element == null) {
				continue;
			}

			CSSEngine engine = getCSSEngine(root);
			try {
				SelectorList selectors = engine.parseSelectors(text);
				subMonitor.split(2);
				processCSSSearch(subMonitor.split(8), engine, selectors, element, null, results);
			} catch (CSSParseException | IOException e) {
				System.out.println(e.toString());
			}
		}
		monitor.done();
	}

	private void processCSSSearch(IProgressMonitor monitor, CSSEngine engine, SelectorList selectors,
			CSSStylableElement element, String pseudo, Collection<Widget> results) {

		if (monitor.isCanceled()) {
			return;
		}
		NodeList children = element.getChildNodes();
		SubMonitor subMonitor = SubMonitor.convert(monitor, Messages.CssSpyPart_Searching, 5 + 5 * children.getLength());
		boolean matched = false;
		for (int i = 0; i < selectors.getLength(); i++) {
			if (matched = engine.matches(selectors.item(i), element, pseudo)) {
				break;
			}
		}
		if (matched) {
			results.add((Widget) element.getNativeWidget());
		}
		subMonitor.split(5);
		for (int i = 0; i < children.getLength(); i++) {
			processCSSSearch(subMonitor.split(5), engine, selectors,
					(CSSStylableElement) children.item(i), pseudo, results);
		}
	}

	protected void copyWidgetHierarchyToClipboard() {
		if (!(widgetTreeViewer.getSelection() instanceof IStructuredSelection sel) || sel.isEmpty()) {
			return;
		}
		Object first = sel.getFirstElement();
		if (!(first instanceof Widget selected)) {
			return;
		}

		StringBuilder sb = new StringBuilder();
		sb.append("Widget Hierarchy\n================\n\n"); //$NON-NLS-1$
		appendWidgetSubtree(selected, sb, 0);

		Clipboard clipboard = new Clipboard(display);
		try {
			clipboard.setContents(new Object[] { sb.toString() }, new Transfer[] { TextTransfer.getInstance() });
		} finally {
			clipboard.dispose();
		}
	}

	private void appendWidgetSubtree(Widget widget, StringBuilder sb, int depth) {
		String indent = "  ".repeat(depth); //$NON-NLS-1$

		CSSStylableElement element = getCSSElement(widget);
		if (element == null) {
			sb.append(indent).append(widget.getClass().getSimpleName()).append("\n"); //$NON-NLS-1$
		} else {
			// CSS selector line: type#id.class1.class2
			sb.append(indent).append(element.getLocalName());
			if (element.getCSSId() != null) {
				sb.append("#").append(element.getCSSId()); //$NON-NLS-1$
			}
			if (element.getCSSClass() != null) {
				for (String cls : element.getCSSClass().split(" +")) { //$NON-NLS-1$
					sb.append(".").append(cls); //$NON-NLS-1$
				}
			}
			sb.append("\n"); //$NON-NLS-1$

			// Warn when CSS tree parent differs from SWT parent — selector matching uses SWT
			Node cssParentNode = element.getParentNode();
			if (cssParentNode instanceof CSSStylableElement cssParent
					&& widget instanceof Control c && !c.isDisposed()) {
				Object cssParentNative = cssParent.getNativeWidget();
				if (cssParentNative != c.getParent()) {
					sb.append(indent)
							.append("  ! CSS tree parent (").append(cssParent.getLocalName()).append(")") //$NON-NLS-1$ //$NON-NLS-2$
							.append(" differs from SWT parent (") //$NON-NLS-1$
							.append(c.getParent().getClass().getSimpleName())
							.append(") — CSS selectors use SWT parent\n"); //$NON-NLS-1$
				}
			}

			if (element.getCSSClass() != null) {
				sb.append(indent).append("  Classes: ").append(element.getCSSClass()).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
			}

			// Filtered SWT style bits — only those meaningful for this widget type
			if (element.getAttribute("style") != null) { //$NON-NLS-1$
				Set<String> relevant = getRelevantStyleBits(widget);
				List<String> filtered = new ArrayList<>();
				for (String bit : element.getAttribute("style").split(" +")) { //$NON-NLS-1$ //$NON-NLS-2$
					if (relevant.contains(bit)) {
						filtered.add(bit);
					}
				}
				if (!filtered.isEmpty()) {
					sb.append(indent).append("  SWT Style: ").append(String.join(" ", filtered)).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
			}

			if (element.getCSSStyle() != null) {
				sb.append(indent).append("  Inline CSS: ").append(element.getCSSStyle()).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
			}

			// CSS properties: computed value, declared token where different, and matched rule source
			CSSEngine engine = getCSSEngine(element);
			CSSStyleDeclaration decl = engine.getViewCSS().getComputedStyle(element, null);
			Map<String, String> ruleSources = getCSSRuleSources(engine, element);
			List<String> propertyNames = new ArrayList<>(engine.getCSSProperties(element));
			Collections.sort(propertyNames);
			boolean hasProperties = false;
			String cssBgValue = null;
			for (String propertyName : propertyNames) {
				String computedValue = trim(engine.retrieveCSSProperty(element, propertyName, "")); //$NON-NLS-1$
				if (computedValue == null) {
					continue;
				}
				if (!hasProperties) {
					sb.append(indent).append("  CSS Properties:\n"); //$NON-NLS-1$
					hasProperties = true;
				}
				if ("background-color".equals(propertyName)) { //$NON-NLS-1$
					cssBgValue = computedValue;
				}
				sb.append(indent).append("    ").append(propertyName).append(": ").append(computedValue); //$NON-NLS-1$ //$NON-NLS-2$
				// Build annotation: declared token (with inherit resolution) and/or matched rule source
				String declaredValue = null;
				if (decl != null) {
					CSSValue cssValue = decl.getPropertyCSSValue(propertyName);
					if (cssValue != null) {
						String dv = trim(cssValue.getCssText());
						if (dv != null && !dv.equals(computedValue)) {
							if ("inherit".equalsIgnoreCase(dv)) { //$NON-NLS-1$
								String inheritSource = findInheritSource(engine, element, propertyName);
								declaredValue = inheritSource != null
										? "inherit \u2192 from " + inheritSource //$NON-NLS-1$ (→)
										: "inherit"; //$NON-NLS-1$
							} else {
								declaredValue = dv;
							}
						}
					}
				}
				String ruleSource = ruleSources.get(propertyName);
				if (declaredValue != null || ruleSource != null) {
					sb.append("  /* "); //$NON-NLS-1$
					if (declaredValue != null) {
						sb.append("declared: ").append(declaredValue); //$NON-NLS-1$
						if (ruleSource != null) {
							sb.append(" \u2014 "); //$NON-NLS-1$ (—)
						}
					}
					if (ruleSource != null) {
						sb.append(ruleSource);
					}
					sb.append(" */"); //$NON-NLS-1$
				}
				sb.append("\n"); //$NON-NLS-1$
			}

			// Actual SWT background — ⚠ if it differs from the CSS computed background-color
			if (widget instanceof Control c && !c.isDisposed()) {
				Color bg = c.getBackground();
				if (bg != null && !bg.isDisposed()) {
					RGB rgb = bg.getRGB();
					sb.append(indent).append("  SWT background: rgb(") //$NON-NLS-1$
							.append(rgb.red).append(",") //$NON-NLS-1$
							.append(rgb.green).append(",") //$NON-NLS-1$
							.append(rgb.blue).append(")"); //$NON-NLS-1$
					if (cssBgValue != null && !swtRGBMatchesCSSColor(rgb, cssBgValue)) {
						sb.append("  \u26a0 differs from CSS background-color"); //$NON-NLS-1$ (⚠)
					}
					sb.append("\n"); //$NON-NLS-1$
				}
			}

			Rectangle bounds = getBounds(widget);
			if (bounds != null) {
				sb.append(indent).append("  Bounds: x=").append(bounds.x) //$NON-NLS-1$
						.append(" y=").append(bounds.y) //$NON-NLS-1$
						.append(" w=").append(bounds.width) //$NON-NLS-1$
						.append(" h=").append(bounds.height).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

		// Recurse into children as shown in the tree
		for (Object child : widgetTreeProvider.getChildren(widget)) {
			if (child instanceof Widget childWidget) {
				appendWidgetSubtree(childWidget, sb, depth + 1);
			}
		}
	}

	/**
	 * Returns a map from CSS property name to "selector @ filename" for the last
	 * matching CSS rule that declares each property for the given element. Iterates
	 * stylesheets in source order so the last match (highest cascade precedence by
	 * source position) wins.
	 */
	private Map<String, String> getCSSRuleSources(CSSEngine engine, CSSStylableElement element) {
		Map<String, String> sources = new HashMap<>();
		if (!(engine instanceof DocumentCSS docCSS)) {
			return sources;
		}
		StyleSheetList sheets = docCSS.getStyleSheets();
		for (int i = 0; i < sheets.getLength(); i++) {
			StyleSheet sheet = sheets.item(i);
			if (!(sheet instanceof CSSStyleSheet cssSheet)) {
				continue;
			}
			String href = cssSheet.getHref();
			String filename = href != null ? href.substring(href.lastIndexOf('/') + 1) : null;
			CSSRuleList rules = cssSheet.getCssRules();
			for (int j = 0; j < rules.getLength(); j++) {
				CSSRule rule = rules.item(j);
				if (!(rule instanceof CSSStyleRule styleRule)) {
					continue;
				}
				try {
					SelectorList selectors = engine.parseSelectors(styleRule.getSelectorText());
					boolean matched = false;
					for (int k = 0; k < selectors.getLength(); k++) {
						if (engine.matches(selectors.item(k), element, null)) {
							matched = true;
							break;
						}
					}
					if (!matched) {
						continue;
					}
					String source = styleRule.getSelectorText()
							+ (filename != null ? " @ " + filename : ""); //$NON-NLS-1$
					CSSStyleDeclaration ruleDecl = styleRule.getStyle();
					for (int p = 0; p < ruleDecl.getLength(); p++) {
						sources.put(ruleDecl.item(p), source); // last match in source order wins
					}
				} catch (Exception ignored) {
					// skip unparseable selectors
				}
			}
		}
		return sources;
	}

	/**
	 * Walks up the CSS DOM from {@code element} to find the first ancestor that
	 * has an explicit (non-inherit) declared value for {@code propertyName}, and
	 * returns its CSS element name. Returns null if no such ancestor is found.
	 */
	private String findInheritSource(CSSEngine engine, CSSStylableElement element, String propertyName) {
		Node parentNode = element.getParentNode();
		while (parentNode instanceof CSSStylableElement parentEl) {
			CSSStyleDeclaration parentDecl = engine.getViewCSS().getComputedStyle(parentEl, null);
			if (parentDecl != null) {
				CSSValue parentValue = parentDecl.getPropertyCSSValue(propertyName);
				if (parentValue != null) {
					String parentDeclText = trim(parentValue.getCssText());
					if (parentDeclText != null && !"inherit".equalsIgnoreCase(parentDeclText)) { //$NON-NLS-1$
						return cssElementName(parentEl);
					}
				}
			}
			parentNode = parentNode.getParentNode();
		}
		return null;
	}

	private static String cssElementName(CSSStylableElement el) {
		StringBuilder name = new StringBuilder(el.getLocalName());
		if (el.getCSSId() != null) {
			name.append("#").append(el.getCSSId()); //$NON-NLS-1$
		} else if (el.getCSSClass() != null) {
			name.append(".").append(el.getCSSClass().split(" +")[0]); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return name.toString();
	}

	/**
	 * Returns true if the SWT RGB matches the CSS color string (rgb() or #rrggbb
	 * format). Returns true (no warning) when the format is unrecognised.
	 */
	private static boolean swtRGBMatchesCSSColor(RGB swtRGB, String cssColor) {
		Matcher m = RGB_PATTERN.matcher(cssColor);
		if (m.find()) {
			return swtRGB.red == Integer.parseInt(m.group(1))
					&& swtRGB.green == Integer.parseInt(m.group(2))
					&& swtRGB.blue == Integer.parseInt(m.group(3));
		}
		m = HEX_COLOR_PATTERN.matcher(cssColor.trim());
		if (m.find()) {
			return swtRGB.red == Integer.parseInt(m.group(1), 16)
					&& swtRGB.green == Integer.parseInt(m.group(2), 16)
					&& swtRGB.blue == Integer.parseInt(m.group(3), 16);
		}
		return true; // unrecognised format — assume match, no spurious warning
	}

	protected void dispose() {
		disposeHighlights();
	}

}
