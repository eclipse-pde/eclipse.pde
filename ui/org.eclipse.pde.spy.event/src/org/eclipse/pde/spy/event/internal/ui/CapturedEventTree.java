/*******************************************************************************
 * Copyright (c) 2013, 2022 IBM Corporation and others.
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
package org.eclipse.pde.spy.event.internal.ui;

import java.util.ArrayList;

import org.eclipse.core.databinding.beans.typed.PojoProperties;
import org.eclipse.core.databinding.observable.IObservable;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.core.databinding.observable.map.IObservableMap;
import org.eclipse.core.databinding.observable.masterdetail.IObservableFactory;
import org.eclipse.core.databinding.observable.set.IObservableSet;
import org.eclipse.jface.databinding.viewers.ObservableListTreeContentProvider;
import org.eclipse.jface.databinding.viewers.ObservableMapLabelProvider;
import org.eclipse.jface.databinding.viewers.TreeStructureAdvisor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.pde.spy.event.internal.model.CapturedEvent;
import org.eclipse.pde.spy.event.internal.model.IEventItem;
import org.eclipse.pde.spy.event.internal.model.ItemToFilter;
import org.eclipse.pde.spy.event.internal.util.JDTUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Resource;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

@SuppressWarnings({"rawtypes", "unchecked"})
public class CapturedEventTree extends TreeViewer {
	private ICapturedEventTreeListener listener;

	private WritableList<CapturedEvent> capturedEvents;

	private Clipboard clipboard;

	private TreeItemCursor treeItemCursor;

	private TreeItemForeground treeItemForeground;

	private TreeItemBackground treeItemBackground;

	private TreeItemFont treeItemFont;

	private SelectedTreeItem selectedClassNameTreeItem;

	private SelectedTreeItem selectedTreeItem;

	public CapturedEventTree(Composite parent) {
		super(parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);

		getTree().setHeaderVisible(true);
		getTree().setLinesVisible(true);

		TreeColumn column = new TreeColumn(getTree(), SWT.LEFT);
		column.setText(ItemToFilter.Topic.toString());
		column.setWidth(350);

		column = new TreeColumn(getTree(), SWT.LEFT);
		column.setText(ItemToFilter.Publisher.toString());
		column.setWidth(150);

		column = new TreeColumn(getTree(), SWT.LEFT);
		column.setText(ItemToFilter.ChangedElement.toString());
		column.setWidth(150);

		ObservableListTreeContentProvider contentProvider = new ObservableListTreeContentProvider<>(
				new CapturedEventsObservableFactory(), new CapturedEventsTreeStructureAdvisor());
		setContentProvider(contentProvider);

		IObservableMap[] attributes = observeMaps(contentProvider.getKnownElements(), IEventItem.class,
				new String[] { Messages.CapturedEventTree_Name, Messages.CapturedEventTree_Param1, Messages.CapturedEventTree_Param2 });
		setLabelProvider(new ObservableMapLabelProvider(attributes));

		capturedEvents = new WritableList<>(new ArrayList<>(), CapturedEvent.class);
		setInput(capturedEvents);

		clipboard = new Clipboard(getTree().getDisplay());

		createTreeItemResources();

		addTreeEventListeners();
	}

	private static class CapturedEventsTreeStructureAdvisor extends TreeStructureAdvisor<CapturedEvent> {
		@Override
		public Boolean hasChildren(CapturedEvent element) {
			return !element.getParameters().isEmpty();
		}
	}


	private static IObservableMap[] observeMaps(IObservableSet domain,
			Class pojoClass, String[] propertyNames) {
		IObservableMap[] result = new IObservableMap[propertyNames.length];
		for (int i = 0; i < propertyNames.length; i++) {
			result[i] = observeMap(domain, pojoClass, propertyNames[i]);
		}
		return result;
	}

	private static IObservableMap observeMap(IObservableSet domain,
			Class pojoClass, String propertyName) {
		return PojoProperties.value(pojoClass, propertyName).observeDetail(
				domain);
	}

	private static class CapturedEventsObservableFactory implements IObservableFactory {
		@Override
		public IObservable createObservable(Object target) {
			if (target instanceof IObservableList) {
				return (IObservableList) target;
			}
			if (target instanceof CapturedEvent) {
				PojoProperties.list("parameters").observe(target);
			}
			return null;
		}
	}

	private void createTreeItemResources() {
		Display display = getTree().getDisplay();

		treeItemCursor = new TreeItemCursor(getTree().getCursor(), display.getSystemCursor(SWT.CURSOR_HAND));

		treeItemForeground = new TreeItemForeground(new Color(display, new RGB(0, 0, 120)),
				display.getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT), display.getSystemColor(SWT.COLOR_BLACK));

		treeItemBackground = new TreeItemBackground(display.getSystemColor(SWT.COLOR_LIST_SELECTION),
				getTree().getBackground());

		Font currentFont = getTree().getFont();
		FontData currentFontData = currentFont.getFontData()[0];
		treeItemFont = new TreeItemFont(currentFont,
				new Font(display, currentFontData.getName(), currentFontData.getHeight(), SWT.ITALIC));

		selectedClassNameTreeItem = new SelectedTreeItem() {
			@Override
			public void clear() {
				redrawTreeItem(getTreeItem(), getColumnIndex());
				super.clear();

				Tree tree = getTree();
				if (tree.getCursor() != treeItemCursor.getDefaultCursor()) {
					tree.setCursor(treeItemCursor.getDefaultCursor());
				}
			}
		};

		selectedTreeItem = new SelectedTreeItem() {
			@Override
			public void clear() {
				redrawTreeItem(getTreeItem(), getColumnIndex());
				super.clear();
			}
		};
	}

	private void addTreeEventListeners() {
		getTree().addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				if (clipboard != null && !clipboard.isDisposed()) {
					clipboard.dispose();
				}
				disposeResource(treeItemForeground.getParamColor());
				disposeResource(treeItemFont.getSelectedClassNameFont());
			}

			private void disposeResource(Resource resource) {
				if (resource != null && !resource.isDisposed()) {
					resource.dispose();
				}
			}
		});

		getTree().addMouseMoveListener(e -> {
			selectedClassNameTreeItem.clear();

			// we can select and finally open the class only when 'ctrl' is
			// pressed
			if ((e.stateMask & SWT.CTRL) != SWT.CTRL) {
				return;
			}

			TreeItem item = getTree().getItem(new Point(e.x, e.y));
			int index = getSelectedColumnIndex(item, e.x, e.y);

			if (index > 0 /* we check the 2nd and 3rd column only */ && item
					.getParentItem() == null /*
												 * we don't check parameters at
												 * this moment
												 */) {
				String text = item.getText(index);
				if (JDTUtils.containsClassName(text)) {
					selectedClassNameTreeItem.setText(text);
					selectedClassNameTreeItem.setColumnIndex(index);
					selectedClassNameTreeItem.setTreeItem(item);
					getTree().setCursor(treeItemCursor.getPointerCursor());
					redrawTreeItem(item, index);
				}
			}
		});

		getTree().addMouseListener(MouseListener.mouseDownAdapter(e -> {
			TreeItem item = getTree().getItem(new Point(e.x, e.y));
			updateSelectedTreeItem(item, getSelectedColumnIndex(item, e.x, e.y));

			if (listener != null && (e.stateMask & SWT.CTRL) == SWT.CTRL
					&& selectedClassNameTreeItem.getText() != null) {
				listener.treeItemWithClassNameClicked(selectedClassNameTreeItem.getText());
			}
		}));

		getTree().addListener(SWT.EraseItem, event -> {
			if ((event.detail & SWT.FOREGROUND) == SWT.FOREGROUND) {
				event.detail &= ~SWT.FOREGROUND;
			}
			if ((event.detail & SWT.SELECTED) == SWT.SELECTED) {
				event.detail &= ~SWT.SELECTED;
			}
		});

		getTree().addListener(SWT.PaintItem, event -> {
			TreeItem item = (TreeItem) event.item;
			String text = item.getText(event.index);
			int xOffset = item.getParentItem() != null ? 10 : 2;
			Rectangle rec = item.getBounds(event.index);

			event.gc.setFont(getFont(item, event.index));
			event.gc.setForeground(getForeground(item, event.index));
			event.gc.setBackground(getBackground(item, event.index));
			event.gc.fillRectangle(rec.x, rec.y, rec.width, rec.height);
			event.gc.drawText(text, event.x + xOffset, event.y, true);
		});

		getTree().addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
				String text = selectedTreeItem.getText();
				if (text == null || (e.stateMask & SWT.CTRL) != SWT.CTRL) {
					return;
				}
				if (e.keyCode == 'c' && text.trim().length() > 0) {
					clipboard.setContents(new Object[] { text }, new Transfer[] { TextTransfer.getInstance() });
				} else if (e.keyCode == SWT.ARROW_LEFT) {
					updateSelectedTreeItem(selectedTreeItem.getTreeItem(),
							Math.max(0, selectedTreeItem.getColumnIndex() - 1));
				} else if (e.keyCode == SWT.ARROW_RIGHT) {
					updateSelectedTreeItem(selectedTreeItem.getTreeItem(),
							Math.min(getTree().getColumnCount() - 1, selectedTreeItem.getColumnIndex() + 1));
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
				selectedClassNameTreeItem.clear();
			}
		});

		getTree().addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			if ((e.stateMask & SWT.BUTTON1) != SWT.BUTTON1) {
				updateSelectedTreeItem((TreeItem) e.item,
						selectedTreeItem.getText() != null ? selectedTreeItem.getColumnIndex() : 0);
			}
		}));

		getTree().addFocusListener(FocusListener.focusLostAdapter(e -> selectedClassNameTreeItem.clear()));
	}

	private void updateSelectedTreeItem(TreeItem item, int columnIndex) {
		if (columnIndex > -1) {
			selectedTreeItem.clear(); // clear old selection
			selectedTreeItem.setTreeItem(item);
			selectedTreeItem.setColumnIndex(columnIndex);
			selectedTreeItem.setText(item.getText(columnIndex));
			redrawTreeItem(item, columnIndex);
		}
	}

	private Color getForeground(TreeItem item, int index) {
		if (selectedTreeItem.getTreeItem() == item && selectedTreeItem.getColumnIndex() == index) {
			return treeItemForeground.getSelectedColor();
		}
		if (item.getParentItem() != null) {
			return treeItemForeground.getParamColor();
		}
		return treeItemForeground.getDefaultColor();
	}

	private Color getBackground(TreeItem item, int index) {
		if (selectedTreeItem.getTreeItem() == item && selectedTreeItem.getColumnIndex() == index) {
			return treeItemBackground.getSelectedColor();
		}
		return treeItemBackground.getDefaultColor();
	}

	private Font getFont(TreeItem item, int columnIndex) {
		if (selectedClassNameTreeItem.getTreeItem() == item
				&& selectedClassNameTreeItem.getColumnIndex() == columnIndex) {
			return treeItemFont.getSelectedClassNameFont();
		}
		return treeItemFont.getDefaultFont();
	}

	private void redrawTreeItem(TreeItem item, int columnIndex) {
		if (item != null && !item.isDisposed()) {
			Rectangle rec = item.getBounds(columnIndex);
			getTree().redraw(rec.x, rec.y, rec.width, rec.height, true);
		}
	}

	private int getSelectedColumnIndex(TreeItem item, int mouseX, int mouseY) {
		for (int i = 0; item != null && i < getTree().getColumnCount(); i++) {
			Rectangle rec = item.getBounds(i);
			if (mouseX >= rec.x && mouseX <= rec.x + rec.width) {
				return i;
			}
		}
		return -1;
	}

	public void addEvent(CapturedEvent event) {
		capturedEvents.add(event);
	}

	public void setListener(ICapturedEventTreeListener listener) {
		this.listener = listener;
	}

	public void removeAll() {
		capturedEvents.clear();
	}

	private static class TreeItemForeground {
		private final Color paramColor;

		private final Color selectedColor;

		private final Color defaultColor;

		public TreeItemForeground(Color paramColor, Color selectedColor, Color defaultColor) {
			this.paramColor = paramColor;
			this.selectedColor = selectedColor;
			this.defaultColor = defaultColor;
		}

		public Color getParamColor() {
			return paramColor;
		}

		public Color getSelectedColor() {
			return selectedColor;
		}

		public Color getDefaultColor() {
			return defaultColor;
		}
	}

	private static class TreeItemBackground {
		private final Color selectedColor;

		private final Color defaultColor;

		public TreeItemBackground(Color selectedColor, Color defaultColor) {
			this.selectedColor = selectedColor;
			this.defaultColor = defaultColor;
		}

		public Color getSelectedColor() {
			return selectedColor;
		}

		public Color getDefaultColor() {
			return defaultColor;
		}
	}

	private static class TreeItemCursor {
		private final Cursor defaultCursor;

		private final Cursor pointerCursor;

		public TreeItemCursor(Cursor defaultCursor, Cursor pointerCursor) {
			this.defaultCursor = defaultCursor;
			this.pointerCursor = pointerCursor;
		}

		public Cursor getDefaultCursor() {
			return defaultCursor;
		}

		public Cursor getPointerCursor() {
			return pointerCursor;
		}
	}

	private static class TreeItemFont {
		private final Font defaultFont;

		private final Font selectedClassNameFont;

		public TreeItemFont(Font defaultFont, Font selectedClassNameFont) {
			this.defaultFont = defaultFont;
			this.selectedClassNameFont = selectedClassNameFont;
		}

		public Font getDefaultFont() {
			return defaultFont;
		}

		public Font getSelectedClassNameFont() {
			return selectedClassNameFont;
		}
	}

	private static class SelectedTreeItem {
		private TreeItem treeItem;

		private int columnIndex;

		private String text;

		public SelectedTreeItem() {
			clear();
		}

		public void setTreeItem(TreeItem treeItem) {
			this.treeItem = treeItem;
		}

		public TreeItem getTreeItem() {
			return treeItem;
		}

		public void setColumnIndex(int columnIndex) {
			this.columnIndex = columnIndex;
		}

		public int getColumnIndex() {
			return columnIndex;
		}

		public void setText(String text) {
			this.text = text;
		}

		public String getText() {
			return text;
		}

		public void clear() {
			treeItem = null;
			columnIndex = -1;
			text = null;
		}
	}
}
