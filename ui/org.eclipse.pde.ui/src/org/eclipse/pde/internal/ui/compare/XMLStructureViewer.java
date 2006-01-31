/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.compare;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.compare.structuremergeviewer.IStructureComparator;
import org.eclipse.compare.structuremergeviewer.StructureDiffViewer;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

/**
 * An XML diff tree viewer that can be configured with a <code>IStructureCreator</code>
 * to retrieve a hierarchical structure from the input object (an <code>ICompareInput</code>)
 * and perform a two-way or three-way compare on it.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed outside
 * this package.
 * </p>
 *
 * @see ICompareInput
 */
public class XMLStructureViewer extends StructureDiffViewer {
	
	class XMLSorter extends ViewerSorter {

		public XMLSorter() {
			super();
		}

		public int category(Object node) {
			if (node instanceof DiffNode) {
				Object o = ((DiffNode) node).getId();
				if (o instanceof XMLNode) {
					String xmlType= ((XMLNode) o).getXMLType();
					if (xmlType.equals(XMLStructureCreator.TYPE_ATTRIBUTE))
						return 1;
					if (xmlType.equals(XMLStructureCreator.TYPE_ELEMENT) 
							|| xmlType.equals(XMLStructureCreator.TYPE_TEXT)
							|| xmlType.equals(XMLStructureCreator.TYPE_EXTENSION)
							|| xmlType.equals(XMLStructureCreator.TYPE_EXTENSIONPOINT))
						return 2;
				}
			}
			return 0;
		}

		public void sort(final Viewer viewer, Object[] elements) {
			if (elements != null
					&& elements.length > 0
					&& elements[0] instanceof DiffNode) {
				Object o = ((DiffNode) elements[0]).getId();
				if (o instanceof XMLNode) {
					XMLNode parent = ((XMLNode) o).getParent();
					String sig = parent.getSignature();
					if (sig.endsWith(XMLStructureCreator.SIGN_ELEMENT)) {
						final ArrayList originalTree =
							new ArrayList(Arrays.asList(parent.getChildren()));
						Arrays.sort(elements, new Comparator() {
							public int compare(Object a, Object b) {
								return XMLSorter.this.compare(
									(DiffNode) a,
									(DiffNode) b,
									originalTree);
							}
						});
						return;
					}
				}
			}
			super.sort(viewer, elements);
		}

		private int compare(DiffNode a, DiffNode b, ArrayList originalTree) {

			int index_a = originalTree.indexOf(a.getId());
			int index_b = originalTree.indexOf(b.getId());
			if (index_a < index_b)
				return -1;
			return 1;
		}
	}

	/**
	 * Creates a new viewer under the given SWT parent with the specified configuration.
	 *
	 * @param parent the SWT control under which to create the viewer
	 * @param configuration the configuration for this viewer
	 */
	public XMLStructureViewer(Composite parent, CompareConfiguration configuration) {
		super(parent, configuration);
		setStructureCreator(new XMLStructureCreator());
		setSorter(new XMLSorter());
	}


	protected XMLStructureCreator getXMLStructureCreator() {
		return (XMLStructureCreator) getStructureCreator();
	}


	/*
	 * Recreates the comparable structures for the input sides.
	 */
	protected void compareInputChanged(ICompareInput input) {
		if (input != null) {
			ITypedElement t = input.getLeft();
			if (t != null) {
				String fileExtension = t.getType();
				getXMLStructureCreator().setFileExtension(fileExtension);
			}
		}

		getXMLStructureCreator().initIdMaps();
		super.compareInputChanged(input);

	}


	public IRunnableWithProgress getMatchingRunnable(
			final XMLNode left,
			final XMLNode right,
			final XMLNode ancestor) {
		
		return new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws
					InvocationTargetException,
					InterruptedException,
					OperationCanceledException {
				
				if (monitor == null)
					monitor = new NullProgressMonitor();
				
				int totalWork;
				if (ancestor != null)
					totalWork = 1;
				else
					totalWork = 3;
				monitor.beginTask("Running Matching algorithm...", totalWork); 
				
				AbstractMatching m = new OrderedMatching();
				try {
					m.match(left, right, false, monitor);
					if (ancestor != null) {
						m.match(left, ancestor, true,
							new SubProgressMonitor(monitor, 1));
						m.match(right, ancestor, true,
							new SubProgressMonitor(monitor, 1));
					}
				} finally {
					monitor.done();
				}
			}
		};
	}

	protected void preDiffHook(
			IStructureComparator ancestor,
			IStructureComparator left,
			IStructureComparator right) {
		if (left != null && right != null) {
			try {
				PlatformUI.getWorkbench().getProgressService().run(true, true,
					getMatchingRunnable((XMLNode) left, (XMLNode) right, (XMLNode) ancestor));
			} catch (Exception e) {
				PDEPlugin.log(e);
			}
		}
	}

}
