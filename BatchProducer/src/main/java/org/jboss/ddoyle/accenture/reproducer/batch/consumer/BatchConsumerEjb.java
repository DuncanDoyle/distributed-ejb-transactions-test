package org.jboss.ddoyle.accenture.reproducer.batch.consumer;

import javax.ejb.Remote;

@Remote
public interface BatchConsumerEjb {

	
	public abstract void processRecord(String message);
	
	
	
	
}
