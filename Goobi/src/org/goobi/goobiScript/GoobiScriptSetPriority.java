package org.goobi.goobiScript;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.goobi.beans.Process;
import org.goobi.beans.Step;
import org.goobi.production.enums.GoobiScriptResultType;
import org.goobi.production.enums.LogType;

import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.persistence.managers.ProcessManager;
import de.sub.goobi.persistence.managers.StepManager;
import lombok.extern.log4j.Log4j;

@Log4j
public class GoobiScriptSetPriority extends AbstractIGoobiScript implements IGoobiScript {

    @Override
    public boolean prepare(List<Integer> processes, String command, HashMap<String, String> parameters) {
        super.prepare(processes, command, parameters);

        if (parameters.get("priority") == null || parameters.get("priority").equals("")) {
            Helper.setFehlerMeldung("goobiScriptfield", "Missing parameter: ", "priority");
            return false;
        }

        String prio = parameters.get("priority").toLowerCase();
        if (!prio.equals("standard") && !prio.equals("high") && !prio.equals("higher") && !prio.equals("highest") && !prio.equals("correction")) {
            Helper.setFehlerMeldung("goobiScriptfield", "Wrong priority parameter",
                    "(only the following values are allowed: standard, high, higher, highest, correction)");
            return false;
        }

        // add all valid commands to list
        for (Integer i : processes) {
            GoobiScriptResult gsr = new GoobiScriptResult(i, command, username, starttime);
            resultList.add(gsr);
        }

        return true;
    }

    @Override
    public void execute() {
        SetPriorityThread et = new SetPriorityThread();
        et.start();
    }

    class SetPriorityThread extends Thread {
        @Override
        public void run() {
            // wait until there is no earlier script to be executed first
            while (gsm.getAreEarlierScriptsWaiting(starttime)) {
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    log.error("Problem while waiting for running GoobiScripts", e);
                }
            }

            String st = parameters.get("steptitle");
            if (st == null) {
                st = "";
            }

            int prio = 0;
            switch (parameters.get("priority").toLowerCase()) {
                case "high":
                    prio = 1;
                    break;
                case "higher":
                    prio = 2;
                    break;
                case "highest":
                    prio = 3;
                    break;
                case "correction":
                    prio = 10;
                    break;
                default:
                    prio = 0;
            }

            // execute all jobs that are still in waiting state
            synchronized (resultList) {
                for (GoobiScriptResult gsr : resultList) {
                    if (gsm.getAreScriptsWaiting(command) && gsr.getResultType() == GoobiScriptResultType.WAITING
                            && gsr.getCommand().equals(command)) {
                        Process p = ProcessManager.getProcessById(gsr.getProcessId());
                        gsr.setProcessTitle(p.getTitel());
                        gsr.setResultType(GoobiScriptResultType.RUNNING);
                        gsr.updateTimestamp();

                        for (Iterator<Step> iterator = p.getSchritteList().iterator(); iterator.hasNext();) {
                            Step s = iterator.next();
                            if (st.length() == 0 || s.getTitel().equals(st)) {
                                s.setPrioritaet(prio);
                                try {
                                    StepManager.saveStep(s);
                                    Helper.addMessageToProcessLog(p.getId(), LogType.DEBUG,
                                            "Changed priority of step '" + s.getTitel() + "' to '" + s.getPrioritaet() + "' using GoobiScript.",
                                            username);
                                    log.info("Changed priority of step '" + s.getTitel() + "' to '" + s.getPrioritaet()
                                            + "' using GoobiScript for process with ID " + p.getId());
                                    gsr.setResultMessage(
                                            "Changed priority of step '" + s.getTitel() + "' to '" + s.getReihenfolge() + "' successfully.");
                                    gsr.setResultType(GoobiScriptResultType.OK);
                                } catch (DAOException e) {
                                    log.error("goobiScriptfield" + "Error while saving process: " + p.getTitel(), e);
                                    gsr.setResultMessage(
                                            "Error while changing the priority of step '" + s.getTitel() + "' to '" + s.getReihenfolge() + "'.");
                                    gsr.setResultType(GoobiScriptResultType.ERROR);
                                    gsr.setErrorText(e.getMessage());
                                }
                            }
                        }
                        gsr.updateTimestamp();
                    }
                }
            }
        }
    }
}
