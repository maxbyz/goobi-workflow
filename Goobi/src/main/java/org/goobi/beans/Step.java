package org.goobi.beans;

/**
 * This file is part of the Goobi Application - a Workflow tool for the support of mass digitization.
 * 
 * Visit the websites for more information.
 *     		- https://goobi.io
 * 			- https://www.intranda.com
 * 			- https://github.com/intranda/goobi-workflow
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 * Linking this library statically or dynamically with other modules is making a combined work based on this library. Thus, the terms and conditions
 * of the GNU General Public License cover the whole combination. As a special exception, the copyright holders of this library give you permission to
 * link this library with independent modules to produce an executable, regardless of the license terms of these independent modules, and to copy and
 * distribute the resulting executable under terms of your choice, provided that you also meet, for each linked independent module, the terms and
 * conditions of the license of that module. An independent module is a module which is not derived from or based on this library. If you modify this
 * library, you may extend this exception to your version of the library, but you are not obliged to do so. If you do not wish to do so, delete this
 * exception statement from your version.
 */
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.goobi.api.mail.SendMail;
import org.goobi.api.mq.AutomaticThumbnailHandler;
import org.goobi.api.mq.QueueType;
import org.goobi.api.mq.TaskTicket;
import org.goobi.api.mq.TicketGenerator;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.enums.StepEditType;
import de.sub.goobi.helper.enums.StepStatus;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.persistence.managers.ProcessManager;
import de.sub.goobi.persistence.managers.UserManager;
import de.sub.goobi.persistence.managers.UsergroupManager;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

public class Step implements Serializable, DatabaseObject, Comparable<Step> {
    private static final long serialVersionUID = 6831844584239811846L;

    @Getter
    @Setter
    private Integer id;
    @Getter
    @Setter
    private String titel;
    @Getter
    @Setter
    private Integer prioritaet;
    @Getter
    @Setter
    private Integer reihenfolge;
    @Getter (AccessLevel.PRIVATE)
    @Setter (AccessLevel.PRIVATE)
    private Integer bearbeitungsstatus;
    @Getter
    @Setter
    private Date bearbeitungszeitpunkt;
    @Getter
    @Setter
    private Date bearbeitungsbeginn;
    @Getter
    @Setter
    private Date bearbeitungsende;
    @Getter (AccessLevel.PRIVATE)
    @Setter (AccessLevel.PRIVATE)
    private Integer editType;
    private User bearbeitungsbenutzer;
    // temporär
    @Getter
    @Setter
    private Integer userId;

    @Getter
    @Setter
    private short homeverzeichnisNutzen;

    @Getter
    @Setter
    private boolean typMetadaten = false;
    @Getter
    @Setter
    private boolean typAutomatisch = false;
    @Getter
    @Setter
    private boolean typAutomaticThumbnail = false;
    @Getter
    @Setter
    private String automaticThumbnailSettingsYaml;
    @Getter
    @Setter
    private boolean typImportFileUpload = false;
    @Getter
    @Setter
    private boolean typExportRus = false;
    @Getter
    @Setter
    private boolean typImagesLesen = false;
    @Getter
    private boolean typImagesSchreiben = false;
    @Getter
    @Setter
    private boolean typExportDMS = false;
    @Getter
    @Setter
    private boolean typBeimAnnehmenModul = false;
    @Getter
    @Setter
    private boolean typBeimAnnehmenAbschliessen = false;
    @Getter
    @Setter
    private boolean typBeimAnnehmenModulUndAbschliessen = false;
    @Getter
    @Setter
    private boolean typScriptStep = false;
    @Getter
    @Setter
    private String scriptname1;
    @Getter
    @Setter
    private String typAutomatischScriptpfad;
    @Getter
    @Setter
    private String scriptname2;
    @Getter
    @Setter
    private String typAutomatischScriptpfad2;
    @Getter
    @Setter
    private String scriptname3;
    @Getter
    @Setter
    private String typAutomatischScriptpfad3;
    @Getter
    @Setter
    private String scriptname4;
    @Getter
    @Setter
    private String typAutomatischScriptpfad4;
    @Getter
    @Setter
    private String scriptname5;
    @Getter
    @Setter
    private String typAutomatischScriptpfad5;
    @Getter
    @Setter
    private String typModulName;
    @Getter
    @Setter
    private boolean typBeimAbschliessenVerifizieren = false;
    private Boolean batchStep = false;

