/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.tasks;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.comparator.DeltaXmlVisitor;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IApiProfile;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.comparator.ApiComparator;
import org.eclipse.pde.api.tools.internal.provisional.comparator.DeltaProcessor;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.util.Util;

public class APIFreezeTask extends Task {
	
	public static class APIFreezeDeltaVisitor extends DeltaXmlVisitor {
		private String excludeListLocation;
		private Set excludedElement;

		public APIFreezeDeltaVisitor(String excludeListLocation) throws CoreException {
			super();
			this.excludeListLocation = excludeListLocation;
		}
		protected void processLeafDelta(IDelta delta) {
			if (DeltaProcessor.isCompatible(delta)) {
				if (delta.getKind() == IDelta.ADDED) {
					int modifiers = delta.getModifiers();
					if (Util.isPublic(modifiers)) {
						// if public, we always want to check @since tags
						switch(delta.getFlags()) {
							case IDelta.TYPE_MEMBER :
							case IDelta.METHOD :
							case IDelta.CONSTRUCTOR :
							case IDelta.ENUM_CONSTANT :
							case IDelta.METHOD_WITH_DEFAULT_VALUE :
							case IDelta.METHOD_WITHOUT_DEFAULT_VALUE :
							case IDelta.FIELD :
							case IDelta.TYPE :
								if (!checkExclude(delta)) {
									super.processLeafDelta(delta);
								}
								break;
						}
					} else if (Util.isProtected(modifiers) && !RestrictionModifiers.isExtendRestriction(delta.getRestrictions())) {
						// if protected, we only want to check @since tags if the enclosing class can be subclassed
						switch(delta.getFlags()) {
							case IDelta.TYPE_MEMBER :
							case IDelta.METHOD :
							case IDelta.CONSTRUCTOR :
							case IDelta.ENUM_CONSTANT :
							case IDelta.FIELD :
							case IDelta.TYPE :
								if (!checkExclude(delta)) {
									super.processLeafDelta(delta);
								}
								break;
						}
					}
				}
			} else {
				if (delta.getKind() == IDelta.ADDED) {
					// if public, we always want to check @since tags
					switch(delta.getFlags()) {
						case IDelta.TYPE_MEMBER :
						case IDelta.METHOD :
						case IDelta.CONSTRUCTOR :
						case IDelta.ENUM_CONSTANT :
						case IDelta.METHOD_WITH_DEFAULT_VALUE :
						case IDelta.METHOD_WITHOUT_DEFAULT_VALUE :
						case IDelta.FIELD :
							// ensure that there is a @since tag for the corresponding member
							if (delta.getKind() == IDelta.ADDED && Util.isVisible(delta)) {
								if (!checkExclude(delta)) {
									super.processLeafDelta(delta);
								}
							}
					}
				}
			}
		}
		private boolean checkExclude(IDelta delta) {
			if (this.excludedElement == null) {
				initializeExcludedElement();
			}
			return isExcluded(delta);
		}
		private boolean isExcluded(IDelta delta) {
			String typeName = delta.getTypeName();
			StringBuffer buffer = new StringBuffer(typeName);
			switch(delta.getFlags()) {
				case IDelta.TYPE_MEMBER :
					buffer.append('.').append(delta.getKey());
					break;
				case IDelta.METHOD :
				case IDelta.CONSTRUCTOR :
				case IDelta.ENUM_CONSTANT :
				case IDelta.METHOD_WITH_DEFAULT_VALUE :
				case IDelta.METHOD_WITHOUT_DEFAULT_VALUE :
				case IDelta.FIELD :
					buffer.append('#').append(delta.getKey());
					break;
			}
			String excludeListKey = String.valueOf(buffer);
			if (this.excludedElement.contains(excludeListKey)) {
				return true;
			}
			System.out.println(excludeListKey);
			return false;
		}
		private void initializeExcludedElement() {
			this.excludedElement = new HashSet();
			if (this.excludeListLocation == null) return;
			File file = new File(this.excludeListLocation);
			if (!file.exists()) return;
			InputStream stream = null;
			char[] contents = null;
			try {
				stream = new BufferedInputStream(new FileInputStream(file));
				contents = Util.getInputStreamAsCharArray(stream, -1, "ISO-8859-1"); //$NON-NLS-1$
			} catch (FileNotFoundException e) {
				// ignore
			} catch (IOException e) {
				// ignore
			} finally {
				if (stream != null) {
					try {
						stream.close();
					} catch (IOException e) {
						// ignore
					}
				}
			}
			if (contents == null) return;
			LineNumberReader reader = new LineNumberReader(new StringReader(new String(contents)));
			String line = null;
			try {
				while ((line = reader.readLine()) != null) {
					if (line.startsWith("#")) continue; //$NON-NLS-1$
					this.excludedElement.add(line);
				}
			} catch (IOException e) {
				// ignore
			} finally {
				try {
					reader.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}

	private static final String PLUGINS_FOLDER_NAME = "plugins"; //$NON-NLS-1$
	private static final String ECLIPSE_FOLDER_NAME = "eclipse"; //$NON-NLS-1$
	private static final String CVS_FOLDER_NAME = "CVS"; //$NON-NLS-1$
	private static final String REFERENCE = "reference"; //$NON-NLS-1$
	private static final String CURRENT = "currentProfile"; //$NON-NLS-1$
	private static final String REFERENCE_PROFILE_NAME = "reference_profile"; //$NON-NLS-1$
	private static final String CURRENT_PROFILE_NAME = "current_profile"; //$NON-NLS-1$

	private static final boolean DEBUG = true;

	private String referenceLocation;
	private String profileLocation;
	private String reportLocation;
	private String eeFileLocation;
	private String excludeListLocation;

	public void setProfile(String profileLocation) {
		this.profileLocation = profileLocation;
	}
	public void setReference(String referenceLocation) {
		this.referenceLocation = referenceLocation;
	}
	public void setReport(String reportLocation) {
		this.reportLocation = reportLocation;
	}
	public void setExcludeList(String excludeListLocation) {
		this.excludeListLocation = excludeListLocation;
	}
	public void setEEFile(String eeFileLocation) {
		this.eeFileLocation = eeFileLocation;
	}
	
	public void execute() throws BuildException {
		if (DEBUG) {
			System.out.println("reference : " + this.referenceLocation); //$NON-NLS-1$
			System.out.println("profile to compare : " + this.profileLocation); //$NON-NLS-1$
			System.out.println("report location : " + this.reportLocation); //$NON-NLS-1$
		}
		if (this.referenceLocation == null
				|| this.profileLocation == null
				|| this.reportLocation == null) {
			StringWriter out = new StringWriter();
			PrintWriter writer = new PrintWriter(out);
			writer.println("Missing arguments :"); //$NON-NLS-1$
			writer.print("reference location :"); //$NON-NLS-1$
			writer.println(this.referenceLocation);
			writer.print("current profile location :"); //$NON-NLS-1$
			writer.println(this.profileLocation);
			writer.print("report location :"); //$NON-NLS-1$
			writer.println(this.reportLocation);
			writer.flush();
			writer.close();
			throw new BuildException(String.valueOf(out.getBuffer()));
		}
		// unzip reference
		File tempDir = new File(System.getProperty("java.io.tmpdir")); //$NON-NLS-1$
		
		File referenceInstallDir = new File(tempDir, REFERENCE);
		extractSDK(referenceInstallDir, this.referenceLocation);

		File profileInstallDir = new File(tempDir, CURRENT);
		extractSDK(profileInstallDir, this.profileLocation);

		// run the comparison
		// create profile for the reference
		IApiProfile referenceProfile = createProfile(REFERENCE_PROFILE_NAME, getInstallDir(tempDir, REFERENCE), this.eeFileLocation);
		IApiProfile currentProfile = createProfile(CURRENT_PROFILE_NAME, getInstallDir(tempDir, CURRENT), this.eeFileLocation);
		
		IDelta delta = null;
		long time = 0;
		if (DEBUG) {
			time = System.currentTimeMillis();
		}
		try {
			delta = ApiComparator.compare(referenceProfile, currentProfile, VisibilityModifiers.API);
		} finally {
			referenceProfile.dispose();
			currentProfile.dispose();
			Util.delete(referenceInstallDir);
			Util.delete(profileInstallDir);
		}
		if (delta == null) {
			// an error occured during the comparison
			throw new BuildException("An error occured during the comparison"); //$NON-NLS-1$
		}
		if (delta != ApiComparator.NO_DELTA) {
			// dump the report in the appropriate folder
			BufferedWriter writer = null;
			File outputFile = new File(this.reportLocation);
			if (outputFile.exists()) {
				// delete the file
				// TODO we might want to customize it
				outputFile.delete();
			} else {
				File outputDir = outputFile.getParentFile();
				if (!outputDir.exists()) {
					if (!outputDir.mkdirs()) {
						throw new BuildException("An error occured creating the parent of the report file : " + this.reportLocation); //$NON-NLS-1$
					}
				}
			}
			try {
				writer = new BufferedWriter(new FileWriter(outputFile));
				APIFreezeDeltaVisitor visitor = new APIFreezeDeltaVisitor(this.excludeListLocation);
				delta.accept(visitor);
				writer.write(visitor.getXML());
				writer.flush();
			} catch (IOException e) {
				ApiPlugin.log(e);
			} catch (CoreException e) {
				ApiPlugin.log(e);
			} finally {
				try {
					if (writer != null) {
						writer.close();
					}
				} catch(IOException e) {
					// ignore
				}
			}
			if (DEBUG) {
				System.out.println("Time spent: " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}
	private static void extractSDK(File installDir, String location) {
		if (installDir.exists()) {
			// delta existing folder
			if (!Util.delete(installDir)) {
				throw new BuildException("Could not delete : " + installDir.getAbsolutePath()); //$NON-NLS-1$
			}
		}
		if (!installDir.mkdirs()) {
			throw new BuildException("Could not create : " + installDir.getAbsolutePath()); //$NON-NLS-1$
		}

		try {
			Util.unzip(location, installDir.getAbsolutePath());
		} catch (IOException e) {
			throw new BuildException("Could not unzip SDK into : " + installDir.getAbsolutePath()); //$NON-NLS-1$
		}
	}
	
	private static String getInstallDir(File dir, String profileInstallName) {
		return new File(new File(new File(dir, profileInstallName), ECLIPSE_FOLDER_NAME), PLUGINS_FOLDER_NAME).getAbsolutePath();
	}

	private static IApiProfile createProfile(String profileName, String fileName, String eeFileLocation) {
		try {
			IApiProfile baseline = null;
			if (ApiPlugin.isRunningInFramework()) {
				baseline = Factory.newApiProfile(profileName);
			} else if (eeFileLocation != null) {
				baseline = Factory.newApiProfile(profileName, new File(eeFileLocation));
			} else {
				baseline = Factory.newApiProfile(profileName, Util.getEEDescriptionFile());
			}
			// create a component for each jar/directory in the folder
			File dir = new File(fileName);
			File[] files = dir.listFiles();
			List components = new ArrayList();
			for (int i = 0; i < files.length; i++) {
				File bundle = files[i];
				if (!bundle.getName().equals(CVS_FOLDER_NAME)) {
					// ignore CVS folder
					IApiComponent component = baseline.newApiComponent(bundle.getAbsolutePath());
					if(component != null) {
						components.add(component);
					}
				}
			}
			
			baseline.addApiComponents((IApiComponent[]) components.toArray(new IApiComponent[components.size()]));
			return baseline;
		} catch (CoreException e) {
			e.printStackTrace();
			return null;
		}
	}
}
