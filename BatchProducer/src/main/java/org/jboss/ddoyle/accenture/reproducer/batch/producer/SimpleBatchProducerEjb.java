package org.jboss.ddoyle.accenture.reproducer.batch.producer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.transaction.Status;
import javax.transaction.UserTransaction;

import org.jboss.ddoyle.accenture.reproducer.batch.consumer.BatchConsumerEjb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Session Bean implementation class SimpleBatchProducerEjb
 */
@Stateless
public class SimpleBatchProducerEjb implements BatchProducerEjb {

	private static final Logger LOGGER = LoggerFactory.getLogger(SimpleBatchProducerEjb.class);

	private Context ejbRootNamingContext;
	
	private BatchConsumerEjb consumer;

	@Resource(mappedName = "java:/jboss/datasources/AccenturePragueDS")
	private DataSource ds;

	
	@Resource
	private EJBContext ejbCtx;
	/**
	 * Default constructor.
	 */
	public SimpleBatchProducerEjb() {
	}
	
	@PostConstruct
	private void init() {
		LOGGER.info("Initializing EJB.");
		consumer = getBatchConsumerEjbProxy();
	}
	
	@PreDestroy
	private void destroy() {
		if (ejbRootNamingContext != null) {
			try {
				ejbRootNamingContext.close();
			} catch (NamingException ne) {
				//Not much we can do here. Just log.
				LOGGER.error("Unable to close EJB Root Naming Context.");
			}
		}
	}

	// Always start a new transaction.
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override
	public void processBatch(Set<String> batch) {
		LOGGER.info("Processing batch.");
		int counter = 0;
		for (String nextRecord : batch) {
			counter++;
			
			// go through the batch, write every record to DB and do a remote call.

			Connection connection = null;
			try {
				connection = ds.getConnection();
				PreparedStatement prepStatement = connection.prepareStatement("INSERT INTO batch_producer (record_id) VALUES (?)");
				prepStatement.setString(1, nextRecord);
				prepStatement.execute();

			} catch (SQLException sqle) {
				String errorMessage = "Error executing SQL prepared statement.";
				LOGGER.error(errorMessage, sqle);
				throw new RuntimeException(errorMessage, sqle);
			} finally {
				if (connection != null) {
					try {
						connection.close();
					} catch (SQLException sqle) {
						// Not much we can do here. Log the exception and move on.
						LOGGER.warn("Unable to close connection.", sqle);
					}
				}
			}
			consumer.processRecord(nextRecord);
		}
		LOGGER.info("Batch processed.");
	}


	private BatchConsumerEjb getBatchConsumerEjbProxy() {
		LOGGER.info("Retrieving remote EJB proxy.");
		BatchConsumerEjb ejb = null;
		final Hashtable jndiProps = new Hashtable();
		// setup the ejb: namespace URL factory
		//props.put("jboss.naming.client.ejb.context", true);

		jndiProps.put("jboss.naming.client.ejb.context", true);
		jndiProps.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
		
		// create the InitialContext
		Context context = null;
		try {
			context = new javax.naming.InitialContext(jndiProps);
			//ejb = (BatchConsumerEjb) context.lookup("ejb:/BatchConsumer-1.0.0-SNAPSHOT/SimpleBatchConsumerEjb!org.jboss.ddoyle.accenture.reproducer.batch.consumer.BatchConsumerEjb");
			ejb = (BatchConsumerEjb) context.lookup("ejb:BatchConsumerEAR-1.0.0-SNAPSHOT/BatchConsumer-1.0.0-SNAPSHOT/SimpleBatchConsumerEjb!org.jboss.ddoyle.accenture.reproducer.batch.consumer.BatchConsumerEjb");
			
			//ejb.processRecord("Bla");
		} catch (NamingException ne) {
			String message = "Error looking up remote EJB.";
			LOGGER.error(message, ne);
			throw new RuntimeException(message, ne);
		} finally {
			if (context != null) {
				//try {
				//	context.close();
				//} catch (NamingException e) {
					// TODO Auto-generated catch block
				//	e.printStackTrace();
				//}
			}
		}
		return ejb;
	}
	
	
	/*
	 * Using pure JNDI against remote EJB naming context. Client application manages connections, etc.
	 * 
	 *
	 */
	/*
	private BatchConsumerEjb getBatchConsumerEjbProxy() {
		Properties p = new Properties();
		p.put("remote.connections", "node1");
		p.put("remote.connection.node1.host", "127.0.0.3"); // the host, replace if necessary
		p.put("remote.connection.node1.port", "4447"); // the default remoting port, replace if necessary
		p.put("remote.connectionprovider.create.options.org.xnio.Options.SSL_ENABLED", "false"); // the server defaults to SSL_ENABLED=false

		// these 3 lines below are not necessary, if security-realm is removed from remoting-connector
		p.put("remote.connection.node1.connect.options.org.xnio.Options.SASL_POLICY_NOANONYMOUS", "false");
		//p.put("remote.connection.node1.username", "guest");
		//p.put("remote.connection.node1.password", "jboss@01");

		// if the ejb is clustered, add the following lines...otherwise the client will log an
		// exception (JBREM000200) even though the remote ejb call completes successfully
		// p.put("remote.clusters", "ejb");
		// p.put("remote.cluster.ejb.connect.options.org.xnio.Options.SASL_POLICY_NOANONYMOUS", "false");
		// p.put("remote.cluster.ejb.username", "user");
		// p.put("remote.cluster.ejb.password", "password");

		p.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
		p.put("org.jboss.ejb.client.scoped.context", true); // enable scoping here

		Context context = null;
		try {
			context = new InitialContext(p);
			ejbRootNamingContext = (Context) context.lookup("ejb:");
			//final String consumerBeanJndiName = "/BatchConsumer-1.0.0-SNAPSHOT/SimpleBatchConsumerEjb!org.jboss.ddoyle.accenture.reproducer.batch.consumer.BatchConsumerEjb";
			final String consumerBeanJndiName = "BatchConsumerEAR-1.0.0-SNAPSHOT/BatchConsumer-1.0.0-SNAPSHOT/SimpleBatchConsumerEjb!org.jboss.ddoyle.accenture.reproducer.batch.consumer.BatchConsumerEjb";
			//final String consumerBeanJndiName = "SimpleBatchConsumerEjb!org.jboss.ddoyle.accenture.reproducer.batch.consumer.BatchConsumerEjb";
			//BatchConsumerEjb ejbProxy = (BatchConsumerEjb) ejbRootNamingContext.lookup(consumerBeanJndiName);
			
			BatchConsumerEjb ejbProxy = (BatchConsumerEjb) ejbRootNamingContext.lookup(consumerBeanJndiName);
			
			LOGGER.info("BatchConsumer EJB proxy class:" + ejbProxy.getClass());
			return (BatchConsumerEjb) ejbProxy;
		} catch (NamingException ne) {
			String message = "Error looking up remote EJB.";
			LOGGER.error(message, ne);
			throw new RuntimeException(message, ne);
		} finally {
			try {
				if (context != null) {
					context.close();
				}
			} catch (Exception e) {
			}
		}

	}
	*/
	

}
