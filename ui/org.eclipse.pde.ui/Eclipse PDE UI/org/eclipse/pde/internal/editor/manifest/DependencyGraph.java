package org.eclipse.pde.internal.editor.manifest;

import org.eclipse.jface.action.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.*;
import org.eclipse.swt.widgets.*;
import java.util.*;
import org.eclipse.pde.internal.forms.*;

public class DependencyGraph extends Canvas implements IHyperlinkListener {
	private IDependencyGraphNode rootNode;
	private IDependencyGraphNode selectedNode;
	private boolean borderPainted;
	private boolean reverse;
	public static final int OP_HOME = 1;
	public static final int OP_BACK = 2;
	public static final int OP_FORWARD = 3;
	private boolean shadowPainted;
	private HyperlinkHandler hyperlinkHandler = new HyperlinkHandler();

	private int horizontalMargin = 3;
	private int verticalMargin = 20;
	public int verticalSpacing = 5;
	private int horizontalSpacing = 20;
	private Color homeBackground;
	private Color nodeForeground;
	private Color nodeBackground;
	private Color activeNodeBackground;
	private boolean traversalEnabled=true;
	private Color errorBackground;
	private Color activeHomeBackground;
	private Color borderColor;
	private boolean dirty;
	private Color shadowColor;
	private Color homeForeground;

	class GraphLayout extends Layout {
		int hpadding = 2;
		int vpadding = 1;
		int borderWidth = 1;
		/**
		* Computes the preferred size.
		*/
		public Point computeSize(
			Composite parent,
			int wHint,
			int hHint,
			boolean changed) {
			int width = 0;
			int height = 0;

			Control[] children = parent.getChildren();

			if (children.length == 0)
				return new Point(width, height);

			int maxNodeWidth = 0;
			Label rootLabel = (Label) children[0];

			width =
				horizontalMargin * 2
					+ rootLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT, changed).x
					+ hpadding * 2
					+ borderWidth*2
					+ horizontalSpacing;
			height = verticalMargin * 2 + vpadding *2;

			for (int i = 1; i < children.length; i++) {
				Label label = (Label) children[i];
				Point gsize = label.computeSize(SWT.DEFAULT, SWT.DEFAULT, changed);
				maxNodeWidth = Math.max(maxNodeWidth, gsize.x + hpadding*2 + borderWidth * 2);
				if (i < children.length - 1) {
					height += gsize.y + vpadding*2 + borderWidth *2 + verticalSpacing;
				}
			}
			width += maxNodeWidth;

			if (wHint != SWT.DEFAULT)
				width = wHint;
			if (hHint != SWT.DEFAULT)
				height = hHint;
			return new Point(width, height);
		}
		public void layout(Composite parent, boolean changed) {
			Control[] children = parent.getChildren();
			if (children.length == 0)
				return;
			Rectangle clientArea = parent.getClientArea();
			Label root = (Label) children[0];

			int x = clientArea.x + horizontalMargin;
			int y = clientArea.y + verticalMargin;

			Point rootSize = root.computeSize(SWT.DEFAULT, SWT.DEFAULT, changed);
			root.setBounds(x, y, rootSize.x + hpadding * 2, rootSize.y + vpadding * 2);

			x += rootSize.x + hpadding * 2 + +borderWidth*2 + horizontalSpacing;

			for (int i = 1; i < children.length; i++) {
				Label node = (Label) children[i];
				Point nodeSize = node.computeSize(SWT.DEFAULT, SWT.DEFAULT, changed);
				node.setBounds(x, y, nodeSize.x + hpadding * 2, nodeSize.y + vpadding * 2);
				y += nodeSize.y + vpadding * 2 + borderWidth*2 + verticalSpacing;
			}
		}
	}
	private Vector rootNodeListeners = new Vector();
	private org.eclipse.jface.action.IMenuListener popupListener;
	private MenuManager menuManager;

