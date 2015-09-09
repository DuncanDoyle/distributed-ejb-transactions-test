package org.jboss.ddoyle.ejbtmbug.reproducer.web.servlet;

import java.io.IOException;
import java.io.Writer;

import javax.annotation.Resource;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class StartReproducerServlet
 */
@WebServlet(name="StartReproducerServlet", urlPatterns="/start")
public class StartReproducerServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	//Inject JCA JMS ConnectionFactory (which is pooled).
	@Resource(mappedName="java:/JmsXA")
	private ConnectionFactory cf;
	
	private static final String NR_OF_MESSAGES_PROPERTY_NAME = "messages.number";
	
	private static final String DEFAULT_NR_OF_MESSAGES = "15";
			
	private final int nrOfMessages; 
	
    /**
     * Default constructor. 
     */
    public StartReproducerServlet() {
    	String configuredNumberOfMessages = System.getProperty(NR_OF_MESSAGES_PROPERTY_NAME);
    	if (configuredNumberOfMessages == null || "".equals(configuredNumberOfMessages)) {
    		configuredNumberOfMessages = DEFAULT_NR_OF_MESSAGES;
    	}
    	nrOfMessages = Integer.parseInt(configuredNumberOfMessages);
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Writer writer = response.getWriter();
		writer.write("Sending message to reproducer queue.");
		writer.flush();
		
		Connection connection;
		try {
			connection = cf.createConnection("guest", "jboss@01");
			Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			Queue batchQueue = session.createQueue("BatchQueue");
			MessageProducer producer = session.createProducer(batchQueue);
			for (int counter = 0; counter < nrOfMessages; counter++) {
				Message textMessage = session.createTextMessage("My simple message.");
				producer.send(textMessage);
			}
			session.close();
			connection.close();
		} catch (JMSException jmse) {
			throw new RuntimeException(jmse);
		}
		
		
		
		
		
		
		
		writer.write("Message sent successfully");
		writer.flush();
		writer.close();
		
		
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

}
