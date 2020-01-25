package org.goobi.api.mq;

import java.util.Date;
import java.util.stream.IntStream;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.ConfigurationException;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQPrefetchPolicy;
import org.apache.activemq.RedeliveryPolicy;
import org.goobi.beans.LogEntry;
import org.goobi.beans.Step;
import org.goobi.production.enums.LogType;

import com.google.gson.Gson;

import de.sub.goobi.helper.HelperSchritte;
import de.sub.goobi.helper.JwtHelper;
import de.sub.goobi.helper.enums.StepStatus;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.persistence.managers.ExternalMQManager;
import de.sub.goobi.persistence.managers.ProcessManager;
import de.sub.goobi.persistence.managers.StepManager;
import lombok.extern.log4j.Log4j;

@Log4j
public class GoobiCommandListener {
    Gson gson = new Gson();

    private ActiveMQConnection conn;

    public void register(String username, String password) throws JMSException {
        ActiveMQConnectionFactory connFactory = new ActiveMQConnectionFactory();
        conn = (ActiveMQConnection) connFactory.createConnection(username, password);
        ActiveMQPrefetchPolicy prefetchPolicy = new ActiveMQPrefetchPolicy();
        prefetchPolicy.setAll(0);
        conn.setPrefetchPolicy(prefetchPolicy);
        RedeliveryPolicy policy = conn.getRedeliveryPolicy();
        policy.setMaximumRedeliveries(0);
        final Session sess = conn.createSession(false, Session.CLIENT_ACKNOWLEDGE);
        final Destination dest = sess.createQueue(StartQueueBrokerListener.COMMAND_QUEUE);

        final MessageConsumer cons = sess.createConsumer(dest);

        Runnable run = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Message message = cons.receive();
                        // check command and if the token allows this.
                        String strMessage = null;
                        if (message instanceof TextMessage) {
                            TextMessage tm = (TextMessage) message;
                            strMessage = tm.getText();
                        }
                        if (message instanceof BytesMessage) {
                            BytesMessage bm = (BytesMessage) message;
                            byte[] bytes = new byte[(int) bm.getBodyLength()];
                            bm.readBytes(bytes);
                            strMessage = new String(bytes);
                        }
                        CommandTicket t = gson.fromJson(strMessage, CommandTicket.class);

                        handleCommandTicket(t);
                        message.acknowledge();
                    } catch (JMSException e) {
                        // TODO Auto-generated catch block
                        log.error(e);
                    }
                }
            }
        };
        Thread t = new Thread(run);
        t.setDaemon(true);

        conn.start();
        t.start();
    }

    private void handleCommandTicket(CommandTicket t) {
        String command = t.getCommand();
        String token = t.getJwt();
        Integer stepId = t.getStepId();
        Integer processId = t.getProcessId();
        switch (command) {
            case "changeStep":
                try {
                    if (JwtHelper.verifyChangeStepToken(token, stepId)) {
                        // change step
                        Step step = StepManager.getStepById(stepId);
                        String newStatus = t.getNewStatus();
                        switch (newStatus) {
                            case "error":
                                step.setBearbeitungsstatusEnum(StepStatus.ERROR);
                                StepManager.saveStep(step);
                                break;
                            case "done":
                                //TODO: Write to DB with date.
                                for(String scriptName : t.getScriptNames()) {
                                    ExternalMQManager.insertResult(new ExternalCommandResult(t.getProcessId(), t.getStepId(), scriptName));
                                }
                                new HelperSchritte().CloseStepObjectAutomatic(step);
                                break;
                        }
                    }
                } catch (ConfigurationException | DAOException e) {
                    log.error(e);
                }
                break;
            case "addToProcessLog":
                try {
                    if (JwtHelper.verifyChangeStepToken(token, stepId)) {
                        // add to process log
                        LogEntry entry = LogEntry.build(processId)
                                .withCreationDate(new Date())
                                .withType(LogType.getByTitle(t.getLogType()))
                                .withUsername(t.getIssuer())
                                .withContent(t.getContent());
                        ProcessManager.saveLogEntry(entry);
                    }
                } catch (ConfigurationException e) {
                    log.error(e);
                }
                break;
            default:
                break;
        }
    }
}
