package org.goobi.managedbeans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.inject.Named;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.commons.lang.StringUtils;
import org.apache.deltaspike.core.api.scope.WindowScoped;
import org.goobi.api.mq.TaskTicket;

import com.google.gson.Gson;

import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.persistence.managers.MQResultManager;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

/**
 * 
 * This bean can be used to display the current state of the goobi_fast and goobi_slow queues. The bean provides methods to show all active tickets
 * and remove a ticket or clear the queue.
 *
 */

@Named
@WindowScoped
@Log4j2
public class MessageQueueBean extends BasicBean implements Serializable {

    private static final long serialVersionUID = 9201515793444130154L;

    private Gson gson = new Gson();

    private ActiveMQConnection connection;
    private QueueSession queueSession;

    @Getter
    @Setter
    private String mode = "waiting";

    @Getter
    private boolean messageBrokerStart;

    @Getter
    @Setter
    private String messageType;

    @Getter
    @Setter
    private String queueType;

    public MessageQueueBean() {
        this.initMessageBrokerStart();

        if (this.messageBrokerStart) {

            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory();

            try {
                connection = (ActiveMQConnection) connectionFactory.createConnection(ConfigurationHelper.getInstance().getMessageBrokerUsername(),
                        ConfigurationHelper.getInstance().getMessageBrokerPassword());
                queueSession = connection.createQueueSession(false, Session.CLIENT_ACKNOWLEDGE);
            } catch (JMSException e) {
                log.error(e);
                e.printStackTrace();
            }
            paginator = new DatabasePaginator(null, null, new MQResultManager(), "queue.xhtml");
        }
    }

    public Map<String, Integer> getFastQueueContent() {
        Map<String, Integer> fastQueueContent = new TreeMap<>();
        if (ConfigurationHelper.getInstance().isStartInternalMessageBroker()) {
            try {
                connection.start();
                Queue queue = queueSession.createQueue("goobi_fast");
                QueueBrowser browser = queueSession.createBrowser(queue);
                Enumeration<?> messagesInQueue = browser.getEnumeration();
                while (messagesInQueue.hasMoreElements()) {
                    ActiveMQTextMessage queueMessage = (ActiveMQTextMessage) messagesInQueue.nextElement();

                    String type = queueMessage.getStringProperty("JMSType");
                    if (fastQueueContent.containsKey(type)) {
                        fastQueueContent.put(type, fastQueueContent.get(type) + 1);
                    } else {
                        fastQueueContent.put(type, 1);
                    }
                }
                browser.close();
                connection.stop();
            } catch (JMSException e) {
                log.error(e);
            }
        }
        return fastQueueContent;
    }

    public Map<String, Integer> getSlowQueueContent() {
        Map<String, Integer> fastQueueContent = new TreeMap<>();
        if (ConfigurationHelper.getInstance().isStartInternalMessageBroker()) {
            try {
                connection.start();
                Queue queue = queueSession.createQueue("goobi_slow");
                QueueBrowser browser = queueSession.createBrowser(queue);
                Enumeration<?> messagesInQueue = browser.getEnumeration();
                while (messagesInQueue.hasMoreElements()) {
                    ActiveMQTextMessage queueMessage = (ActiveMQTextMessage) messagesInQueue.nextElement();

                    String type = queueMessage.getStringProperty("JMSType");
                    if (fastQueueContent.containsKey(type)) {
                        fastQueueContent.put(type, fastQueueContent.get(type) + 1);
                    } else {
                        fastQueueContent.put(type, 1);
                    }
                }
                browser.close();
                connection.stop();
            } catch (JMSException e) {
                log.error(e);
            }
        }
        return fastQueueContent;
    }

    public void initMessageBrokerStart() {
        this.messageBrokerStart = ConfigurationHelper.getInstance().isStartInternalMessageBroker();
    }

    /**
     * Delete a single message from the goobi_slow queue
     * 
     * @param ticket to delete
     */

    /**
     * Get a list of all active messages in the goobi_slow queue
     */

