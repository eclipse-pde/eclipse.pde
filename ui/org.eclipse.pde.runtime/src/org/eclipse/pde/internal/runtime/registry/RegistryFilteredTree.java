/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.registry;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;

public class RegistryFilteredTree extends FilteredTree {
	
	public RegistryFilteredTree(Composite parent, int treeStyle,
			PatternFilter filter) {
		super(parent, treeStyle, filter);
	}
	
	protected void createControl(Composite parent, int treeStyle) {
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		setLayout(layout);
		setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        if (showFilterControls){
        	filterComposite = new Composite(this, SWT.NONE);
            GridLayout filterLayout = new GridLayout(2, false);
            filterLayout.marginHeight = 4;
            filterLayout.marginWidth = 3;
            filterComposite.setLayout(filterLayout);
            filterComposite.setFont(parent.getFont());
            
        	createFilterControls(filterComposite);
        	filterComposite.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING,
					true, false));
        }
        
        treeComposite = new Composite(this, SWT.NONE);
        GridLayout treeCompositeLayout = new GridLayout();
        treeCompositeLayout.marginHeight = 0;
        treeCompositeLayout.marginWidth = 0;
		treeComposite.setLayout(treeCompositeLayout);
        GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
        treeComposite.setLayoutData(data);
        createTreeControl(treeComposite, SWT.NONE); 
	}
	
}
