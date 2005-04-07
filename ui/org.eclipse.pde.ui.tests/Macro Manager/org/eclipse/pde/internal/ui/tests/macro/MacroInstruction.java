/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.tests.macro;

import java.util.Hashtable;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.w3c.dom.Node;

public abstract class MacroInstruction implements IWritable, IPlayable {
    private int [] range;
    private String id;

	public MacroInstruction (String id) {
        this.id = id;
	}
    
    public String getId() {
        return id;
    }
	
	protected void load(Node node, Hashtable lineTable) {
        this.id = MacroUtil.getAttribute(node, "id");
        bindSourceLocation(node, lineTable);
	}
    
    void bindSourceLocation(Node node, Map lineTable) {
        Integer[] lines = (Integer[]) lineTable.get(node);
        if (lines != null) {
            range = new int[2];
            range[0] = lines[0].intValue();
            range[1] = lines[1].intValue();
        }
    }

    public int getStartLine() {
        if (range == null)
            return -1;
        return range[0];
    }
    public int getStopLine() {
        if (range == null)
            return -1;
        return range[1];
    }
    

    public boolean playback(Display display, Composite parent, IProgressMonitor monitor) throws CoreException {
        return false;
    }
}