    public List<TaskTicket> getActiveSlowQueryMesssages() {

        List<TaskTicket> answer = new ArrayList<>();
        if (!ConfigurationHelper.getInstance().isStartInternalMessageBroker()) {
            return answer;
        }
        if (StringUtils.isNotBlank(messageType)) {
            try {
                connection.start();
                Queue queue = queueSession.createQueue("goobi_slow");
                QueueBrowser browser = queueSession.createBrowser(queue, "JMSType = '" + messageType + "'");
                Enumeration<?> messagesInQueue = browser.getEnumeration();
                while (messagesInQueue.hasMoreElements() && answer.size() < 100) {
                    ActiveMQTextMessage queueMessage = (ActiveMQTextMessage) messagesInQueue.nextElement();
                    TaskTicket ticket = gson.fromJson(queueMessage.getText(), TaskTicket.class);
                    ticket.setMessageId(queueMessage.getJMSMessageID());

                    answer.add(ticket);
                }
                browser.close();
                connection.stop();
            } catch (JMSException e) {
                log.error(e);
            }
        }
        return answer;
    }

    /**
     * Remove all active messages of a given type from the queue
     * 
     */
    public void clearQueue() {
        if (StringUtils.isNotBlank(messageType)) {
            try {
                Queue queue = queueSession.createQueue(queueType);
                MessageConsumer consumer = queueSession.createConsumer(queue, "JMSType='" + messageType + "'");
                connection.start();
                Message message = consumer.receiveNoWait();
                while (message != null) {
                    message.acknowledge();
                    message = consumer.receiveNoWait();
                    // TODO get step, set step status to open
                    // TODO write cancel message to process log

                }
                connection.stop();
            } catch (JMSException e) {
                log.error(e);
            }
        }

    }

    /**
     * Get a list of all active messages in the goobi_fast queue
     */

    public List<TaskTicket> getActiveFastQueryMesssages() {
        List<TaskTicket> answer = new ArrayList<>();
        if (!ConfigurationHelper.getInstance().isStartInternalMessageBroker()) {
            return answer;
        }
        if (StringUtils.isNotBlank(messageType)) {
            try {
                connection.start();
                Queue queue = queueSession.createQueue("goobi_fast");

                QueueBrowser browser = queueSession.createBrowser(queue, "JMSType = '" + messageType + "'");
                Enumeration<?> messagesInQueue = browser.getEnumeration();
                // get up to 100 messages
                while (messagesInQueue.hasMoreElements() && answer.size() < 100) {
                    ActiveMQTextMessage queueMessage = (ActiveMQTextMessage) messagesInQueue.nextElement();
                    TaskTicket ticket = gson.fromJson(queueMessage.getText(), TaskTicket.class);
                    ticket.setMessageId(queueMessage.getJMSMessageID());

                    answer.add(ticket);
                }
                browser.close();
                connection.stop();
            } catch (JMSException e) {
                log.error(e);
            }
        }

        return answer;
    }

    /**
     * Delete a single message from the queue
     * 
     * @param ticket
     */

    public void deleteMessage(TaskTicket ticket) {
        try {
            connection.start();
            Queue queue = queueSession.createQueue(queueType);
            QueueReceiver receiver = queueSession.createReceiver(queue, "JMSMessageID='" + ticket.getMessageId() + "'");
            Message message = receiver.receiveNoWait();
            //
            //            //            MessageConsumer consumer =
            //            //                    queueSession.createConsumer(queue, "JMSMessageID='" + ticket.getMessageId().replace("ID:", "").replace(":1:1:1:1", "")+"'");
            //            MessageConsumer consumer = queueSession.createConsumer(queue, "JMSMessageID='" + ticket.getMessageId() + "'");
            //            //            MessageConsumer consumer =
            //            //                    queueSession.createConsumer(queue, "JMSType='" + messageType + "' AND processid='" + ticket.getProcessId() + "'");
            //            connection.start();
            //                        Message message = consumer.receiveNoWait();
            //            //            Message message = consumer.receive(2000l);
            //
            if (message != null) {
                message.acknowledge();
            }
            connection.stop();

            // TODO get step, set step status to open
            // TODO write cancel message to process log
        } catch (JMSException e) {
            log.error(e);
        }
    }

}