    @Getter
    @Setter
    transient boolean batchSize;

    @Getter
    @Setter
    private boolean httpStep;
    @Getter
    @Setter
    private String httpUrl;
    @Getter
    @Setter
    private String httpMethod;
    @Getter
    @Setter
    private String[] possibleHttpMethods = new String[] { "POST", "PUT", "PATCH", "GET" };
    @Getter
    @Setter
    private String httpJsonBody;
    @Getter
    @Setter
    private boolean httpCloseStep;
    @Getter
    @Setter
    private boolean httpEscapeBodyJson;

    @Setter
    private Process prozess;
    // temporär
    @Getter
    @Setter
    private Integer processId;

    @Setter
    private List<ErrorProperty> eigenschaften;
    @Setter
    private List<User> benutzer;
    @Setter
    private List<Usergroup> benutzergruppen;
    @Getter
    @Setter
    private boolean panelAusgeklappt = false;
    @Getter
    @Setter
    private boolean selected = false;
    private SimpleDateFormat formatter = new SimpleDateFormat("yyyymmdd");

    @Getter
    @Setter
    private String stepPlugin;
    @Getter
    @Setter
    private String validationPlugin;
    @Getter
    @Setter
    private boolean delayStep;

    @Getter
    @Setter
    private boolean updateMetadataIndex;

    @Getter
    @Setter
    private boolean generateDocket = false;

    @Getter
    private QueueType messageQueue;

    //    @Getter
    //    @Setter
    //    private String messageId;

    public Step() {
        this.titel = "";
        this.eigenschaften = new ArrayList<>();
        this.benutzer = new ArrayList<>();
        this.benutzergruppen = new ArrayList<>();
        this.prioritaet = 0;
        this.reihenfolge = 0;
        this.httpJsonBody = "";
        setBearbeitungsstatusEnum(StepStatus.LOCKED);
    }

    // This constructor is needed when creating a new Step
    public Step(Process process) {
        this();
        this.prozess = process;

        // Look for the next available order number
        List<Step> steps = process.getSchritte();
        if (steps.size() == 0) {
            this.reihenfolge = 1;
            return;
        }

        // Here the list of steps is NOT empty
        // Before iterating over all steps, the order of the first step is assumed as the highest one
        int maximumOrder = steps.get(0).getReihenfolge();
        // After that a higher one can be found. Here the index begins at 1.
        Step currentStep;
        for (int i = 1; i < steps.size(); i++) {
            currentStep = steps.get(i);
            if (currentStep.getReihenfolge() > maximumOrder) {
                maximumOrder = currentStep.getReihenfolge();
            }
        }

        // Maximum order + 1 cannot be in use until now
        this.reihenfolge = maximumOrder + 1;
    }

    /*
     * Getter und Setter
     */

    public String getBearbeitungsbeginnAsFormattedString() {
        return Helper.getDateAsFormattedString(this.bearbeitungsbeginn);
    }

    public String getStartDate() {
        if (this.bearbeitungsbeginn != null) {
            return this.formatter.format(this.bearbeitungsbeginn);
        }
        return "";
    }

    public String getEndDate() {
        if (this.bearbeitungsende != null) {
            return this.formatter.format(this.bearbeitungsende);
        }
        return "";
    }

    public String getBearbeitungsendeAsFormattedString() {
        return Helper.getDateAsFormattedString(this.bearbeitungsende);
    }

    public JSONObject getAutoThumbnailSettingsJSON() {
        //new JSONObject("{'Master':true,'Media':true, 'Sizes':[800] }");
        Yaml yaml= new Yaml();
        @SuppressWarnings("unchecked")
        Map<String,Object> map= (Map<String, Object>) yaml.load(this.automaticThumbnailSettingsYaml);
        return new JSONObject(map);
    }