public DependencyGraph(Composite parent, int style) {
	super(parent, style);
	setLayout(new GraphLayout());
	addPaintListener(new PaintListener() {
		public void paintControl(PaintEvent e) {
			paintGraph(e.gc);
		}
	});
	menuManager = new MenuManager("#PopupMenu");
	Menu menu = menuManager.createContextMenu(this);
	this.setMenu(menu);
	menuManager.addMenuListener(new IMenuListener() {
		public void menuAboutToShow(IMenuManager manager) {
			Control node = hyperlinkHandler.getLastLink();
			if (node != null) {
				selectedNode = (IDependencyGraphNode) node.getData();
			}
			if (popupListener != null)
				popupListener.menuAboutToShow(manager);
		}
	});
}
public void addRootNodeListener(IRootNodeListener listener) {
	rootNodeListeners.addElement(listener);
}
public boolean canDoOperation(int operation) {
	switch (operation) {
		case OP_BACK :
			return (rootNode != null && rootNode.isHomeNode() == false);
		case OP_FORWARD :
			return (rootNode != null && rootNode.getLastChild()!=null);
		case OP_HOME :
			return true;
	}
	return false;
}
private void clearGraph() {
	Control [] children = getChildren();
	if (children.length >0) {
		for (int i=0; i<children.length; i++) {
			children[i].dispose();
		}
	}
	hyperlinkHandler.reset();
}
private void createGraphObjects() {
	if (rootNode==null) return;

	Label rootLabel = createLabel(this, rootNode);
	for (Iterator iter = rootNode.getChildren(isDirty()); iter.hasNext();) {
		IDependencyGraphNode node = (IDependencyGraphNode)iter.next();
		Label label = createLabel(this, node);
		if (traversalEnabled) hyperlinkHandler.registerHyperlink(label, this);
	}
}
protected Label createLabel(Composite parent, IDependencyGraphNode node) {
	Label label = new Label(parent, SWT.NONE);
	boolean useHomeColor = node.isHomeNode();
	if (reverse)
		useHomeColor = (!useHomeColor && node != rootNode);

	label.setBackground(useHomeColor ? homeBackground : nodeBackground);
	if (node.isCyclical())
		label.setForeground(errorBackground);
	else
		label.setForeground(useHomeColor ? homeForeground : nodeForeground);
	label.setText(node.getName());
	label.setData(node);
	label.setAlignment(SWT.CENTER);
	label.setToolTipText(node.getId());
	return label;
}
public void dispose () {
	super.dispose();
	hyperlinkHandler.dispose();

}
public void doOperation(int operation) {
	if (canDoOperation(operation)==false) return;
	switch (operation) {
		case OP_BACK :
			IDependencyGraphNode parent = rootNode.getParent();
			parent.setLastChild(rootNode);
			setRoot(parent);
			break;
		case OP_FORWARD :
			setRoot(rootNode.getLastChild());
			break;
		case OP_HOME :
			if (rootNode!=null) setRoot(rootNode.getHomeNode());
			break;
	}
}
private void drawArrow(GC gc, int x, int y) {
	if (reverse) {
		gc.drawLine(x+4, y+2, x+4, y-2);
		gc.drawLine(x+4, y+1, x+2, y+1);
		gc.drawLine(x+4, y-1, x+2, y-1);
	}
	else {
		gc.drawLine(x-5, y+2, x-5, y-2);
		gc.drawLine(x-5, y+1, x-3, y+1);
		gc.drawLine(x-5, y-1, x-3, y-1);
	}
}
public org.eclipse.swt.graphics.Color getActiveHomeBackground() {
	return activeHomeBackground;
}
public org.eclipse.swt.graphics.Color getActiveNodeBackground() {
	return activeNodeBackground;
}
public org.eclipse.swt.graphics.Color getBorderColor() {
	return borderColor;
}
public org.eclipse.swt.graphics.Color getErrorBackground() {
	return errorBackground;
}
public org.eclipse.swt.graphics.Color getHomeBackground() {
	return homeBackground;
}
public org.eclipse.swt.graphics.Color getHomeForeground() {
	return homeForeground;
}
public int getHorizontalMargin() {
	return horizontalMargin;
}
public int getHorizontalSpacing() {
	return horizontalSpacing;
}
public org.eclipse.jface.action.MenuManager getMenuManager() {
	return menuManager;
}
public org.eclipse.swt.graphics.Color getNodeBackground() {
	return nodeBackground;
}
public org.eclipse.swt.graphics.Color getNodeForeground() {
	return nodeForeground;
}
public org.eclipse.jface.action.IMenuListener getPopupListener() {
	return popupListener;
}
public IDependencyGraphNode getSelectedNode() {
	return selectedNode;
}
public org.eclipse.swt.graphics.Color getShadowColor() {
	return shadowColor;
}
public int getVerticalMargin() {
	return verticalMargin;
}
public int getVerticalSpacing() {
	return verticalSpacing;
}
private void initializeHyperlinkHandler() {
	hyperlinkHandler.setHyperlinkUnderlineMode(HyperlinkHandler.UNDERLINE_NEVER);
	hyperlinkHandler.setBackground(reverse ? homeBackground : nodeBackground);
	hyperlinkHandler.setActiveBackground(
		reverse ? activeHomeBackground : activeNodeBackground);
}
public boolean isBorderPainted() {
	return borderPainted;
}
public boolean isDirty() {
	return dirty;
}
public boolean isReverse() {
	return reverse;
}
public boolean isShadowPainted() {
	return shadowPainted;
}
public boolean isTraversalEnabled() {
	return traversalEnabled;
}
public void linkActivated(Control linkLabel) {
	setRoot((IDependencyGraphNode)linkLabel.getData());
}
public void linkEntered(org.eclipse.swt.widgets.Control linkLabel) {}
public void linkExited(org.eclipse.swt.widgets.Control linkLabel) {}
private void paintGraph(GC gc) {
	Control[] children = getChildren();

	if (borderPainted) {
		gc.setForeground(borderColor);
	}
	if (shadowPainted) {
		gc.setBackground(shadowColor);
	}
	for (int i = 0; i < children.length; i++) {
		Control child = children[i];
		Rectangle bounds = child.getBounds();
		if (shadowPainted) {
			gc.fillRectangle(bounds.x + 3, bounds.y + 3, bounds.width, bounds.height);
		}
		if (borderPainted) {
			gc.drawRectangle(
				bounds.x - 1,
				bounds.y - 1,
				bounds.width + 1,
				bounds.height + 1);
		}

	}
	if (children.length > 1) {
		// draw a line between the root and the first child
		gc.setForeground(children[0].getForeground());
		gc.setBackground(gc.getForeground());
		Rectangle rootBounds = children[0].getBounds();
		Rectangle firstChildBounds = children[1].getBounds();
		int x0 = rootBounds.x + rootBounds.width;
		int y0 = rootBounds.y + rootBounds.height / 2;
		int x1 = firstChildBounds.x;
		int y1 = y0;
		gc.drawLine(x0, y0, x1, y1);

		// draw arrow
		if (reverse)
		   drawArrow (gc, x0, y0);
		else 
		drawArrow(gc, x1, y1);

		int cx0 = (x1 + x0) / 2;

		for (int i = 2; i < children.length; i++) {
			Control child = children[i];
			Rectangle childBounds = child.getBounds();
			int cy0 = y0;
			int kx = cx0;
			int ky = childBounds.y + childBounds.height / 2;
			int x2 = childBounds.x;
			int y2 = ky;
			gc.drawLine(cx0, cy0, kx, ky);
			gc.drawLine(kx, ky, x2, y2);
			if (!reverse)
			   drawArrow(gc, x2, y2);
		}
	}
}
public void removeRootNodeListener(IRootNodeListener listener) {
	rootNodeListeners.removeElement(listener);
}
public void setActiveHomeBackground(org.eclipse.swt.graphics.Color newActiveHomeBackground) {
	activeHomeBackground = newActiveHomeBackground;
}
public void setActiveNodeBackground(Color color) {
	activeNodeBackground = color;
}
public void setBorderColor(org.eclipse.swt.graphics.Color newBorderColor) {
	borderColor = newBorderColor;
}
public void setBorderPainted(boolean newBorderPainted) {
	borderPainted = newBorderPainted;
}
public void setDirty(boolean newDirty) {
	dirty = newDirty;
}
public void setErrorBackground(org.eclipse.swt.graphics.Color newErrorBackground) {
	errorBackground = newErrorBackground;
}
public void setHomeBackground(org.eclipse.swt.graphics.Color newHomeBackground) {
	homeBackground = newHomeBackground;
}
public void setHomeForeground(org.eclipse.swt.graphics.Color newHomeForeground) {
	homeForeground = newHomeForeground;
}
public void setHorizontalMargin(int newHorizontalMargin) {
	horizontalMargin = newHorizontalMargin;
}
public void setHorizontalSpacing(int newHorizontalSpacing) {
	horizontalSpacing = newHorizontalSpacing;
}
public void setMenuManager(org.eclipse.jface.action.MenuManager newMenuManager) {
	menuManager = newMenuManager;
}
public void setNodeBackground(Color color) {

   nodeBackground = color;
}
public void setNodeForeground(Color color) {
	nodeForeground = color;
	hyperlinkHandler.setForeground(nodeForeground);
}
public void setPopupListener(org.eclipse.jface.action.IMenuListener newPopupListener) {
	popupListener = newPopupListener;
}
public void setReverse(boolean newReverse) {
	if (newReverse != reverse) {
		dirty=true;
		reverse = newReverse;
		setRoot(rootNode);
		dirty=false;
	}
}
public void setRoot(final IDependencyGraphNode node) {
	clearGraph();
	rootNode = node;
	initializeHyperlinkHandler();
	createGraphObjects();
	layout(true);
	redraw();
	for (Iterator iter = rootNodeListeners.iterator(); iter.hasNext();) {
		((IRootNodeListener) iter.next()).rootNodeChanged(node);
	}
}
public void setShadowColor(org.eclipse.swt.graphics.Color newShadowColor) {
	shadowColor = newShadowColor;
}
public void setShadowPainted(boolean newShadowPainted) {
	shadowPainted = newShadowPainted;
}
public void setTraversalEnabled(boolean newTraversalEnabled) {
	traversalEnabled = newTraversalEnabled;
}
public void setVerticalMargin(int newVerticalMargin) {
	verticalMargin = newVerticalMargin;
}
public void setVerticalSpacing(int newVerticalSpacing) {
	verticalSpacing = newVerticalSpacing;
}
}
