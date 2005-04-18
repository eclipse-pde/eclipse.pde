/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.wizards.templates;

import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.templates.ITemplateSection;
import org.eclipse.pde.ui.templates.NewPluginTemplateWizard;

public class IntroNewWizard extends NewPluginTemplateWizard {
    
    public void init(IFieldData data) {
        super.init(data);
        setWindowTitle(PDEUIMessages.IntroNewWizard_wtitle);      
    }

    public ITemplateSection[] createTemplateSections() {
        return new ITemplateSection[] {new IntroTemplate()};
    }

}
