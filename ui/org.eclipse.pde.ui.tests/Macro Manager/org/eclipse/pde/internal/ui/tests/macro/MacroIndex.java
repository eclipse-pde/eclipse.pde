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

import java.io.PrintWriter;

public class MacroIndex extends MacroInstruction {
    
    public MacroIndex() {
        super(null);
    }

	public MacroIndex(String id) {
		super(id);
	}

	public void write(String indent, PrintWriter writer) {
		writer.print(indent);
		writer.print("<index id=\"");
		writer.print(getId());
		writer.println("\"/>");
	}
}