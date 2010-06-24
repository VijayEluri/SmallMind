package org.smallmind.cloud.transport.messaging;

import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.NamingException;
import org.smallmind.quorum.pool.ConnectionPoolException;

public class MessagingReceiver implements MessageListener {

   private MessageTarget messageTarget;
   private QueueConnection queueConnection;
   private QueueSession queueSession;
   private QueueReceiver queueReceiver;
   private boolean stopped = false;

   public MessagingReceiver (MessageTarget messageTarget, MessagingConnectionDetails messagingConnectionDetails)
      throws ConnectionPoolException, NamingException, JMSException {

      Context javaEnvironment;
      Queue queue;
      QueueConnectionFactory queueConnectionFactory;

      this.messageTarget = messageTarget;

      javaEnvironment = (Context)messagingConnectionDetails.getContextPool().getConnection();
      try {
         queue = (Queue)javaEnvironment.lookup(messagingConnectionDetails.getDestinationName());
         queueConnectionFactory = (QueueConnectionFactory)javaEnvironment.lookup(messagingConnectionDetails.getConnectionFactoryName());
      }
      finally {
         javaEnvironment.close();
      }

      queueConnection = queueConnectionFactory.createQueueConnection(messagingConnectionDetails.getUserName(), messagingConnectionDetails.getPassword());
      queueSession = queueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

      queueReceiver = (messagingConnectionDetails.getServiceSelector() == null) ? queueSession.createReceiver(queue) : queueSession.createReceiver(queue, MessagingConnectionDetails.SELECTION_PROPERTY + "='" + messagingConnectionDetails.getServiceSelector() + "'");
      queueReceiver.setMessageListener(this);

      queueConnection.start();
   }

   public synchronized void close () {

      stopped = true;

      try {
         queueConnection.stop();

         queueReceiver.close();
         queueSession.close();
         queueConnection.close();
      }
      catch (JMSException jmsException) {
         messageTarget.logError(jmsException);
      }
   }

   public synchronized void onMessage (Message message) {

      Message responseMessage;
      QueueSender queueSender;

      if (!stopped) {
         try {
            try {
               responseMessage = messageTarget.handleMessage(queueSession, message);
            }
            catch (JMSException jmsException) {
               throw jmsException;
            }
            catch (Exception exception) {
               responseMessage = queueSession.createObjectMessage(exception);
               responseMessage.setBooleanProperty(MessagingConnectionDetails.EXCEPTION_PROPERTY, true);
            }

            responseMessage.setJMSDeliveryMode(DeliveryMode.NON_PERSISTENT);

            queueSender = queueSession.createSender((Queue)message.getJMSReplyTo());
            queueSender.send(responseMessage);
            queueSender.close();
         }
         catch (JMSException jmsException) {
            messageTarget.logError(jmsException);
         }
      }
   }
}
