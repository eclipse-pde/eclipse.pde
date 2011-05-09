/*******************************************************************************
 *  Copyright (c) 2008, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.build;

import java.util.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.publisher.eclipse.FeatureEntry;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.internal.build.site.BuildTimeFeature;

public class ShapeAdvisor implements IPDEBuildConstants {
	public static final String UPDATEJAR = "updateJar"; //$NON-NLS-1$
	public static final String FLAT = "flat"; //$NON-NLS-1$

	public static final String FOLDER = "folder"; //$NON-NLS-1$
	public static final String FILE = "file"; //$NON-NLS-1$

	private Properties pluginsPostProcessingSteps;
	private Properties featuresPostProcessingSteps;
	private boolean forceUpdateJarFormat = false;

	public ShapeAdvisor() {
		try {
			pluginsPostProcessingSteps = AbstractScriptGenerator.readProperties(AbstractScriptGenerator.getWorkingDirectory(), DEFAULT_PLUGINS_POSTPROCESSINGSTEPS_FILENAME_DESCRIPTOR, IStatus.INFO);
			featuresPostProcessingSteps = AbstractScriptGenerator.readProperties(AbstractScriptGenerator.getWorkingDirectory(), DEFAULT_FEATURES_POSTPROCESSINGSTEPS_FILENAME_DESCRIPTOR, IStatus.INFO);
		} catch (CoreException e) {
			//Ignore
		}
	}

	public void setForceUpdateJars(boolean force) {
		this.forceUpdateJarFormat = force;
	}

	public Object[] getFinalShape(BundleDescription bundle) {
		String style = getUnpackClause(bundle) ? FLAT : UPDATEJAR;
		return getFinalShape(bundle.getSymbolicName(), bundle.getVersion().toString(), style, true);
	}

	public Object[] getFinalShape(BuildTimeFeature feature) {
		return getFinalShape(feature.getId(), feature.getVersion(), FLAT, true);
	}

	private Object[] getFinalShape(String name, String version, String initialShape, boolean bundle) {
		String style = initialShape;
		style = getShapeOverride(name, bundle, style);

		if (FLAT.equalsIgnoreCase(style)) {
			//do nothing
			return new Object[] {name + '_' + version, FOLDER};
		}
		if (UPDATEJAR.equalsIgnoreCase(style)) {
			return new Object[] {name + '_' + version + ".jar", FILE}; //$NON-NLS-1$
		}
		return new Object[] {name + '_' + version, FOLDER};
	}

	private String getShapeOverride(String name, boolean bundle, String initialStyle) {
		String result = initialStyle;
		Properties currentProperties = bundle ? pluginsPostProcessingSteps : featuresPostProcessingSteps;
		if (currentProperties.size() > 0) {
			String styleFromFile = currentProperties.getProperty(name);
			if (styleFromFile == null)
				styleFromFile = currentProperties.getProperty(IBuildPropertiesConstants.DEFAULT_FINAL_SHAPE);
			result = styleFromFile;
		}
		if (forceUpdateJarFormat)
			result = UPDATEJAR;
		return result;
	}

	private boolean getUnpackClause(BundleDescription bundle) {
		Properties properties = (Properties) bundle.getUserObject();
		if (properties != null) {
			Set entries = (Set) properties.get(PLUGIN_ENTRY);
			if (entries != null && entries.size() > 0) {
				Boolean result = null;
				boolean contradiction = false;
				for (Iterator iterator = entries.iterator(); iterator.hasNext();) {
					FeatureEntry entry = (FeatureEntry) iterator.next();
					if (entry.unpackSet()) {
						if (result == null)
							result = Boolean.valueOf(entry.isUnpack());
						else if (result.booleanValue() != entry.isUnpack()) {
							contradiction = true;
							break;
						}
					}
				}
				if (result != null && !contradiction)
					return result.booleanValue();
			}

			String shape = properties.getProperty(ECLIPSE_BUNDLE_SHAPE);
			if (shape != null) {
				return shape.equalsIgnoreCase("dir"); //$NON-NLS-1$
			}
		}

		return true; //don't know, return the default
	}
}
