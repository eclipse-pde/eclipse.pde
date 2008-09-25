/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Jan Lohre - APIs are not detected in the old style plug-ins (bug 202258) 
 *******************************************************************************/
Known issues:

1. Internal class PDEState is used.
Note also that API information is not going to be resolved for plug-ins that
use plugn.xml (not manifest.mf) to describe exported packages. (Internally it uses 
PluginConverter.convertManifest() with the parameter analyseJars set to false.) 
See bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=202258 for details.

2. PDEState has some synchronization issues which result in workspace plug-ins not being processed.
To workaround this, at present API tools put all sources into the "target" bucket. 
However, this means that automatic conflict resolution between workspace bundles and target bundles
is not available.

3. For re-exported bundles, we don't store resolution information. As a result if several versions 
of the re-exported bundles are present in the snapshot, one will be picked up randomly.

