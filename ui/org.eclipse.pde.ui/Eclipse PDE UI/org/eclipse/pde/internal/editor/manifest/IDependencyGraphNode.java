package org.eclipse.pde.internal.editor.manifest;

import java.util.*;

public interface IDependencyGraphNode {
	public Iterator getChildren(boolean flushCashe);
	public IDependencyGraphNode getHomeNode();
	public String getId();
	public IDependencyGraphNode getLastChild();
	public String getName();
	public IDependencyGraphNode getParent();
	public boolean isCyclical();
	public boolean isHomeNode();
	public void setLastChild(IDependencyGraphNode child);
}
