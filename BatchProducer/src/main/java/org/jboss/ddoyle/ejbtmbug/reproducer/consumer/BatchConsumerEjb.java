package org.jboss.ddoyle.ejbtmbug.reproducer.consumer;

import javax.ejb.Remote;

@Remote
public interface BatchConsumerEjb {
	
	public abstract void processRecord(String message);
	
}
