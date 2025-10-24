package org.eclipse.pde.spy.adapter.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.eclipse.core.internal.runtime.AdapterManager;
import org.eclipse.core.internal.runtime.IAdapterFactoryExt;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.pde.spy.adapter.tools.AdapterHelper;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;


@SuppressWarnings("restriction")
@Creatable
@Singleton
public class AdapterRepository {

	@Inject
	IExtensionRegistry extensionRegistry;
	
	Map<String, AdapterData> sourceTypeToAdapterDataMap = new HashMap<>();
	Map<String, AdapterData> destinationTypeToAdapterDataMap = new HashMap<>();
	List<IConfigurationElement> configEleme;

	public Collection<AdapterData> getAdapters() {
		
		if(!sourceTypeToAdapterDataMap.isEmpty()) {
			return sourceTypeToAdapterDataMap.values();
		}
		
		Map<String, List<IAdapterFactory>> factories = Collections.synchronizedMap(AdapterManager.getDefault().getFactories());
		AtomicReference<AdapterData> refAdapterData= new AtomicReference<>();
		factories.forEach( (k,v) -> {
	
			if (!sourceTypeToAdapterDataMap.containsKey(k))
			{
				AdapterData adapData = new AdapterData(AdapterElementType.SOURCE_TYPE);
				adapData.setSourceType(k);
				sourceTypeToAdapterDataMap.put(k, adapData);
			}	
			refAdapterData.set(sourceTypeToAdapterDataMap.get(k));
			final List<IConfigurationElement> configsForSourceType = getAdapterFactoryClassFromExtension(k);
			v.forEach(l -> {
				if( l instanceof IAdapterFactoryExt) {
					
					IAdapterFactoryExt adapfext = (IAdapterFactoryExt) l;
					AtomicReference<String> refClassName = new AtomicReference<>();
					for( String targetType :adapfext.getAdapterNames()) {
						AdapterData adapData = new AdapterData(AdapterElementType.DESTINATION_TYPE);
						adapData.setParent(refAdapterData.get());
						adapData.setDestinationType(targetType);
						destinationTypeToAdapterDataMap.put(targetType, adapData);
						refClassName.set("");
						configsForSourceType.forEach( config -> {
							for ( IConfigurationElement child :config.getChildren()) {
								String type = child.getAttribute(AdapterHelper.EXT_POINT_ATTR_TYPE);
								if( type.equals(targetType))
								{
									refClassName.set(config.getAttribute(AdapterHelper.EXT_POINT_ATTR_CLASS));
								}
							}
						});
						adapData.setAdapterClassName(refClassName.get());
						refAdapterData.get().getChildrenList().add(adapData);
					}
				}
			});
			
		});
		destinationTypeToAdapterDataMap.values().forEach( ad -> {
			String destType = ad.getDestinationType();
			Optional<AdapterData> found = sourceTypeToAdapterDataMap.values().stream().filter( ads -> ads.getSourceType().equals(destType)).findAny();
			if(found.isPresent()) {
				found.get().getChildrenList().forEach( adchild -> {
					if (((AdapterData)ad.getParent()).getSourceType().equals(adchild.getDestinationType())) {
						AdapterData adpd=new AdapterData(adchild);
						ad.getChildrenList().add(adpd);
						return;
					}
					ad.getChildrenList().add(adchild);	
				});
			}
		});
		return sourceTypeToAdapterDataMap.values();
	}
	
	
	public List<AdapterData> revertSourceToType(){
		return sourceTypeToAdapterDataMap.values().stream().flatMap(AdapterData::convertSourceToType)
			.collect(Collectors.toList());
	}
	
	
	public void clear() {
		sourceTypeToAdapterDataMap.clear();
		destinationTypeToAdapterDataMap.clear();
	}
	
	
	private List<IConfigurationElement>  getAdapterFactoryClassFromExtension(String sourceType) {
		if( configEleme ==null) {
			configEleme = Arrays.asList(extensionRegistry.getConfigurationElementsFor(AdapterHelper.EXT_POINT_ID));
		}
		return configEleme.stream().filter( config-> config.getAttribute(AdapterHelper.EXT_POINT_ATTR_ADAPTABLE_TYPE).equals(sourceType)).collect(Collectors.toList());
		
	}

}
