/*******************************************************************************
 * Copyright (c) 2010, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.search;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.IApiCoreConstants;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IComponentDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMemberDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.util.Signatures;

/**
 * Generates an HTML report from an XML use scan.  The generated report
 * is 'consumer based'.  It lists the bundles that have references in them
 * (rather than listing bundles that produce the types being referenced)
 * 
 * @since 1.0.300
 */
public class ConsumerReportConvertor extends UseReportConverter {
	
	/**
	 * Use scan visitor that collects a list of the bundles (as {@link IComponentDescriptor}s)
	 * that consume api references.
	 */
	class ListConsumersVisitor extends UseScanVisitor{
		/**
		 * Set of {@link IComponentDescriptor}s representing the consumers of references
		 */
		Set consumers = new HashSet();
		
		/* (non-Javadoc)
		 * @see org.eclipse.pde.api.tools.internal.search.UseScanVisitor#visitComponent(org.eclipse.pde.api.tools.internal.provisional.descriptors.IComponentDescriptor)
		 */
		public boolean visitComponent(IComponentDescriptor target) {
			return true;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.pde.api.tools.internal.search.UseScanVisitor#visitReferencingComponent(org.eclipse.pde.api.tools.internal.provisional.descriptors.IComponentDescriptor)
		 */
		public boolean visitReferencingComponent(IComponentDescriptor component) {
			consumers.add(component);
			return false;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.pde.api.tools.internal.search.UseScanVisitor#visitMember(org.eclipse.pde.api.tools.internal.provisional.descriptors.IMemberDescriptor)
		 */
		public boolean visitMember(IMemberDescriptor referencedMember) {
			return false;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.pde.api.tools.internal.search.UseScanVisitor#visitReference(org.eclipse.pde.api.tools.internal.search.IReferenceDescriptor)
		 */
		public void visitReference(IReferenceDescriptor reference) {
		}
		
	}
	
	/**
	 * Use scan visitor that produces the report data for a single consumer bundle.  The visitor collects the
	 * report data in a {@link Consumer}.
	 *
	 */
	class ConsumerReportVisitor extends UseScanVisitor{
		Consumer consumer;
		Map producers = new HashMap();
		private IComponentDescriptor consumerDescriptor;
		private Producer currentProducer;
		private Type2 currenttype = null;
		private Member currentmember = null;
		
		/**
		 * Cache for type descriptions, maps enclosing descriptors to Type objects
		 */
		HashMap keys = new HashMap();
		
		/**
		 * Constructor
		 * @param consumerDescriptor the bundle that we are collecting information on
		 */
		public ConsumerReportVisitor(IComponentDescriptor consumerDescriptor){
			this.consumerDescriptor = consumerDescriptor;
			consumer = new Consumer();
			consumer.name = composeName(consumerDescriptor.getId(), consumerDescriptor.getVersion());
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.pde.api.tools.internal.search.UseScanVisitor#endVisitScan()
		 */
		public void endVisitScan() {
			try {
				long start = 0;
				if(ApiPlugin.DEBUG_USE_REPORT_CONVERTER) {
					System.out.println("Writing consumer report for bundle: "+ consumer.name); //$NON-NLS-1$
					start = System.currentTimeMillis();
				}
				if(consumer.counts.getTotalRefCount() > 0) {
					writeConsumerReport(consumer, producers);
					producers.clear();
				}
				if(ApiPlugin.DEBUG_USE_REPORT_CONVERTER) {
					System.out.println("Done in: "+(System.currentTimeMillis()-start)+ " ms"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			catch(Exception e) {
				ApiPlugin.log(e);
			}
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.pde.api.tools.internal.search.UseScanVisitor#visitComponent(org.eclipse.pde.api.tools.internal.provisional.descriptors.IComponentDescriptor)
		 */
		public boolean visitComponent(IComponentDescriptor target) {
			currentProducer = new Producer();
			currentProducer.name = composeName(target.getId(), target.getVersion());
			return true;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.pde.api.tools.internal.search.UseScanVisitor#endVisitComponent(org.eclipse.pde.api.tools.internal.provisional.descriptors.IComponentDescriptor)
		 */
		public void endVisitComponent(IComponentDescriptor target) {
			if(this.currentProducer.counts.getTotalRefCount() > 0) {
				try {
					if (!producers.containsKey(currentProducer.name)){
						producers.put(currentProducer.name, currentProducer);
					}
					long start = 0;
					if(ApiPlugin.DEBUG_USE_REPORT_CONVERTER) {
						System.out.println("Writing producer report for bundle: "+ currentProducer.name); //$NON-NLS-1$
						start = System.currentTimeMillis();
					}
					if(consumer.counts.getTotalRefCount() > 0) {
						writeProducerReport(consumer, currentProducer);
					}
					if(ApiPlugin.DEBUG_USE_REPORT_CONVERTER) {
						System.out.println("Done in: "+(System.currentTimeMillis()-start)+ " ms"); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
				catch(Exception e) {
					ApiPlugin.log(e);
				}
			}
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.pde.api.tools.internal.search.UseScanVisitor#visitReferencingComponent(org.eclipse.pde.api.tools.internal.provisional.descriptors.IComponentDescriptor)
		 */
		public boolean visitReferencingComponent(IComponentDescriptor component) {
			return component.equals(consumerDescriptor);
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.pde.api.tools.internal.search.UseScanVisitor#endVisitReferencingComponent(org.eclipse.pde.api.tools.internal.provisional.descriptors.IComponentDescriptor)
		 */
		public void endVisitReferencingComponent(IComponentDescriptor component) {
			//Do nothing, visitor only runs for one consumer at a time, html gets written and the end of the scan
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.pde.api.tools.internal.search.UseScanVisitor#visitMember(org.eclipse.pde.api.tools.internal.provisional.descriptors.IMemberDescriptor)
		 */
		public boolean visitMember(IMemberDescriptor referencedMember) {
			IReferenceTypeDescriptor desc = getEnclosingDescriptor(referencedMember);
			if(desc == null) {
				return false;
			}
			this.currenttype = (Type2) this.keys.get(desc);
			if(this.currenttype == null) {
				this.currenttype = new Type2(desc);
				this.keys.put(desc, this.currenttype);
			}
			
			currentProducer.types.put(desc, currenttype);
			
			this.currentmember = new Member(referencedMember);
			this.currenttype.referencingMembers.put(referencedMember, currentmember);
			
			return true;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.pde.api.tools.internal.search.UseScanVisitor#endVisitMember(org.eclipse.pde.api.tools.internal.provisional.descriptors.IMemberDescriptor)
		 */
		public void endVisitMember(IMemberDescriptor referencedMember) {
			if(this.currenttype.counts.getTotalRefCount() == 0) {
				IReferenceTypeDescriptor desc = getEnclosingDescriptor(referencedMember);
				if(desc != null) {
					this.keys.remove(desc);
					this.currentProducer.types.remove(this.currenttype);
				}
			}
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.pde.api.tools.internal.search.UseScanVisitor#visitReference(org.eclipse.pde.api.tools.internal.search.IReferenceDescriptor)
		 */
		public void visitReference(IReferenceDescriptor reference) {
			IMemberDescriptor fromMember = reference.getMember();
			if(!acceptReference(reference.getReferencedMember(), topatterns) || 
					!acceptReference(fromMember, frompatterns)) {
				return;
			}
			int lineNumber = reference.getLineNumber();
			int refKind = reference.getReferenceKind();
			int visibility = reference.getVisibility();
			String refname = org.eclipse.pde.api.tools.internal.builder.Reference.getReferenceText(refKind);
			ArrayList refs = (ArrayList) this.currentmember.children.get(refname);
			if(refs == null) {
				refs = new ArrayList();
				this.currentmember.children.put(refname, refs);
			}
			refs.add(new Reference(fromMember, lineNumber, visibility, formatMessages(reference.getProblemMessages())));
			switch(fromMember.getElementType()) {
				case IElementDescriptor.TYPE: {
					switch(visibility) {
						case VisibilityModifiers.API: {
							this.consumer.counts.total_api_type_count++;
							this.currentProducer.counts.total_api_type_count++;
							this.currentmember.counts.total_api_type_count++;
							this.currenttype.counts.total_api_type_count++;
							break;
						}
						case VisibilityModifiers.PRIVATE: {
							this.consumer.counts.total_private_type_count++;
							this.currentProducer.counts.total_private_type_count++;
							this.currentmember.counts.total_private_type_count++;
							this.currenttype.counts.total_private_type_count++;
							break;
						}
						case VisibilityModifiers.PRIVATE_PERMISSIBLE: {
							this.consumer.counts.total_permissable_type_count++;
							this.currentProducer.counts.total_permissable_type_count++;
							this.currentmember.counts.total_permissable_type_count++;
							this.currenttype.counts.total_permissable_type_count++;
							break;
						}
						case FRAGMENT_PERMISSIBLE: {
							this.consumer.counts.total_fragment_permissible_type_count++;
							this.currentProducer.counts.total_fragment_permissible_type_count++;
							this.currentmember.counts.total_fragment_permissible_type_count++;
							this.currenttype.counts.total_fragment_permissible_type_count++;
							break;
						}
						case VisibilityModifiers.ILLEGAL_API: {
							this.consumer.counts.total_illegal_type_count++;
							this.currentProducer.counts.total_illegal_type_count++;
							this.currentmember.counts.total_illegal_type_count++;
							this.currenttype.counts.total_illegal_type_count++;
							break;
						}
					}
					break;
				}
				case IElementDescriptor.METHOD: {
					switch(visibility) {
						case VisibilityModifiers.API: {
							this.consumer.counts.total_api_method_count++;
							this.currentProducer.counts.total_api_method_count++;
							this.currentmember.counts.total_api_method_count++;
							this.currenttype.counts.total_api_method_count++;
							break;
						}
						case VisibilityModifiers.PRIVATE: {
							this.consumer.counts.total_private_method_count++;
							this.currentProducer.counts.total_private_method_count++;
							this.currentmember.counts.total_private_method_count++;
							this.currenttype.counts.total_private_method_count++;
							break;
						}
						case VisibilityModifiers.PRIVATE_PERMISSIBLE: {
							this.consumer.counts.total_permissable_method_count++;
							this.currentProducer.counts.total_permissable_method_count++;
							this.currentmember.counts.total_permissable_method_count++;
							this.currenttype.counts.total_permissable_method_count++;
							break;
						}
						case FRAGMENT_PERMISSIBLE: {
							this.consumer.counts.total_fragment_permissible_method_count++;
							this.currentProducer.counts.total_fragment_permissible_method_count++;
							this.currentmember.counts.total_fragment_permissible_method_count++;
							this.currenttype.counts.total_fragment_permissible_method_count++;
							break;
						}
						case VisibilityModifiers.ILLEGAL_API: {
							this.consumer.counts.total_illegal_method_count++;
							this.currentProducer.counts.total_illegal_method_count++;
							this.currentmember.counts.total_illegal_method_count++;
							this.currenttype.counts.total_illegal_method_count++;
							break;
						}
					}
					break;
				}
				case IElementDescriptor.FIELD: {
					switch(visibility) {
						case VisibilityModifiers.API: {
							this.consumer.counts.total_api_field_count++;
							this.currentProducer.counts.total_api_field_count++;
							this.currentmember.counts.total_api_field_count++;
							this.currenttype.counts.total_api_field_count++;
							break;
						}
						case VisibilityModifiers.PRIVATE: {
							this.consumer.counts.total_private_field_count++;
							this.currentProducer.counts.total_private_field_count++;
							this.currentmember.counts.total_private_field_count++;
							this.currenttype.counts.total_private_field_count++;
							break;
						}
						case VisibilityModifiers.PRIVATE_PERMISSIBLE: {
							this.consumer.counts.total_permissable_field_count++;
							this.currentProducer.counts.total_permissable_field_count++;
							this.currentmember.counts.total_permissable_field_count++;
							this.currenttype.counts.total_permissable_field_count++;
							break;
						}
						case FRAGMENT_PERMISSIBLE: {
							this.consumer.counts.total_fragment_permissible_field_count++;
							this.currentProducer.counts.total_fragment_permissible_field_count++;
							this.currentmember.counts.total_fragment_permissible_field_count++;
							this.currenttype.counts.total_fragment_permissible_field_count++;
							break;
						}
						case VisibilityModifiers.ILLEGAL_API: {
							this.consumer.counts.total_illegal_field_count++;
							this.currentProducer.counts.total_illegal_field_count++;
							this.currentmember.counts.total_illegal_field_count++;
							this.currenttype.counts.total_illegal_field_count++;
							break;
						}
					}
					break;
				}
			}
		}
		
		/**
		 * Returns if the reference should be reported or not
		 * @param desc
		 * @return true if the reference should be reported false otherwise
		 */
		private boolean acceptReference(IMemberDescriptor desc, Pattern[] patterns) {
			if(patterns != null) {
				for (int i = 0; i < patterns.length; i++) {
					if(patterns[i].matcher(desc.getPackage().getName()).find()) {
						return false;
					}
				}
			}
			return true;
		}
		
		/**
		 * Returns the enclosing {@link IReferenceTypeDescriptor} for the given member
		 * descriptor
		 * @param member
		 * @return the enclosing {@link IReferenceTypeDescriptor} or <code>null</code>
		 */
		private IReferenceTypeDescriptor getEnclosingDescriptor(IMemberDescriptor member) {
			switch(member.getElementType()) {
			case IElementDescriptor.TYPE: {
				return (IReferenceTypeDescriptor) member;
			}
			case IElementDescriptor.METHOD:
			case IElementDescriptor.FIELD: {
				return member.getEnclosingType();
			}
			}
			return null;
		}
		

		/**
		 * Formats the arrays of messages
		 * @param messages
		 * @return the formatted messages or <code>null</code>
		 */
		private String formatMessages(String[] messages) {
			if(messages != null) {
				StringBuffer buffer = new StringBuffer();
				for (int i = 0; i < messages.length; i++) {
					buffer.append(messages[i]);
					if(i < messages.length-1) {
						buffer.append("\n"); //$NON-NLS-1$
					}
				}
				return buffer.toString();
			}
			return null;
		}
	}
	
	class Consumer{
		String name;
		CountGroup counts = new CountGroup();
	}
	
	class Producer {
		String name;
		Map types = new HashMap();
		CountGroup counts = new CountGroup();
	}
	
	class Type2 extends Type{
		public Type2(IElementDescriptor desc) {
			super(desc);
		}
		Map referencingMembers = new HashMap();
	}
	
	/**
	 * Constructor
	 * @param htmlroot the folder root where the HTML reports should be written
	 * @param xmlroot the folder root where the current API use scan output is located
	 * @param topatterns array of regular expressions used to prune references to a given name pattern
	 * @param frompatterns array of regular expressions used to prune references from a given name pattern
	 */
	public ConsumerReportConvertor(String htmlroot, String xmlroot, String[] topatterns, String[] frompatterns) {
		super(htmlroot, xmlroot, topatterns, frompatterns);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.UseReportConverter#parse(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected List parse(IProgressMonitor monitor) throws Exception {
		SubMonitor subMon = SubMonitor.convert(monitor, 20);
		ListConsumersVisitor listVisitor = new ListConsumersVisitor();
		UseScanParser parser = new UseScanParser();
		parser.parse(getXmlLocation(), subMon.newChild(5), listVisitor);
		List consumerReports = new ArrayList();
		
		IComponentDescriptor currentConsumer = null;
		ConsumerReportVisitor visitor = null;
		for (Iterator iterator = listVisitor.consumers.iterator(); iterator.hasNext();) {
			currentConsumer = (IComponentDescriptor)iterator.next();
			visitor = new ConsumerReportVisitor(currentConsumer);
			parser.parse(getXmlLocation(), null, visitor);
			if (visitor.consumer.counts.getTotalRefCount() > 0){
				consumerReports.add(visitor.consumer);
			}
		}
		return consumerReports;
	}

	protected String getConsumerTitle(String bundle) {
		return NLS.bind(SearchMessages.ConsumerReportConvertor_ConsumerTitle, bundle);
	}
	
	protected String getProducerTitle(String consumer, String producer) {
		return NLS.bind(SearchMessages.ConsumerReportConvertor_ProducerTitle, consumer, producer);
	}
	
	protected String getIndexTitle() {
		return SearchMessages.ConsumerReportConvertor_IndexTitle;
	}
	
	/**
	 * Writes the main index file for the reports
	 * @param scanResult a list of {@link Consumer} objects returns from the use scan parser
	 * @param reportsRoot 
	 */
	void writeIndexPage(List scanResult) throws Exception {
		Collections.sort(scanResult, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((Consumer)o1).name.compareTo(((Consumer)o2).name);
			}
		});
		
		PrintWriter writer = null;
		try {
			File reportIndex = new File(getHtmlLocation(), "index.html"); //$NON-NLS-1$
			if(!reportIndex.exists()) {
				reportIndex.createNewFile();
			}
			setReportIndex(reportIndex);
			
			StringBuffer buffer = new StringBuffer();
			buffer.append(HTML_HEADER);
			buffer.append(OPEN_HTML).append(OPEN_HEAD).append(CONTENT_TYPE_META);
			writeMetadataHeaders(buffer);
			buffer.append(OPEN_TITLE).append(getIndexTitle()).append(CLOSE_TITLE); 
			buffer.append(CLOSE_HEAD); 
			buffer.append(OPEN_BODY); 
			buffer.append(OPEN_H3).append(getIndexTitle()).append(CLOSE_H3);
			try {
				getMetadata();
				writeMetadataSummary(buffer);
			}
			catch(Exception e) {
				//do nothing, failed meta-data should not prevent the index from being written
			}
			buffer.append(OPEN_H4).append(SearchMessages.UseReportConvertor_additional_infos_section).append(CLOSE_H4); 
			if(hasMissing()) {
				buffer.append(OPEN_P); 
				buffer.append(NLS.bind(SearchMessages.UseReportConverter_missing_bundles_prevented_scan, 
						new String[] {" <a href=\"./missing.html\">", "</a>"})); //$NON-NLS-1$ //$NON-NLS-2$
				buffer.append(CLOSE_P); 
			}
			buffer.append(OPEN_P); 
			buffer.append(NLS.bind(SearchMessages.UseReportConverter_bundles_that_were_not_searched, new String[] {"<a href=\"./not_searched.html\">", "</a></p>\n"}));  //$NON-NLS-1$//$NON-NLS-2$
			String additional = getAdditionalIndexInfo(scanResult.size() > 0);
			if(additional != null) {
				buffer.append(additional);
			}
			if(scanResult.size() > 0) {
				buffer.append(OPEN_P).append(SearchMessages.UseReportConverter_inlined_description).append(CLOSE_P);
				buffer.append(getColourLegend());
				buffer.append(getReferencesTableHeader(SearchMessages.ConsumerReportConvertor_ProducerListHeader, SearchMessages.UseReportConverter_bundle, true));
				if(scanResult.size() > 0) {
					Consumer consumer = null;
					File refereehtml = null;
					String link = null;
					for(Iterator iter = scanResult.iterator(); iter.hasNext();) {
						consumer = (Consumer) iter.next();
						if(consumer != null) {
							refereehtml = new File(getReportsRoot(), consumer.name+File.separator+"index.html"); //$NON-NLS-1$
							link = extractLinkFrom(getReportsRoot(), refereehtml.getAbsolutePath());
							buffer.append(getReferenceTableEntry(consumer.counts, link, consumer.name, true));
						}
					}
					buffer.append(CLOSE_TABLE); 
				}
			}
			else {
				buffer.append(getNoReportsInformation()); 
			}
			buffer.append(W3C_FOOTER);
			buffer.append(CLOSE_BODY).append(CLOSE_HTML);  
			
			//write the file
			writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(reportIndex), IApiCoreConstants.UTF_8));
			writer.print(buffer.toString());
			writer.flush();
		} catch (IOException e) {
			throw new Exception(NLS.bind(SearchMessages.ioexception_writing_html_file, getReportIndex().getAbsolutePath()));
		} finally {
			if (writer != null) {
				writer.close();
			}
		}
	}
	/**
	 * Writes the html report for a specific consumer.  It lists all the producers (bundles that provide the api) that
	 * the consumer bundle references.
	 * 
	 * @param consumer consumer to write the report for
	 * @param producers a map of producer name to a {@link Producer} object
	 * @throws Exception
	 */
	protected void writeConsumerReport(Consumer consumer, Map producers) throws Exception {
		PrintWriter writer = null;
		File originhtml = null;
		try {
			File htmlroot = new File(getHtmlLocation(), consumer.name);
			if(!htmlroot.exists()) {
				htmlroot.mkdirs();
			}
			originhtml = new File(htmlroot, "index.html"); //$NON-NLS-1$
			if(!originhtml.exists()) {
				originhtml.createNewFile();
			}
			StringBuffer buffer = new StringBuffer();
			buffer.append(HTML_HEADER);
			buffer.append(OPEN_HTML).append(OPEN_HEAD).append(CONTENT_TYPE_META);
			buffer.append(REF_STYLE);
			buffer.append(REF_SCRIPT);
			buffer.append(OPEN_TITLE).append(getConsumerTitle(consumer.name)).append(CLOSE_TITLE); 
			buffer.append(CLOSE_HEAD); 
			buffer.append(OPEN_BODY); 
			buffer.append(OPEN_H3).append(getConsumerTitle(consumer.name)).append(CLOSE_H3);
			
			List producerNames = new ArrayList();
			producerNames.addAll(producers.keySet());
			Collections.sort(producerNames, compare);
			
			buffer.append(getReferencesTableHeader(SearchMessages.ConsumerReportConvertor_ConsumerListHeader, SearchMessages.UseReportConverter_bundle, true));
			Producer producer = null;
			File refereehtml = null;
			String link = null;
			for(Iterator iter = producerNames.iterator(); iter.hasNext();) {
				producer = (Producer)producers.get(iter.next());
				if(producer != null) {
					refereehtml = new File(getReportsRoot(), producer.name+File.separator+"index.html"); //$NON-NLS-1$
					link = extractLinkFrom(getReportsRoot(), refereehtml.getAbsolutePath());
					buffer.append(getReferenceTableEntry(producer.counts, link, producer.name, true));
				}
			}
			buffer.append(CLOSE_TABLE); 
			buffer.append(BR);
			
			buffer.append(OPEN_P).append("<a href=\"../index.html\">").append(SearchMessages.UseReportConverter_back_to_bundle_index).append(CLOSE_A).append(CLOSE_P); //$NON-NLS-1$ 
			buffer.append(W3C_FOOTER);
			
			writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(originhtml), IApiCoreConstants.UTF_8));;
			writer.println(buffer.toString());
			writer.flush();
		}
		catch(IOException ioe) {
			throw new Exception(NLS.bind(SearchMessages.ioexception_writing_html_file, originhtml.getAbsolutePath()), ioe);
		}
		finally {
			if (writer != null) {
				writer.close();
			}
		}
	}
	
	/**
	 * Writes the html report for a given producer.  The page lists all the types that are referenced by the
	 * parent consumer that are from the producer.
	 * <p>
	 * Called from {@link #writeConsumerReport(Consumer)}
	 * </p>
	 * @param producer producer to write the report for
	 * @throws Exception
	 */
	protected void writeProducerReport(Consumer parentConsumer, Producer producer) throws Exception {
		PrintWriter writer = null;
		File originhtml = null;
		try {
			File htmlroot = new Path(getHtmlLocation()).append(parentConsumer.name).append(producer.name).toFile();
			if(!htmlroot.exists()) {
				htmlroot.mkdirs();
			}
			originhtml = new File(htmlroot, "index.html"); //$NON-NLS-1$
			if(!originhtml.exists()) {
				originhtml.createNewFile();
			}
			StringBuffer buffer = new StringBuffer();
			buffer.append(HTML_HEADER);
			buffer.append(OPEN_HTML).append(OPEN_HEAD).append(CONTENT_TYPE_META);
			buffer.append(REF_STYLE);
			buffer.append(REF_SCRIPT);
			buffer.append(OPEN_TITLE).append(getProducerTitle(parentConsumer.name, producer.name)).append(CLOSE_TITLE); 
			buffer.append(CLOSE_HEAD); 
			buffer.append(OPEN_BODY); 
			buffer.append(OPEN_H3).append(getProducerTitle(parentConsumer.name, producer.name)).append(CLOSE_H3);
			String additional = getAdditionalReferencedTypeInformation();
			if(additional != null) {
				buffer.append(additional);
			}
			buffer.append(getReferencesTableHeader(SearchMessages.ConsumerReportConvertor_TypeListHeader, SearchMessages.UseReportConverter_referenced_type, false));
			
			List producerTypes = new ArrayList();
			producerTypes.addAll(producer.types.keySet());
			Collections.sort(producerTypes, compare);
			
			CountGroup counts = null;
			String link = null;
			File typefile = null;
			Type2 type = null;
			for (Iterator iter = producerTypes.iterator(); iter.hasNext();) {
				type = (Type2) producer.types.get(iter.next());
				counts = type.counts;
				
				String fqname = Signatures.getQualifiedTypeSignature((IReferenceTypeDescriptor) type.desc);
				typefile = new File(htmlroot, fqname+HTML_EXTENSION); 
				if(!typefile.exists()) {
					typefile.createNewFile();
				}
				link = extractLinkFrom(htmlroot, typefile.getAbsolutePath());
				buffer.append(getReferenceTableEntry(counts, link, fqname, false));
				writeTypePage(type.referencingMembers, type, typefile, fqname);
			}
			buffer.append(CLOSE_TABLE); 
			buffer.append(BR);
			
			buffer.append(OPEN_P).append("<a href=\"../index.html\">").append(NLS.bind(SearchMessages.ConsumerReportConvertor_BackLinkToConsumer, parentConsumer.name)).append(CLOSE_A).append(CLOSE_P); //$NON-NLS-1$ 
			buffer.append(W3C_FOOTER);
			
			writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(originhtml), IApiCoreConstants.UTF_8));;
			writer.println(buffer.toString());
			writer.flush();
		}
		catch(IOException ioe) {
			throw new Exception(NLS.bind(SearchMessages.ioexception_writing_html_file, originhtml.getAbsolutePath()));
		}
		finally {
			if (writer != null) {
				writer.close();
			}
		}
	}
	
}