    public void submitAutomaticThumbnailTicket() {
        try {
            TaskTicket ticket = new TaskTicket(AutomaticThumbnailHandler.HANDLERNAME);
            ticket.setStepId(this.id);
            ticket.setProcessId(this.getProcessId());
            ticket.setStepName(this.titel);
            if (!ConfigurationHelper.getInstance().isStartInternalMessageBroker()) {
                AutomaticThumbnailHandler handler = new AutomaticThumbnailHandler();
                handler.call(ticket);
            }else {
                TicketGenerator.submitInternalTicket(ticket, QueueType.SLOW_QUEUE, this.titel, this.getProcessId());
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * set editType to specific value from {@link StepEditType}
     * 
     * @param inType as {@link StepEditType}
     */
    public void setEditTypeEnum(StepEditType inType) {
        this.editType = inType.getValue();
    }

    /**
     * get editType as {@link StepEditType}
     * 
     * @return current bearbeitungsstatus
     */
    public StepEditType getEditTypeEnum() {
        return StepEditType.getTypeFromValue(this.editType);
    }

    /**
     * set bearbeitungsstatus to specific value from {@link StepStatus}
     * 
     * @param inStatus as {@link StepStatus}
     */
    public void setBearbeitungsstatusEnum(StepStatus inStatus) {
        this.bearbeitungsstatus = inStatus.getValue();
    }

    /**
     * get bearbeitungsstatus as {@link StepStatus}
     * 
     * @return current bearbeitungsstatus
     */
    public StepStatus getBearbeitungsstatusEnum() {
        return StepStatus.getStatusFromValue(this.bearbeitungsstatus);
    }

    public String getBearbeitungszeitpunktAsFormattedString() {
        return Helper.getDateAsFormattedString(this.bearbeitungszeitpunkt);
    }

    // a parameter is given here (even if not used) because jsf expects setter convention
    public void setBearbeitungszeitpunktNow(int in) {
        this.bearbeitungszeitpunkt = new Date();
    }

    public int getBearbeitungszeitpunktNow() {
        return 1;
    }

    public User getBearbeitungsbenutzer() {
        if (bearbeitungsbenutzer == null && userId != null) {
            try {
                bearbeitungsbenutzer = UserManager.getUserById(userId);
            } catch (DAOException e) {
            }
        }
        return this.bearbeitungsbenutzer;
    }

    public void setBearbeitungsbenutzer(User bearbeitungsbenutzer) {
        this.bearbeitungsbenutzer = bearbeitungsbenutzer;
        if (bearbeitungsbenutzer != null) {
            userId = bearbeitungsbenutzer.getId();
        } else {
            userId = null;
        }
    }

    /*
     * if you change anything in the logic of priorities make sure that you catch dependencies on this system which are not directly related to
     * priorities
     */
    public Boolean isCorrectionStep() {
        return (this.prioritaet == 10);
    }

    public void setCorrectionStep() {
        this.prioritaet = 10;
    }

    public Process getProzess() {
        if (prozess == null) {
            lazyLoad();
        }
        return this.prozess;
    }

    public String getTitelLokalisiert() {
        String translatedTitle = Helper.getTranslation("stepname_" + this.titel);
        return translatedTitle.startsWith("stepname_") ? titel : translatedTitle;
    }

    public String getNormalizedTitle() {
        return this.titel.replace(" ", "_");
    }

    public List<ErrorProperty> getEigenschaften() {
        if (this.eigenschaften == null) {
            this.eigenschaften = new ArrayList<>();
        }
        return this.eigenschaften;
    }

    public List<User> getBenutzer() {
        if ((benutzer == null || benutzer.isEmpty()) && id != null) {
            benutzer = UserManager.getUserForStep(id);
        }
        return this.benutzer;
    }

    public List<Usergroup> getBenutzergruppen() {
        if ((benutzergruppen == null || benutzergruppen.isEmpty()) && id != null) {
            benutzergruppen = UsergroupManager.getUserGroupsForStep(id);
        }
        return this.benutzergruppen;
    }

    /*
     *  Helper
     */

    public int getEigenschaftenSize() {
        return getEigenschaften().size();
    }

    public List<ErrorProperty> getEigenschaftenList() {

        return getEigenschaften();
    }

    public int getBenutzerSize() {

        return getBenutzer().size();

    }

    public List<User> getBenutzerList() {

        return getBenutzer();
    }

    public int getBenutzergruppenSize() {

        return getBenutzergruppen().size();
    }

    public List<Usergroup> getBenutzergruppenList() {

        return getBenutzergruppen();
    }

    public void setBearbeitungsstatusUp() {
        switch (getBearbeitungsstatusEnum()) {
            case ERROR:
            case INFLIGHT:
            case INWORK:
                bearbeitungsstatus = StepStatus.DONE.getValue();
                SendMail.getInstance().sendMailToAssignedUser(this, StepStatus.DONE);
                break;
            case OPEN:
                bearbeitungsstatus = 2;
                SendMail.getInstance().sendMailToAssignedUser(this, StepStatus.getStatusFromValue(bearbeitungsstatus));
                break;
            case LOCKED:
                bearbeitungsstatus = 1;
                //                if(this.typAutomaticThumbnail) {
                //                    this.submitAutomaticThumbnailTicket();
                //                }
                SendMail.getInstance().sendMailToAssignedUser(this, StepStatus.getStatusFromValue(bearbeitungsstatus));
                break;
            case DONE:
            case DEACTIVATED:
            default:
        }
    }

    public void setBearbeitungsstatusDown() {
        switch (getBearbeitungsstatusEnum()) {
            case DONE:
                bearbeitungsstatus = 2;
                SendMail.getInstance().sendMailToAssignedUser(this, StepStatus.getStatusFromValue(bearbeitungsstatus));
                break;
            case ERROR:
            case INFLIGHT:
            case INWORK:
                bearbeitungsstatus = 1;
                //                if(this.typAutomaticThumbnail) {
                //                    this.submitAutomaticThumbnailTicket();
                //                }
                SendMail.getInstance().sendMailToAssignedUser(this, StepStatus.getStatusFromValue(bearbeitungsstatus));
                break;

            case OPEN:
                bearbeitungsstatus = 0;
                SendMail.getInstance().sendMailToAssignedUser(this, StepStatus.getStatusFromValue(bearbeitungsstatus));
                break;
            case LOCKED:
            case DEACTIVATED:
            default:
        }
    }

    public void setTypImagesSchreiben(boolean typImagesSchreiben) {
        this.typImagesSchreiben = typImagesSchreiben;
        if (typImagesSchreiben) {
            this.typImagesLesen = true;
        }
    }

    /*
     * Helper
     */

    /**
     * @return Rückgabe des Schritttitels sowie (sofern vorhanden) den Benutzer mit vollständigem Namen
     */
    public String getTitelMitBenutzername() {
        String rueckgabe = this.titel;
        if (this.bearbeitungsbenutzer != null && this.bearbeitungsbenutzer.getId() != null && this.bearbeitungsbenutzer.getId().intValue() != 0) {
            rueckgabe += " (" + this.bearbeitungsbenutzer.getNachVorname() + ")";
        }
        return rueckgabe;
    }

    public String getBearbeitungsstatusAsString() {
        return String.valueOf(this.bearbeitungsstatus.intValue());
    }

    public void setBearbeitungsstatusAsString(String inbearbeitungsstatus) {
        this.bearbeitungsstatus = Integer.parseInt(inbearbeitungsstatus);
    }

    public ArrayList<String> getAllScriptPaths() {
        ArrayList<String> answer = new ArrayList<>();
        if (this.typAutomatischScriptpfad != null && !this.typAutomatischScriptpfad.equals("")) {
            answer.add(this.typAutomatischScriptpfad);
        }
        if (this.typAutomatischScriptpfad2 != null && !this.typAutomatischScriptpfad2.equals("")) {
            answer.add(this.typAutomatischScriptpfad2);
        }
        if (this.typAutomatischScriptpfad3 != null && !this.typAutomatischScriptpfad3.equals("")) {
            answer.add(this.typAutomatischScriptpfad3);
        }
        if (this.typAutomatischScriptpfad4 != null && !this.typAutomatischScriptpfad4.equals("")) {
            answer.add(this.typAutomatischScriptpfad4);
        }
        if (this.typAutomatischScriptpfad5 != null && !this.typAutomatischScriptpfad5.equals("")) {
            answer.add(this.typAutomatischScriptpfad5);
        }
        return answer;
    }

    public HashMap<String, String> getAllScripts() {
        LinkedHashMap<String, String> answer = new LinkedHashMap<>();
        if (this.typAutomatischScriptpfad != null && !this.typAutomatischScriptpfad.equals("")) {
            answer.put(this.scriptname1, this.typAutomatischScriptpfad);
        }
        if (this.typAutomatischScriptpfad2 != null && !this.typAutomatischScriptpfad2.equals("")) {
            answer.put(this.scriptname2, this.typAutomatischScriptpfad2);
        }
        if (this.typAutomatischScriptpfad3 != null && !this.typAutomatischScriptpfad3.equals("")) {
            answer.put(this.scriptname3, this.typAutomatischScriptpfad3);
        }
        if (this.typAutomatischScriptpfad4 != null && !this.typAutomatischScriptpfad4.equals("")) {
            answer.put(this.scriptname4, this.typAutomatischScriptpfad4);
        }
        if (this.typAutomatischScriptpfad5 != null && !this.typAutomatischScriptpfad5.equals("")) {
            answer.put(this.scriptname5, this.typAutomatischScriptpfad5);
        }
        return answer;
    }

    public void setAllScripts(HashMap<String, String> paths) {
        Set<String> keys = paths.keySet();
        ArrayList<String> keyList = new ArrayList<>();
        for (String key : keys) {
            keyList.add(key);
        }
        int size = keyList.size();
        if (size > 0) {
            this.scriptname1 = keyList.get(0);
            this.typAutomatischScriptpfad = paths.get(keyList.get(0));
        }
        if (size > 1) {
            this.scriptname2 = keyList.get(1);
            this.typAutomatischScriptpfad2 = paths.get(keyList.get(1));
        }
        if (size > 2) {
            this.scriptname3 = keyList.get(2);
            this.typAutomatischScriptpfad3 = paths.get(keyList.get(2));
        }
        if (size > 3) {
            this.scriptname4 = keyList.get(3);
            this.typAutomatischScriptpfad4 = paths.get(keyList.get(3));
        }
        if (size > 4) {
            this.scriptname5 = keyList.get(4);
            this.typAutomatischScriptpfad5 = paths.get(keyList.get(4));
        }
    }

    public String getListOfPaths() {
        String answer = "";
        if (this.scriptname1 != null) {
            answer += this.scriptname1;
        }
        if (this.scriptname2 != null) {
            answer = answer + "; " + this.scriptname2;
        }
        if (this.scriptname3 != null) {
            answer = answer + "; " + this.scriptname3;
        }
        if (this.scriptname4 != null) {
            answer = answer + "; " + this.scriptname4;
        }
        if (this.scriptname5 != null) {
            answer = answer + "; " + this.scriptname5;
        }
        return answer;

    }

    /*
     * batch step information
     */

    public Boolean getBatchStep() {
        if (this.batchStep == null) {
            this.batchStep = Boolean.valueOf(false);
        }
        return this.batchStep;
    }

    public Boolean isBatchStep() {
        if (this.batchStep == null) {
            this.batchStep = Boolean.valueOf(false);
        }
        return this.batchStep;
    }

    public void setBatchStep(Boolean batchStep) {
        if (batchStep == null) {
            batchStep = Boolean.valueOf(false);
        }
        this.batchStep = batchStep;
    }

    @Override
    public int compareTo(Step arg0) {

        return id.compareTo(arg0.getId());
    }

    @Override
    public void lazyLoad() {
        if (processId != null) {
            prozess = ProcessManager.getProcessById(processId);
        }
        if (userId != null) {
            try {
                bearbeitungsbenutzer = UserManager.getUserById(userId);
            } catch (DAOException e) {

            }
        }
        // Eigenschaften

        // Nutzer
        if (benutzer == null) {
            benutzer = UserManager.getUserForStep(id);
        }
        // Nutzergruppen
        if (benutzergruppen == null) {
            benutzergruppen = UsergroupManager.getUserGroupsForStep(id);
        }

    }

    public void setMessageQueue(QueueType mq) {
        if (mq == null) {
            this.messageQueue = QueueType.NONE;
        } else {
            this.messageQueue = mq;
        }
    }

}
