package org.jboss.ddoyle.ejbtmbug.reproducer.producer;

import java.util.Set;

import javax.ejb.Local;

@Local
public interface BatchProducerEjb {

	
	public abstract void processBatch(Set<String> batch);
		
	
}
