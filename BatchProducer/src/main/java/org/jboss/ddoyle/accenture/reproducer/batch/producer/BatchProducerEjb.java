package org.jboss.ddoyle.accenture.reproducer.batch.producer;

import java.util.Set;

import javax.ejb.Local;

@Local
public interface BatchProducerEjb {

	
	public abstract void processBatch(Set<String> batch);
		
	
}
