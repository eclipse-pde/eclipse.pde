/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.tasks;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.tools.ant.BuildException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.model.StubApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.comparator.ApiComparator;
import org.eclipse.pde.api.tools.internal.provisional.comparator.ApiScope;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiScope;
import org.eclipse.pde.api.tools.internal.util.FilteredElements;

/**
 * Ant task to run the API freeze check during Eclipse build.
 */
public class APIFreezeTask extends CommonUtilsTask {
	
	private boolean debug;

	private String eeFileLocation;
	private String excludeListLocation;
	private String includeListLocation;

	public void execute() throws BuildException {
		if (this.referenceBaselineLocation == null
				|| this.currentBaselineLocation == null
				|| this.reportLocation == null) {
			StringWriter out = new StringWriter();
			PrintWriter writer = new PrintWriter(out);
			writer.println(
				NLS.bind(Messages.printArguments,
					new String[] {
						this.referenceBaselineLocation,
						this.currentBaselineLocation,
						this.reportLocation,
					})
			);
			writer.flush();
			writer.close();
			throw new BuildException(String.valueOf(out.getBuffer()));
		}
		if (this.debug) {
			System.out.println("reference : " + this.referenceBaselineLocation); //$NON-NLS-1$
			System.out.println("baseline to compare : " + this.currentBaselineLocation); //$NON-NLS-1$
			System.out.println("report location : " + this.reportLocation); //$NON-NLS-1$
			if (this.excludeListLocation != null) {
				System.out.println("exclude list location : " + this.excludeListLocation); //$NON-NLS-1$
			} else {
				System.out.println("No exclude list location"); //$NON-NLS-1$
			}
			if (this.includeListLocation != null) {
				System.out.println("include list location : " + this.includeListLocation); //$NON-NLS-1$
			} else {
				System.out.println("No include list location"); //$NON-NLS-1$
			}
		}
		File outputFile = new File(this.reportLocation);
		if (outputFile.exists()) {
			if (outputFile.isDirectory()){
				// the output file cannot be a directory
				throw new BuildException(
						NLS.bind(Messages.reportLocationHasToBeAFile, outputFile.getAbsolutePath()));
			}
		} else {
			File outputDir = outputFile.getParentFile();
			if (!outputDir.exists()) {
				if (!outputDir.mkdirs()) {
					throw new BuildException(
						NLS.bind(Messages.errorCreatingParentReportFile, outputDir.getAbsolutePath()));
				}
			}
		}
		int index = this.reportLocation.lastIndexOf('.');
		if (index == -1
				|| !this.reportLocation.substring(index).toLowerCase().equals(".xml")) { //$NON-NLS-1$
			throw new BuildException(Messages.deltaReportTask_xmlFileLocationShouldHaveAnXMLExtension);
		}
		// unzip reference
		long time = 0;
		if (this.debug) {
			time = System.currentTimeMillis();
		}
		File referenceInstallDir = extractSDK(REFERENCE, this.referenceBaselineLocation);

		File baselineInstallDir = extractSDK(CURRENT, this.currentBaselineLocation);
		if (this.debug) {
			System.out.println("Extraction of both archives : " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
			time = System.currentTimeMillis();
		}
		// run the comparison
		// create baseline for the reference
		IApiBaseline referenceBaseline = createBaseline(REFERENCE_BASELINE_NAME, referenceInstallDir.getAbsolutePath(), this.eeFileLocation);
		IApiBaseline currentBaseline = createBaseline(CURRENT_BASELINE_NAME, baselineInstallDir.getAbsolutePath(), this.eeFileLocation);
		
		FilteredElements excludedElements = CommonUtilsTask.initializeFilteredElements(this.excludeListLocation, currentBaseline, this.debug);

		if (this.debug) {
			System.out.println("===================================================================================="); //$NON-NLS-1$
			System.out.println("Excluded elements list:"); //$NON-NLS-1$
			System.out.println(excludedElements);
		}
		
		FilteredElements includedElements = CommonUtilsTask.initializeFilteredElements(this.includeListLocation, currentBaseline, this.debug);

		if (this.debug) {
			System.out.println("===================================================================================="); //$NON-NLS-1$
			System.out.println("Included elements list:"); //$NON-NLS-1$
			System.out.println(includedElements);
		}
		
		IDelta delta = null;
		if (this.debug) {
			System.out.println("Creation of both baselines : " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
			time = System.currentTimeMillis();
		}
		try {
			delta = ApiComparator.compare(getScope(currentBaseline), referenceBaseline, VisibilityModifiers.API, true, null);
		} catch (CoreException e) {
			// ignore
		} finally {
			if (this.debug) {
				System.out.println("API freeze check : " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
				time = System.currentTimeMillis();
			}
			referenceBaseline.dispose();
			currentBaseline.dispose();
			StubApiComponent.disposeAllCaches();
			deleteBaseline(this.referenceBaselineLocation, referenceInstallDir);
			deleteBaseline(this.currentBaselineLocation, baselineInstallDir);
			if (this.debug) {
				System.out.println("Cleanup : " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
				time = System.currentTimeMillis();
			}
		}
		if (delta == null) {
			// an error occurred during the comparison
			throw new BuildException(Messages.errorInComparison);
		}
		if (delta != ApiComparator.NO_DELTA) {
			// dump the report in the appropriate folder
			BufferedWriter writer = null;
			try {
				writer = new BufferedWriter(new FileWriter(outputFile));
				FilterListDeltaVisitor visitor = new FilterListDeltaVisitor(excludedElements, includedElements, FilterListDeltaVisitor.CHECK_OTHER);
				delta.accept(visitor);
				writer.write(visitor.getXML());
				writer.flush();
				if (this.debug) {
					String potentialExcludeList = visitor.getPotentialExcludeList();
					if (potentialExcludeList.length() != 0) {
						System.out.println("Potential exclude list:"); //$NON-NLS-1$
						System.out.println(potentialExcludeList);
					}
				}
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
			if (this.debug) {
				System.out.println("Report generation : " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}
	private IApiScope getScope(IApiBaseline currentBaseline) {
		IApiComponent[] apiComponents = currentBaseline.getApiComponents();
		ApiScope scope = new ApiScope();
		for (int i = 0, max = apiComponents.length; i < max; i++) {
			IApiComponent apiComponent = apiComponents[i];
			try {
				ResolverError[] errors = apiComponent.getErrors();
				if (errors != null) {
					if (this.debug) {
						System.out.println("Errors for component : " + apiComponent.getSymbolicName()); //$NON-NLS-1$
						for (int j = 0, max2 = errors.length; j < max2; j++) {
							System.out.println(errors[j]);
						}
					}
					continue;
				}
				scope.addElement(apiComponent);
			} catch (CoreException e) {
				// ignore
			}
		}
		return scope;
	}
	/**
	 * Set the debug value.
	 * <p>The possible values are: <code>true</code>, <code>false</code></p>
	 * <p>Default is <code>false</code>.</p>
	 *
	 * @param debugValue the given debug value
	 */
	public void setDebug(String debugValue) {
		this.debug = Boolean.toString(true).equals(debugValue); 
	}
	/**
	 * Set the execution environment file to use.
	 * <p>By default, an execution environment file corresponding to a JavaSE-1.6 execution environment
	 * is used.</p>
	 * <p>The file is specified using an absolute path. This is optional.</p> 
	 *
	 * @param eeFileLocation the given execution environment file
	 */
	public void setEEFile(String eeFileLocation) {
		this.eeFileLocation = eeFileLocation;
	}
	/**
	 * Set the exclude list location.
	 * 
	 * <p>The exclude list is used to know what bundles and members should excluded from the xml report
	 * generated by the task execution. Lines starting with '#' are ignored from
	 * the excluded element.</p>
	 * <p>The format of the exclude file looks like this:</p>
	 * <pre># 229688
	 * org.eclipse.jface.databinding_1.2.0:org.eclipse.jface.databinding.viewers.ObservableListContentProvider#dispose()V
	 * org.eclipse.jface.databinding_1.2.0:org.eclipse.jface.databinding.viewers.ObservableListContentProvider#getElements(Ljava/lang/Object;)[Ljava/lang/Object;
	 * org.eclipse.jface.databinding_1.2.0:org.eclipse.jface.databinding.viewers.ObservableListContentProvider#inputChanged(Lorg/eclipse/jface/viewers/Viewer;Ljava/lang/Object;Ljava/lang/Object;)V
	 * org.eclipse.jface.databinding_1.2.0:org.eclipse.jface.databinding.viewers.ObservableListTreeContentProvider#dispose()V
	 * org.eclipse.jface.databinding_1.2.0:org.eclipse.jface.databinding.viewers.ObservableListTreeContentProvider#getChildren(Ljava/lang/Object;)[Ljava/lang/Object;
	 * ...
	 * </pre>
	 * @param excludeListLocation the given location for the excluded list file
	 */
	public void setExcludeList(String excludeListLocation) {
		this.excludeListLocation = excludeListLocation;
	}
	
	/**
	 * Set the include list location.
	 * 
	 * <p>The include list is used to know what bundles and members should included from the xml report
	 * generated by the task execution. Lines starting with '#' are ignored from
	 * the included element.</p>
	 * <p>The format of the include file looks like this:</p>
	 * <pre>
	 * org.eclipse.jface.databinding_1.2.0:org.eclipse.jface.databinding.viewers.ObservableListContentProvider#dispose()V
	 * org.eclipse.jface.databinding_1.2.0:org.eclipse.jface.databinding.viewers.ObservableListContentProvider#getElements(Ljava/lang/Object;)[Ljava/lang/Object;
	 * org.eclipse.jface.databinding_1.2.0:org.eclipse.jface.databinding.viewers.ObservableListContentProvider#inputChanged(Lorg/eclipse/jface/viewers/Viewer;Ljava/lang/Object;Ljava/lang/Object;)V
	 * org.eclipse.jface.databinding_1.2.0:org.eclipse.jface.databinding.viewers.ObservableListTreeContentProvider#dispose()V
	 * org.eclipse.jface.databinding_1.2.0:org.eclipse.jface.databinding.viewers.ObservableListTreeContentProvider#getChildren(Ljava/lang/Object;)[Ljava/lang/Object;
	 * ...
	 * </pre>
	 * @param includeListLocation the given location for the included list file
	 */
	public void setIncludeList(String includeListLocation) {
		this.includeListLocation = includeListLocation;
	}
	
	/**
	 * Set the location of the current product or baseline that you want to compare against
	 * the reference baseline.
	 * 
	 * <p>It can be a .zip, .jar, .tgz, .tar.gz file, or a directory that corresponds to 
	 * the Eclipse installation folder. This is the directory is which you can find the 
	 * Eclipse executable.
	 * </p>
	 *
	 * @param baselineLocation the given location for the baseline to analyze
	 */
	public void setProfile(String baselineLocation) {
		this.currentBaselineLocation = baselineLocation;
	}
	/**
	 * Set the location of the reference baseline.
	 * 
	 * <p>It can be a .zip, .jar, .tgz, .tar.gz file, or a directory that corresponds to 
	 * the Eclipse installation folder. This is the directory is which you can find the 
	 * Eclipse executable.
	 * </p>
	 *
	 * @param baselineLocation the given location for the reference baseline to analyze
	 */
	public void setBaseline(String baselineLocation) {
		this.referenceBaselineLocation = baselineLocation;
	}
	/**
	 * Set the given report file name to be generated.
	 * 
	 * @param reportLocation the given report file name to be generated.
	 */
	public void setReport(String reportLocation) {
		this.reportLocation = reportLocation;
	}
}
