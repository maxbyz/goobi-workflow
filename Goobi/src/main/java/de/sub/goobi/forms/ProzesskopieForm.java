package de.sub.goobi.forms;

import java.io.File;
/**
 * This file is part of the Goobi Application - a Workflow tool for the support of mass digitization.
 * 
 * Visit the websites for more information.
 *             - https://goobi.io
 *             - https://www.intranda.com
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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.TreeSet;

import javax.enterprise.inject.Default;
import javax.faces.model.SelectItem;
import javax.inject.Named;
import javax.naming.NamingException;
import javax.servlet.http.Part;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.deltaspike.core.api.scope.WindowScoped;
import org.goobi.beans.LogEntry;
import org.goobi.beans.Masterpiece;
import org.goobi.beans.Masterpieceproperty;
import org.goobi.beans.Process;
import org.goobi.beans.Processproperty;
import org.goobi.beans.Project;
import org.goobi.beans.Ruleset;
import org.goobi.beans.Step;
import org.goobi.beans.Template;
import org.goobi.beans.Templateproperty;
import org.goobi.beans.User;
import org.goobi.managedbeans.LoginBean;
import org.goobi.production.enums.LogType;
import org.goobi.production.enums.UserRole;
import org.goobi.production.flow.jobs.HistoryAnalyserJob;
import org.goobi.production.plugin.interfaces.IOpacPlugin;
import org.goobi.production.plugin.interfaces.IOpacPluginVersion2;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;

import de.schlichtherle.io.FileOutputStream;
import de.sub.goobi.config.ConfigProjects;
import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.helper.BeanHelper;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.ScriptThreadWithoutHibernate;
import de.sub.goobi.helper.StorageProvider;
import de.sub.goobi.helper.UghHelper;
import de.sub.goobi.helper.enums.StepEditType;
import de.sub.goobi.helper.enums.StepStatus;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.helper.exceptions.SwapException;
import de.sub.goobi.helper.exceptions.UghHelperException;
import de.sub.goobi.metadaten.Image;
import de.sub.goobi.persistence.managers.ProcessManager;
import de.sub.goobi.persistence.managers.ProjectManager;
import de.sub.goobi.persistence.managers.RulesetManager;
import de.sub.goobi.persistence.managers.StepManager;
import de.unigoettingen.sub.search.opac.ConfigOpac;
import de.unigoettingen.sub.search.opac.ConfigOpacCatalogue;
import de.unigoettingen.sub.search.opac.ConfigOpacDoctype;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import ugh.dl.DigitalDocument;
import ugh.dl.DocStruct;
import ugh.dl.DocStructType;
import ugh.dl.Fileformat;
import ugh.dl.Metadata;
import ugh.dl.MetadataType;
import ugh.dl.Person;
import ugh.dl.Prefs;
import ugh.exceptions.DocStructHasNoTypeException;
import ugh.exceptions.MetadataTypeNotAllowedException;
import ugh.exceptions.PreferencesException;
import ugh.exceptions.ReadException;
import ugh.exceptions.TypeNotAllowedAsChildException;
import ugh.exceptions.TypeNotAllowedForParentException;
import ugh.exceptions.UGHException;
import ugh.exceptions.WriteException;
import ugh.fileformats.mets.XStream;

@Named("ProzesskopieForm")
@WindowScoped
@Default
@Log4j2
public class ProzesskopieForm implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 2579641488883675182L;
    private Helper help = new Helper();
    UghHelper ughHelper = new UghHelper();
    private BeanHelper bHelper = new BeanHelper();
    private Fileformat myRdf;
    @Getter
    @Setter
    private String opacSuchfeld = "12";
    @Getter
    @Setter
    private String opacSuchbegriff;
    @Getter
    private String opacKatalog;
    @Getter
    @Setter
    private Process prozessVorlage = new Process();
    @Getter
    @Setter
    private Process prozessKopie = new Process();

    private ConfigOpac co;
    /* komplexe Anlage von Vorgängen anhand der xml-Konfiguration */
    @Getter
    private boolean useOpac;
    @Getter
    private boolean useTemplates;

    @Getter
    private HashMap<String, Boolean> standardFields;
    @Getter
    @Setter
    private List<AdditionalField> additionalFields;
    @Getter
    @Setter
    private List<String> digitalCollections;
    @Getter
    @Setter
    private String tifHeader_imagedescription = "";
    @Getter
    @Setter
    private String tifHeader_documentname = "";

    private String naviFirstPage;
    @Getter
    @Setter
    private Integer auswahl;
    @Getter
    private String docType;
    private String atstsl = "";
    private List<String> possibleDigitalCollection;
    private Integer guessedImages = 0;
    @Getter
    @Setter
    private String addToWikiField = "";
    private List<ConfigOpacCatalogue> catalogues;
    private List<String> catalogueTitles;
    private ConfigOpacCatalogue currentCatalogue;

    public final static String DIRECTORY_SUFFIX = "_tif";

    public String Prepare() {
        atstsl = "";
        opacSuchbegriff = "";
        this.guessedImages = 0;
        if (ConfigurationHelper.getInstance().isResetProcesslog()) {
            addToWikiField = "";
        }

        //      Helper.getHibernateSession().refresh(this.prozessVorlage);
        if (this.prozessVorlage.getContainsUnreachableSteps()) {
            if (this.prozessVorlage.getSchritteList().size() == 0) {
                Helper.setFehlerMeldung("noStepsInWorkflow");
            }
            for (Step s : this.prozessVorlage.getSchritteList()) {
                if (s.getBenutzergruppenSize() == 0 && s.getBenutzerSize() == 0) {
                    Helper.setFehlerMeldung(Helper.getTranslation("noUserInStep", s.getTitel()));
                }
            }

            return "";
        }
        if (this.prozessVorlage.getProjekt().getProjectIsArchived()) {

            Helper.setFehlerMeldung("projectIsArchived");
            return "";
        }

        clearValues();
        this.co = ConfigOpac.getInstance();
        catalogues = co.getAllCatalogues(prozessVorlage.getTitel());

        catalogueTitles = new ArrayList<>(catalogues.size());
        for (ConfigOpacCatalogue coc : catalogues) {
            catalogueTitles.add(coc.getTitle());
        }

        readProjectConfigs();
        this.myRdf = null;
        this.prozessKopie = new Process();
        this.prozessKopie.setTitel("");
        this.prozessKopie.setIstTemplate(false);
        this.prozessKopie.setInAuswahllisteAnzeigen(false);
        this.prozessKopie.setProjekt(this.prozessVorlage.getProjekt());
        this.prozessKopie.setRegelsatz(this.prozessVorlage.getRegelsatz());
        this.prozessKopie.setDocket(this.prozessVorlage.getDocket());
        this.digitalCollections = new ArrayList<>();

        /*
         *  Kopie der Processvorlage anlegen
         */
        this.bHelper.SchritteKopieren(this.prozessVorlage, this.prozessKopie);
        this.bHelper.ScanvorlagenKopieren(this.prozessVorlage, this.prozessKopie);
        this.bHelper.WerkstueckeKopieren(this.prozessVorlage, this.prozessKopie);
        this.bHelper.EigenschaftenKopieren(this.prozessVorlage, this.prozessKopie);

        initializePossibleDigitalCollections();

        return this.naviFirstPage;
    }

    private void readProjectConfigs() {
        /*--------------------------------
         * projektabhängig die richtigen Felder in der Gui anzeigen
         * --------------------------------*/
        ConfigProjects cp = null;
        try {
            cp = new ConfigProjects(this.prozessVorlage.getProjekt().getTitel());
        } catch (IOException e) {
            Helper.setFehlerMeldung("IOException", e.getMessage());
            return;
        }

        this.docType = cp.getParamString("createNewProcess/defaultdoctype", this.co.getAllDoctypes().get(0).getTitle());
        this.useOpac = cp.getParamBoolean("createNewProcess/opac/@use");
        this.useTemplates = cp.getParamBoolean("createNewProcess/templates/@use");

        this.naviFirstPage = "process_new1";
        if (useOpac && StringUtils.isBlank(opacKatalog)) {
            setOpacKatalog(cp.getParamString("createNewProcess/opac/catalogue"));
            opacSuchfeld = cp.getParamString("createNewProcess/opac/catalogue/@searchfield", "12");
        }

        /*
         * -------------------------------- die auszublendenden Standard-Felder ermitteln --------------------------------
         */
        for (String t : cp.getParamList("createNewProcess/itemlist/hide")) {
            this.standardFields.put(t, false);
        }

        /*
         * -------------------------------- die einzublendenen (zusätzlichen) Eigenschaften ermitteln --------------------------------
         */
        List<HierarchicalConfiguration> itemList = cp.getList("createNewProcess/itemlist/item");
        for (HierarchicalConfiguration item : itemList) {
            AdditionalField fa = new AdditionalField();
            fa.setFrom(item.getString("@from"));
            fa.setTitel(item.getString("."));
            fa.setRequired(item.getBoolean("@required", false));
            fa.setIsdoctype(item.getString("@isdoctype"));
            fa.setIsnotdoctype(item.getString("@isnotdoctype"));
            fa.setInitStart(item.getString("@initStart"));
            fa.setInitEnd(item.getString("@initEnd"));

            if (item.getBoolean("@ughbinding", false)) {
                fa.setUghbinding(true);
                fa.setDocstruct(item.getString("@docstruct"));
                fa.setMetadata(item.getString("@metadata"));
            }
            if (item.getBoolean("@autogenerated", false)) {
                fa.setAutogenerated(true);
            }
            /*
             * -------------------------------- prüfen, ob das aktuelle Item eine Auswahlliste werden soll --------------------------------
             */
            List<HierarchicalConfiguration> parameterList = item.configurationsAt("select");
            /* Children durchlaufen und SelectItems erzeugen */

            if (parameterList.size() > 0) {
                if (item.getBoolean("@multiselect", true)) {
                    fa.setMultiselect(true);
                } else {
                    fa.setMultiselect(false);
                }
                fa.setSelectList(new ArrayList<SelectItem>());
            }

            if (parameterList.size() == 1) {
                fa.setWert(parameterList.get(0).getString("."));
                fa.setMultiselect(false);
            }

            for (HierarchicalConfiguration hc : parameterList) {
                String svalue = hc.getString("@label");

                String sid = hc.getString(".");
                fa.getSelectList().add(new SelectItem(sid, svalue, null));
            }

            this.additionalFields.add(fa);
        }

        // check if file upload is allowed
        enableFileUpload = cp.getParamBoolean("createNewProcess/fileupload/@use");
        configuredFolderNames = new ArrayList<>();
        configuredFolderRegex = new ArrayList<>();
        configuredFolderErrorMessageKeys = new ArrayList<>();
        if (enableFileUpload) {
            List<HierarchicalConfiguration> folderObjects = cp.getList("createNewProcess/fileupload/folder");
            if (folderObjects != null) {
                for (HierarchicalConfiguration folderObject : folderObjects) {

                    String regex = folderObject.getString("@regex");
                    if (regex == null) {
                        regex = "";
                    }
                    configuredFolderRegex.add(regex);

                    String key = folderObject.getString("@messageKey");
                    if (key == null) {
                        key = "";
                    }
                    configuredFolderErrorMessageKeys.add(key);

                    String name = folderObject.getString(".");
                    switch (name) {
                        case "intern":
                            configuredFolderNames.add(new SelectItem("intern", Helper.getTranslation("process_log_file_FolderSelectionInternal")));
                            break;
                        case "export":
                            configuredFolderNames
                            .add(new SelectItem("export", Helper.getTranslation("process_log_file_FolderSelectionExportToViewer")));
                            break;
                        case "master":
                            if (ConfigurationHelper.getInstance().isUseMasterDirectory()) {
                                configuredFolderNames.add(new SelectItem("master", Helper.getTranslation("process_log_file_masterFolder")));
                            }
                            break;
                        case "media":
                            configuredFolderNames.add(new SelectItem("media", Helper.getTranslation("process_log_file_mediaFolder")));
                            break;
                        default:
                            if (StringUtils.isNotBlank(ConfigurationHelper.getInstance().getAdditionalProcessFolderName(name))) {
                                configuredFolderNames.add(new SelectItem(name, Helper.getTranslation("process_log_file_" + name + "Folder")));
                            }
                            break;
                    }
                }
            }
            if (configuredFolderNames.isEmpty()) {
                enableFileUpload = false;
            } else {
                uploadFolder = (String) configuredFolderNames.get(0).getValue();
                uploadRegex = configuredFolderRegex.get(0);
            }
        }
    }

    /* =============================================================== */

    public List<SelectItem> getProzessTemplates() throws DAOException {
        List<SelectItem> myProcessTemplates = new ArrayList<>();
        String filter = " istTemplate = false AND inAuswahllisteAnzeigen = true ";

        /* Einschränkung auf bestimmte Projekte, wenn kein Admin */
        User aktuellerNutzer = Helper.getCurrentUser();

        if (aktuellerNutzer != null) {
            /*
             * wenn die maximale Berechtigung nicht Admin ist, dann nur bestimmte
             */
            if (!Helper.getLoginBean().hasRole(UserRole.Workflow_General_Show_All_Projects.name())) {

                filter += " AND prozesse.ProjekteID in (select ProjekteID from projektbenutzer where projektbenutzer.BenutzerID = "
                        + aktuellerNutzer.getId() + ")";

                //              Hibernate.initialize(aktuellerNutzer);
                //              Disjunction dis = Restrictions.disjunction();
                //              for (Project proj : aktuellerNutzer.getProjekte()) {
                //                  dis.add(Restrictions.eq("projekt", proj));
                //              }
                //              crit.add(dis);
            }
        }
        List<Process> selectList = ProcessManager.getProcesses("prozesse.titel", filter);
        //      for (Object proz : crit.list()) {
        for (Process proz : selectList) {
            myProcessTemplates.add(new SelectItem(proz.getId(), proz.getTitel(), null));
        }
        return myProcessTemplates;
    }

    /* =============================================================== */

    /**
     * OpacAnfrage
     */
    public String OpacAuswerten() {
        clearValues();
        readProjectConfigs();
        try {

            /* den Opac abfragen und ein RDF draus bauen lassen */
            this.myRdf = currentCatalogue.getOpacPlugin()
                    .search(this.opacSuchfeld, this.opacSuchbegriff, currentCatalogue, this.prozessKopie.getRegelsatz().getPreferences());
            if (myRdf == null) {
                Helper.setFehlerMeldung("No hit found", "");
                return "";
            }

            if (currentCatalogue.getOpacPlugin().getOpacDocType() != null) {
                this.docType = currentCatalogue.getOpacPlugin().getOpacDocType().getTitle();
            }
            this.atstsl = currentCatalogue.getOpacPlugin().getAtstsl();
            fillFieldsFromMetadataFile();
            /* über die Treffer informieren */
            if (currentCatalogue.getOpacPlugin().getHitcount() == 0) {
                Helper.setFehlerMeldung("No hit found", "");
            }
            if (currentCatalogue.getOpacPlugin().getHitcount() > 1) {
                Helper.setMeldung(null, "Found more then one hit", " - use first hit");
            }
        } catch (Exception e) {
            Helper.setFehlerMeldung("Error on reading opac ", e);
            log.error(e.toString(), e);
        }
        return "";
    }

    /* =============================================================== */

    /**
     * die Eingabefelder für die Eigenschaften mit Inhalten aus der RDF-Datei füllen
     * 
     * @throws PreferencesException
     */
    private void fillFieldsFromMetadataFile() throws PreferencesException {
        if (this.myRdf != null) {

            for (AdditionalField field : this.additionalFields) {
                if (field.isUghbinding() && field.getShowDependingOnDoctype(getDocType())) {
                    /* welches Docstruct */
                    DocStruct myTempStruct = this.myRdf.getDigitalDocument().getLogicalDocStruct();
                    if (field.getDocstruct().equals("firstchild")) {
                        try {
                            myTempStruct = this.myRdf.getDigitalDocument().getLogicalDocStruct().getAllChildren().get(0);
                        } catch (RuntimeException e) {
                        }
                    }
                    if (field.getDocstruct().equals("boundbook")) {
                        myTempStruct = this.myRdf.getDigitalDocument().getPhysicalDocStruct();
                    }
                    /* welches Metadatum */
                    try {
                        if (field.getMetadata().equals("ListOfCreators")) {
                            /* bei Autoren die Namen zusammenstellen */
                            String myautoren = "";
                            if (myTempStruct.getAllPersons() != null) {
                                for (Person p : myTempStruct.getAllPersons()) {
                                    if (StringUtils.isNotBlank(p.getLastname())) {
                                        myautoren += p.getLastname();
                                    }
                                    if (StringUtils.isNotBlank(p.getFirstname())) {
                                        myautoren += ", " + p.getFirstname();
                                    }
                                    myautoren += "; ";
                                }
                                if (myautoren.endsWith("; ")) {
                                    myautoren = myautoren.substring(0, myautoren.length() - 2);
                                }
                            }
                            field.setWert(myautoren);
                        } else {
                            /* bei normalen Feldern die Inhalte auswerten */
                            MetadataType mdt = this.ughHelper.getMetadataType(this.prozessKopie.getRegelsatz().getPreferences(), field.getMetadata());
                            Metadata md = this.ughHelper.getMetadata(myTempStruct, mdt);
                            if (md != null) {
                                if ((md.getValue() != null && !md.getValue().isEmpty()) || field.getWert() == null || field.getWert().isEmpty()) {
                                    field.setWert(md.getValue());
                                }
                                md.setValue(field.getWert().replace("&amp;", "&"));
                            }
                        }
                    } catch (UghHelperException e) {
                        log.error(e);
                        Helper.setFehlerMeldung(e.getMessage(), "");
                    }
                    if (field.getWert() != null && !field.getWert().equals("")) {
                        field.setWert(field.getWert().replace("&amp;", "&"));
                    }
                } // end if ughbinding
            } // end for
        } // end if myrdf==null
    }

    /**
     * alle Konfigurationseigenschaften und Felder zurücksetzen ================================================================
     */
    private void clearValues() {
        if (this.opacKatalog == null) {
            this.opacKatalog = "";
        }
        this.standardFields = new HashMap<>();
        this.standardFields.put("collections", true);
        this.standardFields.put("doctype", true);
        this.standardFields.put("preferences", true);
        this.standardFields.put("images", true);
        standardFields.put("fileUpload", true);
        this.additionalFields = new ArrayList<>();
        this.tifHeader_documentname = "";
        this.tifHeader_imagedescription = "";
        clearUploadedData();
    }

    /**
     * Auswahl des Processes auswerten
     * 
     * @throws DAOException
     * @throws NamingException
     * @throws SQLException ============================================================== ==
     */
    public String TemplateAuswahlAuswerten() throws DAOException {
        /* den ausgewählten Process laden */
        if (auswahl == null || auswahl == 0) {
            Helper.setFehlerMeldung("ErrorTemplateSelectionIsEmpty");
            return "";
        }
        Process tempProcess = ProcessManager.getProcessById(this.auswahl);

        if (tempProcess.getWerkstueckeSize() > 0) {
            /* erstes Werkstück durchlaufen */
            Masterpiece werk = tempProcess.getWerkstueckeList().get(0);
            for (Masterpieceproperty eig : werk.getEigenschaften()) {
                for (AdditionalField field : this.additionalFields) {
                    if (field.getTitel().equals(eig.getTitel())) {
                        field.setWert(eig.getWert());
                    }
                    if (eig.getTitel().equals("DocType")) {
                        docType = eig.getWert();
                    }
                }
            }
        }

        if (tempProcess.getVorlagenSize() > 0) {
            /* erste Vorlage durchlaufen */
            Template vor = tempProcess.getVorlagenList().get(0);
            for (Templateproperty eig : vor.getEigenschaften()) {
                for (AdditionalField field : this.additionalFields) {
                    if (field.getTitel().equals(eig.getTitel())) {
                        field.setWert(eig.getWert());
                    }
                }
            }
        }

        if (tempProcess.getEigenschaftenSize() > 0) {
            for (Processproperty pe : tempProcess.getEigenschaften()) {
                if (pe.getTitel().equals("digitalCollection")) {
                    digitalCollections.add(pe.getWert());
                }
            }
        }
        try {
            this.myRdf = tempProcess.readMetadataAsTemplateFile();
        } catch (Exception e) {
            Helper.setFehlerMeldung("Error on reading template-metadata ", e);
        }

        /* falls ein erstes Kind vorhanden ist, sind die Collectionen dafür */
        try {
            DocStruct colStruct = this.myRdf.getDigitalDocument().getLogicalDocStruct();
            removeCollections(colStruct);
            colStruct = colStruct.getAllChildren().get(0);
            removeCollections(colStruct);
        } catch (PreferencesException e) {
            Helper.setFehlerMeldung("Error on creating process", e);
            log.error("Error on creating process", e);
        } catch (RuntimeException e) {
            /*
             * das Firstchild unterhalb des Topstructs konnte nicht ermittelt werden
             */
        }

        return "";
    }

    /**
     * Validierung der Eingaben
     * 
     * @return sind Fehler bei den Eingaben vorhanden? ================================================================
     */
    private boolean isContentValid() {
        /*
         * -------------------------------- Vorbedingungen prüfen --------------------------------
         */
        boolean valide = true;

        /*
         * -------------------------------- grundsätzlich den Vorgangstitel prüfen --------------------------------
         */
        /* kein Titel */
        if (this.prozessKopie.getTitel() == null || this.prozessKopie.getTitel().equals("")) {
            valide = false;
            Helper.setFehlerMeldung(Helper.getTranslation("UnvollstaendigeDaten") + " " + Helper.getTranslation("ProcessCreationErrorTitleEmpty"));
        }

        String validateRegEx = ConfigurationHelper.getInstance().getProcessTiteValidationlRegex();
        if (!this.prozessKopie.getTitel().matches(validateRegEx)) {
            valide = false;
            Helper.setFehlerMeldung(Helper.getTranslation("UngueltigerTitelFuerVorgang"));
        }

        /* prüfen, ob der Processtitel schon verwendet wurde */
        if (this.prozessKopie.getTitel() != null) {
            long anzahl = 0;
            //          try {
            anzahl = ProcessManager.countProcessTitle(this.prozessKopie.getTitel(), prozessKopie.getProjekt().getInstitution());
            //          } catch (DAOException e) {
            //              Helper.setFehlerMeldung("Error on reading process information", e.getMessage());
            //              valide = false;
            //          }
            if (anzahl > 0) {
                valide = false;
                Helper.setFehlerMeldung(Helper.getTranslation("UngueltigeDaten:") + " " + Helper.getTranslation("ProcessCreationErrorTitleAllreadyInUse"));
            }
        }

        /*
         * -------------------------------- Prüfung der standard-Eingaben, die angegeben werden müssen --------------------------------
         */
        /* keine Collektion ausgewählt */
        if (this.standardFields.get("collections") && getDigitalCollections().size() == 0) {
            valide = false;
            Helper.setFehlerMeldung(Helper.getTranslation("UnvollstaendigeDaten") + " " + Helper.getTranslation("ProcessCreationErrorNoCollection"));
        }

        /*
         * -------------------------------- Prüfung der additional-Eingaben, die angegeben werden müssen --------------------------------
         */
        for (AdditionalField field : this.additionalFields) {
            if ((field.getWert() == null || field.getWert().equals("")) && field.isRequired() && field.getShowDependingOnDoctype(getDocType())
                    && (StringUtils.isBlank(field.getWert()))) {
                valide = false;
                Helper.setFehlerMeldung(Helper.getTranslation("UnvollstaendigeDaten") + " " + field.getTitel() + " "
                        + Helper.getTranslation("ProcessCreationErrorFieldIsEmpty"));

            }
        }
        return valide;
    }

    /* =============================================================== */

    public String GoToSeite1() {
        return this.naviFirstPage;
    }

    /* =============================================================== */

    public String GoToSeite2() {
        if (this.prozessKopie.getTitel() == null || this.prozessKopie.getTitel().equals("")) {
            CalcProzesstitel();
        }
        if (!isContentValid()) {
            return this.naviFirstPage;
        } else {
            return "process_new2";
        }
    }

    /**
     * Anlegen des Processes und Speichern der Metadaten ================================================================
     * 
     * @throws DAOException
     * @throws SwapException
     * @throws WriteException
     */
    public String NeuenProzessAnlegen()
            throws ReadException, IOException, InterruptedException, PreferencesException, SwapException, DAOException, WriteException {
        //        Helper.getHibernateSession().evict(this.prozessKopie);

        //        this.prozessKopie.setId(null);

        if (this.prozessKopie.getTitel() == null || this.prozessKopie.getTitel().equals("")) {
            CalcProzesstitel();
        }
        if (!isContentValid()) {
            return this.naviFirstPage;
        }
        EigenschaftenHinzufuegen();
        LoginBean loginForm = Helper.getLoginBean();
        for (Step step : this.prozessKopie.getSchritteList()) {
            /*
             * -------------------------------- DO NOT always save date and user for each step --------------------------------
             */
            //            step.setBearbeitungszeitpunkt(this.prozessKopie.getErstellungsdatum());
            //            step.setEditTypeEnum(StepEditType.AUTOMATIC);
            //
            //            if (loginForm != null) {
            //                step.setBearbeitungsbenutzer(loginForm.getMyBenutzer());
            //            }

            /*
             * -------------------------------- only if its done, set edit start and end date --------------------------------
             */
            if (step.getBearbeitungsstatusEnum() == StepStatus.DONE) {

                step.setBearbeitungszeitpunkt(this.prozessKopie.getErstellungsdatum());
                step.setEditTypeEnum(StepEditType.AUTOMATIC);

                if (loginForm != null) {
                    step.setBearbeitungsbenutzer(loginForm.getMyBenutzer());
                }

                step.setBearbeitungsbeginn(this.prozessKopie.getErstellungsdatum());
                // this concerns steps, which are set as done right on creation
                // bearbeitungsbeginn is set to creation timestamp of process
                // because the creation of it is basically begin of work
                Date myDate = new Date();
                step.setBearbeitungszeitpunkt(myDate);
                step.setBearbeitungsende(myDate);
            }

        }

        this.prozessKopie.setSortHelperImages(this.guessedImages);
        ProcessManager.saveProcess(this.prozessKopie);

        if (currentCatalogue != null && currentCatalogue.getOpacPlugin() != null && currentCatalogue.getOpacPlugin() instanceof IOpacPluginVersion2) {
            IOpacPluginVersion2 opacPluginV2 = (IOpacPluginVersion2) currentCatalogue.getOpacPlugin();
            // check if the plugin created files
            if (opacPluginV2.getRecordPathList() != null) {
                for (Path record : opacPluginV2.getRecordPathList()) {
                    // if this is the case, move the files to the import/ folder
                    Path destination = Paths.get(prozessKopie.getImportDirectory(), record.getFileName().toString());
                    StorageProvider.getInstance().createDirectories(destination.getParent());
                    StorageProvider.getInstance().move(record, destination);
                }
            }
            // check if the plugin provides the data as string
            if (opacPluginV2.getRawDataAsString() != null) {
                // if this is the case, store it in a file in import/
                for (Entry<String, String> entry : opacPluginV2.getRawDataAsString().entrySet()) {
                    Path destination = Paths.get(prozessKopie.getImportDirectory(), entry.getKey().replaceAll("\\W", "_"));
                    StorageProvider.getInstance().createDirectories(destination.getParent());
                    Files.write(destination, entry.getValue().getBytes());
                }
            }
        }

        if (addToWikiField != null && !addToWikiField.equals("")) {
            User user = loginForm.getMyBenutzer();
            LogEntry logEntry = new LogEntry();
            logEntry.setContent(addToWikiField);
            logEntry.setCreationDate(new Date());
            logEntry.setProcessId(prozessKopie.getId());
            logEntry.setType(LogType.INFO);
            logEntry.setUserName(user.getNachVorname());
            ProcessManager.saveLogEntry(logEntry);
            prozessKopie.getProcessLog().add(logEntry);
        }

        /*
         * wenn noch keine RDF-Datei vorhanden ist (weil keine Opac-Abfrage stattfand, dann jetzt eine anlegen
         */
        if (this.myRdf == null) {
            try {
                createNewFileformat();
            } catch (TypeNotAllowedForParentException | TypeNotAllowedAsChildException e) {
                Helper.setFehlerMeldung("ProcessCreationError_mets_save_error");
                Helper.setFehlerMeldung(e);
                ProcessManager.deleteProcess(prozessKopie);

                //this ensures that the process will be saved later, if corrected. If
                //the id is not null, then it is assumed that the process is already saved.
                prozessKopie.setId(null);
                return "";
            }
        }

        /*--------------------------------
         * wenn eine RDF-Konfiguration
         * vorhanden ist (z.B. aus dem Opac-Import, oder frisch angelegt), dann
         * diese ergänzen
         * --------------------------------*/
        if (this.myRdf != null) {
            for (AdditionalField field : this.additionalFields) {
                if (field.isUghbinding() && field.getShowDependingOnDoctype(getDocType())) {
                    /* welches Docstruct */
                    DocStruct myTempStruct = this.myRdf.getDigitalDocument().getLogicalDocStruct();
                    DocStruct myTempChild = null;
                    if (field.getDocstruct().equals("firstchild")) {
                        try {
                            myTempStruct = this.myRdf.getDigitalDocument().getLogicalDocStruct().getAllChildren().get(0);
                        } catch (RuntimeException e) {
                            /*
                             * das Firstchild unterhalb des Topstructs konnte nicht ermittelt werden
                             */
                        }
                    }
                    /*
                     * falls topstruct und firstchild das Metadatum bekommen sollen
                     */
                    if (!field.getDocstruct().equals("firstchild") && field.getDocstruct().contains("firstchild")) {
                        try {
                            myTempChild = this.myRdf.getDigitalDocument().getLogicalDocStruct().getAllChildren().get(0);
                        } catch (RuntimeException e) {
                        }
                    }
                    if (field.getDocstruct().equals("boundbook")) {
                        myTempStruct = this.myRdf.getDigitalDocument().getPhysicalDocStruct();
                    }
                    /* welches Metadatum */
                    try {
                        /*
                         * bis auf die Autoren alle additionals in die Metadaten übernehmen
                         */
                        if (!field.getMetadata().equals("ListOfCreators")) {
                            MetadataType mdt = this.ughHelper.getMetadataType(this.prozessKopie.getRegelsatz().getPreferences(), field.getMetadata());
                            Metadata md = this.ughHelper.getMetadata(myTempStruct, mdt);
                            if (md != null) {
                                md.setValue(field.getWert());
                            } else if (this.ughHelper.lastErrorMessage != null && field.getWert() != null && !field.getWert().isEmpty())//if the md could not be found, warn!
                            {
                                Helper.setFehlerMeldung(this.ughHelper.lastErrorMessage);
                                String strError = mdt.getName() + " : " + field.getWert();
                                Helper.setFehlerMeldung(strError);
                            }
                            /*
                             * wenn dem Topstruct und dem Firstchild der Wert gegeben werden soll
                             */
                            if (myTempChild != null) {
                                md = this.ughHelper.getMetadata(myTempChild, mdt);
                                if (md != null) {
                                    md.setValue(field.getWert());
                                }
                            }
                        }
                    } catch (Exception e) {
                        Helper.setFehlerMeldung(e);

                    }
                } // end if ughbinding
            } // end for

            /*
             * -------------------------------- Collectionen hinzufügen --------------------------------
             */
            DocStruct colStruct = this.myRdf.getDigitalDocument().getLogicalDocStruct();
            try {
                addCollections(colStruct);
                /* falls ein erstes Kind vorhanden ist, sind die Collectionen dafür */
                colStruct = colStruct.getAllChildren().get(0);
                addCollections(colStruct);
            } catch (RuntimeException e) {
                /*
                 * das Firstchild unterhalb des Topstructs konnte nicht ermittelt werden
                 */
            }

            /*
             * -------------------------------- Imagepfad hinzufügen (evtl. vorhandene zunächst löschen) --------------------------------
             */
            try {
                MetadataType mdt = this.ughHelper.getMetadataType(this.prozessKopie, "pathimagefiles");
                List<? extends Metadata> alleImagepfade = this.myRdf.getDigitalDocument().getPhysicalDocStruct().getAllMetadataByType(mdt);
                if (alleImagepfade != null && alleImagepfade.size() > 0) {
                    for (Metadata md : alleImagepfade) {
                        this.myRdf.getDigitalDocument().getPhysicalDocStruct().getAllMetadata().remove(md);
                    }
                }
                Metadata newmd = new Metadata(mdt);
                if (SystemUtils.IS_OS_WINDOWS) {
                    newmd.setValue("file:/" + this.prozessKopie.getImagesDirectory() + this.prozessKopie.getTitel().trim() + DIRECTORY_SUFFIX);
                } else {
                    newmd.setValue("file://" + this.prozessKopie.getImagesDirectory() + this.prozessKopie.getTitel().trim() + DIRECTORY_SUFFIX);
                }
                this.myRdf.getDigitalDocument().getPhysicalDocStruct().addMetadata(newmd);

                /* Rdf-File schreiben */
                this.prozessKopie.writeMetadataFile(this.myRdf);
                try {
                    this.prozessKopie.readMetadataFile();
                } catch (IOException e) {
                    Helper.setFehlerMeldung("ProcessCreationError_mets_save_error");
                    ProcessManager.deleteProcess(prozessKopie);

                    //this ensures that the process will be saved later, if corrected. If
                    //the id is not null, then it is assumed that the process is already saved.
                    prozessKopie.setId(null);
                    return "";
                }
                /*
                 * -------------------------------- soll der Process als Vorlage verwendet werden? --------------------------------
                 */
                if (this.useTemplates && this.prozessKopie.isInAuswahllisteAnzeigen()) {
                    this.prozessKopie.writeMetadataAsTemplateFile(this.myRdf);
                }

            } catch (UghHelperException | UGHException e) {
                Helper.setFehlerMeldung("ProcessCreationError_mets_save_error");
                Helper.setFehlerMeldung(e.getMessage());
                log.error("creation of new process throws an error: ", e);
                ProcessManager.deleteProcess(prozessKopie);

                //this ensures that the process will be saved later, if corrected. If
                //the id is not null, then it is assumed that the process is already saved.
                prozessKopie.setId(null);
                return "";
            }

        }

        // Adding process to history
        if (!HistoryAnalyserJob.updateHistoryForProzess(this.prozessKopie)) {
            Helper.setFehlerMeldung("historyNotUpdated");
            return "";
        } else {
            ProcessManager.saveProcess(this.prozessKopie);
        }

        //  read all uploaded files, copy them to the right destination, create log entries
        if (!uploadedFiles.isEmpty()) {
            for (UploadImage image : uploadedFiles) {
                if (!image.isDeleted()) {
                    Path folder = null;

                    if ("intern".equals(image.getFoldername())) {
                        folder = Paths.get(prozessKopie.getProcessDataDirectory(),
                                ConfigurationHelper.getInstance().getFolderForInternalProcesslogFiles());
                    } else if ("export".equals(image.getFoldername())) {
                        folder = Paths.get(prozessKopie.getExportDirectory());
                    } else {
                        folder = Paths.get(prozessKopie.getConfiguredImageFolder(image.getFoldername()));
                    }

                    if (!StorageProvider.getInstance().isFileExists(folder)) {
                        StorageProvider.getInstance().createDirectories(folder);
                    }
                    Path source = image.getImagePath();
                    Path destination = Paths.get(folder.toString(), source.getFileName().toString());
                    StorageProvider.getInstance().copyFile(source, destination);

                    if ("intern".equals(image.getFoldername()) || "export".equals(image.getFoldername())) {

                        LogEntry entry = LogEntry.build(prozessKopie.getId())
                                .withCreationDate(new Date())
                                .withContent(image.getDescriptionText())
                                .withType(LogType.FILE)
                                .withUsername(Helper.getCurrentUser().getNachVorname());
                        entry.setSecondContent(folder.toString());
                        entry.setThirdContent(destination.toString());
                        ProcessManager.saveLogEntry(entry);
                    }
                }

            }
            // finally clean up
            for (UploadImage image : uploadedFiles) {
                try {
                    StorageProvider.getInstance().deleteFile(image.getImagePath());
                } catch (Exception e) {
                    // do nothing, as this happens if the same file gets used in multiple target folders
                }
            }
        }
        List<Step> steps = StepManager.getStepsForProcess(prozessKopie.getId());
        for (Step s : steps) {
            if (s.getBearbeitungsstatusEnum().equals(StepStatus.OPEN) && s.isTypAutomatisch()) {
                ScriptThreadWithoutHibernate myThread = new ScriptThreadWithoutHibernate(s);
                myThread.startOrPutToQueue();
            }
        }
        prozessKopie = ProcessManager.getProcessById(prozessKopie.getId());
        return "process_new3";
    }

    /* =============================================================== */

    private void addCollections(DocStruct colStruct) {
        for (String s : this.digitalCollections) {
            try {
                Metadata md = new Metadata(this.ughHelper.getMetadataType(this.prozessKopie.getRegelsatz().getPreferences(), "singleDigCollection"));
                md.setValue(s);
                md.setParent(colStruct);
                colStruct.addMetadata(md);
            } catch (UghHelperException e) {
                Helper.setFehlerMeldung(e.getMessage(), "");

            } catch (DocStructHasNoTypeException e) {
                Helper.setFehlerMeldung(e.getMessage(), "");

            } catch (MetadataTypeNotAllowedException e) {
                Helper.setFehlerMeldung(e.getMessage(), "");

            }
        }
    }

    /**
     * alle Kollektionen eines übergebenen DocStructs entfernen ================================================================
     */
    private void removeCollections(DocStruct colStruct) {
        try {
            MetadataType mdt = this.ughHelper.getMetadataType(this.prozessKopie.getRegelsatz().getPreferences(), "singleDigCollection");
            ArrayList<Metadata> myCollections = new ArrayList<>(colStruct.getAllMetadataByType(mdt));
            if (myCollections != null && myCollections.size() > 0) {
                for (Metadata md : myCollections) {
                    colStruct.removeMetadata(md, true);
                }
            }
        } catch (UghHelperException e) {
            Helper.setFehlerMeldung(e.getMessage(), "");
            log.error(e);
        } catch (DocStructHasNoTypeException e) {
            Helper.setFehlerMeldung(e.getMessage(), "");
            log.error(e);
        }
    }

    /* =============================================================== */

    private void createNewFileformat() throws TypeNotAllowedForParentException, TypeNotAllowedAsChildException {
        Prefs myPrefs = this.prozessKopie.getRegelsatz().getPreferences();
        try {
            DigitalDocument dd = new DigitalDocument();
            Fileformat ff = new XStream(myPrefs);
            ff.setDigitalDocument(dd);
            /* BoundBook hinzufügen */
            DocStructType dst = myPrefs.getDocStrctTypeByName("BoundBook");
            DocStruct dsBoundBook = dd.createDocStruct(dst);
            dd.setPhysicalDocStruct(dsBoundBook);

            ConfigOpacDoctype configOpacDoctype = co.getDoctypeByName(this.docType);

            /* Monographie */
            if (!configOpacDoctype.isPeriodical() && !configOpacDoctype.isMultiVolume()) {
                DocStructType dsty = myPrefs.getDocStrctTypeByName(configOpacDoctype.getRulesetType());
                DocStruct ds = dd.createDocStruct(dsty);
                dd.setLogicalDocStruct(ds);
                this.myRdf = ff;
            }

            /* periodica */
            else if (configOpacDoctype.isPeriodical() || configOpacDoctype.isMultiVolume()) {

                DocStructType anchor = myPrefs.getDocStrctTypeByName(configOpacDoctype.getRulesetType());
                DocStruct ds = dd.createDocStruct(anchor);
                dd.setLogicalDocStruct(ds);

                DocStructType dstyvolume = myPrefs.getDocStrctTypeByName(configOpacDoctype.getRulesetChildType());
                DocStruct dsvolume = dd.createDocStruct(dstyvolume);
                ds.addChild(dsvolume);
                this.myRdf = ff;
            }

            //        } catch (TypeNotAllowedForParentException e) {
            //            log.error(e);
            //        } catch (TypeNotAllowedAsChildException e) {
            //            log.error(e);
        } catch (PreferencesException e) {
            log.error(e);
        }
    }

    private void EigenschaftenHinzufuegen() {
        /*
         * -------------------------------- Vorlageneigenschaften initialisieren --------------------------------
         */
        Template vor;
        if (this.prozessKopie.getVorlagenSize() > 0) {
            vor = this.prozessKopie.getVorlagenList().get(0);
        } else {
            vor = new Template();
            vor.setProzess(this.prozessKopie);
            List<Template> vorlagen = new ArrayList<>();
            vorlagen.add(vor);
            this.prozessKopie.setVorlagen(vorlagen);
        }

        /*
         * -------------------------------- Werkstückeigenschaften initialisieren --------------------------------
         */
        Masterpiece werk;
        if (this.prozessKopie.getWerkstueckeSize() > 0) {
            werk = this.prozessKopie.getWerkstueckeList().get(0);
        } else {
            werk = new Masterpiece();
            werk.setProzess(this.prozessKopie);
            List<Masterpiece> werkstuecke = new ArrayList<>();
            werkstuecke.add(werk);
            this.prozessKopie.setWerkstuecke(werkstuecke);
        }

        /*
         * -------------------------------- jetzt alle zusätzlichen Felder durchlaufen und die Werte hinzufügen --------------------------------
         */
        BeanHelper bh = new BeanHelper();
        for (AdditionalField field : this.additionalFields) {
            if (field.getShowDependingOnDoctype(getDocType())) {
                if (field.getFrom().equals("werk")) {
                    bh.EigenschaftHinzufuegen(werk, field.getTitel(), field.getWert());
                }
                if (field.getFrom().equals("vorlage")) {
                    bh.EigenschaftHinzufuegen(vor, field.getTitel(), field.getWert());
                }
                if (field.getFrom().equals("prozess")) {
                    bh.EigenschaftHinzufuegen(this.prozessKopie, field.getTitel(), field.getWert());
                }
            }
        }

        for (String col : digitalCollections) {
            bh.EigenschaftHinzufuegen(prozessKopie, "digitalCollection", col);
        }
        /* Doctype */
        bh.EigenschaftHinzufuegen(werk, "DocType", this.docType);
        /* Tiffheader */
        bh.EigenschaftHinzufuegen(werk, "TifHeaderImagedescription", this.tifHeader_imagedescription);
        bh.EigenschaftHinzufuegen(werk, "TifHeaderDocumentname", this.tifHeader_documentname);
        bh.EigenschaftHinzufuegen(prozessKopie, "Template", prozessVorlage.getTitel());
        bh.EigenschaftHinzufuegen(prozessKopie, "TemplateID", String.valueOf(prozessVorlage.getId()));
    }

    public void setDocType(String docType) throws TypeNotAllowedForParentException, TypeNotAllowedAsChildException {
        if (this.docType != null && this.docType.equals(docType)) {
            return;
        } else {
            this.docType = docType;
            if (myRdf != null) {

                Fileformat tmp = myRdf;

                createNewFileformat();

                try {
                    if (myRdf.getDigitalDocument().getLogicalDocStruct().equals(tmp.getDigitalDocument().getLogicalDocStruct())) {
                        myRdf = tmp;
                    } else {
                        DocStruct oldLogicalDocstruct = tmp.getDigitalDocument().getLogicalDocStruct();
                        DocStruct newLogicalDocstruct = myRdf.getDigitalDocument().getLogicalDocStruct();
                        // both have no childen
                        if (oldLogicalDocstruct.getAllChildren() == null && newLogicalDocstruct.getAllChildren() == null) {
                            copyMetadata(oldLogicalDocstruct, newLogicalDocstruct);
                        }
                        // old has a child, new has no child
                        else if (oldLogicalDocstruct.getAllChildren() != null && newLogicalDocstruct.getAllChildren() == null) {
                            copyMetadata(oldLogicalDocstruct, newLogicalDocstruct);
                            copyMetadata(oldLogicalDocstruct.getAllChildren().get(0), newLogicalDocstruct);
                        }
                        // new has a child, bot old not
                        else if (oldLogicalDocstruct.getAllChildren() == null && newLogicalDocstruct.getAllChildren() != null) {
                            copyMetadata(oldLogicalDocstruct, newLogicalDocstruct);
                            copyMetadata(oldLogicalDocstruct.copy(true, false), newLogicalDocstruct.getAllChildren().get(0));
                        }

                        // both have childen
                        else if (oldLogicalDocstruct.getAllChildren() != null && newLogicalDocstruct.getAllChildren() != null) {
                            copyMetadata(oldLogicalDocstruct, newLogicalDocstruct);
                            copyMetadata(oldLogicalDocstruct.getAllChildren().get(0), newLogicalDocstruct.getAllChildren().get(0));
                        }
                    }
                } catch (PreferencesException e) {
                    log.error(e);
                }
                try {
                    fillFieldsFromMetadataFile();
                } catch (PreferencesException e) {
                    log.error(e);
                }
            }
        }
    }

    private void copyMetadata(DocStruct oldDocStruct, DocStruct newDocStruct) {

        if (oldDocStruct.getAllMetadata() != null) {
            for (Metadata md : oldDocStruct.getAllMetadata()) {
                try {
                    newDocStruct.addMetadata(md);
                } catch (MetadataTypeNotAllowedException e) {
                } catch (DocStructHasNoTypeException e) {
                }
            }
        }
        if (oldDocStruct.getAllPersons() != null) {
            for (Person p : oldDocStruct.getAllPersons()) {
                try {
                    newDocStruct.addPerson(p);
                } catch (MetadataTypeNotAllowedException e) {
                } catch (DocStructHasNoTypeException e) {
                }
            }
        }
    }

    public Collection<SelectItem> getArtists() {
        ArrayList<SelectItem> artisten = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(ConfigurationHelper.getInstance().getTiffHeaderArtists(), "|");
        boolean tempBol = true;
        while (tokenizer.hasMoreTokens()) {
            String tok = tokenizer.nextToken();
            if (tempBol) {
                artisten.add(new SelectItem(tok));
            }
            tempBol = !tempBol;
        }
        return artisten;
    }

    /*
     * this is needed for GUI, render multiple select only if this is false if this is true use the only choice
     * 
     * @author Wulf
     */
    public boolean isSingleChoiceCollection() {
        return (getPossibleDigitalCollections() != null && getPossibleDigitalCollections().size() == 1);

    }

    /*
     * this is needed for GUI, render multiple select only if this is false if isSingleChoiceCollection is true use this choice
     * 
     * @author Wulf
     */
    public String getDigitalCollectionIfSingleChoice() {
        List<String> pdc = getPossibleDigitalCollections();
        if (pdc.size() == 1) {
            return pdc.get(0);
        } else {
            return null;
        }
    }

    public List<String> getPossibleDigitalCollections() {
        return this.possibleDigitalCollection;
    }

    private void initializePossibleDigitalCollections() {
        this.possibleDigitalCollection = new ArrayList<>();
        ArrayList<String> defaultCollections = new ArrayList<>();

        String filename = this.help.getGoobiConfigDirectory() + "goobi_digitalCollections.xml";
        if (!StorageProvider.getInstance().isFileExists(Paths.get(filename))) {
            Helper.setFehlerMeldung("File not found: ", filename);
            return;
        }
        this.digitalCollections = new ArrayList<>();
        try {
            /* Datei einlesen und Root ermitteln */
            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(filename);
            Element root = doc.getRootElement();
            /* alle Projekte durchlaufen */
            List<Element> projekte = root.getChildren();
            for (Iterator<Element> iter = projekte.iterator(); iter.hasNext();) {
                Element projekt = iter.next();

                // collect default collections
                if (projekt.getName().equals("default")) {
                    List<Element> myCols = projekt.getChildren("DigitalCollection");
                    for (Iterator<Element> it2 = myCols.iterator(); it2.hasNext();) {
                        Element col = it2.next();

                        if (col.getAttribute("default") != null && col.getAttributeValue("default").equalsIgnoreCase("true")) {
                            digitalCollections.add(col.getText());
                        }

                        defaultCollections.add(col.getText());
                    }
                } else {
                    // run through the projects
                    List<Element> projektnamen = projekt.getChildren("name");
                    for (Iterator<Element> iterator = projektnamen.iterator(); iterator.hasNext();) {
                        Element projektname = iterator.next();
                        // all all collections to list
                        if (projektname.getText().equalsIgnoreCase(this.prozessKopie.getProjekt().getTitel())) {
                            List<Element> myCols = projekt.getChildren("DigitalCollection");
                            for (Iterator<Element> it2 = myCols.iterator(); it2.hasNext();) {
                                Element col = it2.next();

                                if (col.getAttribute("default") != null && col.getAttributeValue("default").equalsIgnoreCase("true")) {
                                    digitalCollections.add(col.getText());
                                }

                                this.possibleDigitalCollection.add(col.getText());
                            }
                        }
                    }
                }
            }
        } catch (JDOMException e1) {
            log.error("error while parsing digital collections", e1);
            Helper.setFehlerMeldung("Error while parsing digital collections", e1);
        } catch (IOException e1) {
            log.error("error while parsing digital collections", e1);
            Helper.setFehlerMeldung("Error while parsing digital collections", e1);
        }

        if (this.possibleDigitalCollection.size() == 0) {
            this.possibleDigitalCollection = defaultCollections;
        }

        // if only one collection is possible take it directly

        if (isSingleChoiceCollection()) {
            this.digitalCollections.add(getDigitalCollectionIfSingleChoice());
        }
    }

    public Map<String, String> getAllSearchFields() {
        if (co.getCatalogueByName(opacKatalog) != null) {
            return co.getCatalogueByName(opacKatalog).getSearchFields();
        } else {
            return ConfigOpac.getInstance().getSearchFieldMap();
        }
    }

    public List<String> getAllOpacCatalogues() {
        return catalogueTitles;
    }

    public List<ConfigOpacDoctype> getAllDoctypes() {
        return co.getAllDoctypes();
    }

    /*
     * changed, so that on first request list gets set if there is only one choice
     */

    public void setOpacKatalog(String opacKatalog) {
        if (!this.opacKatalog.equals(opacKatalog)) {
            this.opacKatalog = opacKatalog;
            currentCatalogue = null;
            for (ConfigOpacCatalogue catalogue : catalogues) {
                if (opacKatalog.equals(catalogue.getTitle())) {
                    currentCatalogue = catalogue;
                    break;
                }
            }

            if (currentCatalogue == null && !catalogues.isEmpty()) {
                // get first catalogue in case configured catalogue doesn't exist
                currentCatalogue = catalogues.get(0);
            }
            if (currentCatalogue != null) {
                currentCatalogue.getOpacPlugin().setTemplateName(prozessVorlage.getTitel());
                currentCatalogue.getOpacPlugin().setProjectName(prozessVorlage.getProjekt().getTitel());
            }
        }
    }

    public String getPluginGui() {
        return currentCatalogue == null || currentCatalogue.getOpacPlugin() == null ? "/uii/includes/process/process_new_opac.xhtml"
                : currentCatalogue.getOpacPlugin().getGui();

    }

    /*
     * Helper
     */

    /**
     * Processtitel und andere Details generieren ================================================================
     */
    public void CalcProzesstitel() {
        String currentAuthors = "";
        String currentTitle = "";
        int counter = 0;
        for (AdditionalField field : this.additionalFields) {
            if (field.getAutogenerated() && field.getWert().isEmpty()) {
                field.setWert(String.valueOf(System.currentTimeMillis() + counter));
                counter++;
            }
            if (field.getMetadata() != null && field.getMetadata().equals("TitleDocMain") && currentTitle.length() == 0) {
                currentTitle = field.getWert();
            } else if (field.getMetadata() != null && field.getMetadata().equals("ListOfCreators") && currentAuthors.length() == 0) {
                currentAuthors = field.getWert();
            }

        }
        String newTitle = "";
        String titeldefinition = "";
        ConfigProjects cp = null;
        try {
            cp = new ConfigProjects(this.prozessVorlage.getProjekt().getTitel());
        } catch (IOException e) {
            Helper.setFehlerMeldung("IOException", e.getMessage());
            return;
        }
        String replacement = "";
        List<HierarchicalConfiguration> processTitleList = cp.getList("createNewProcess/itemlist/processtitle");

        for (HierarchicalConfiguration hc : processTitleList) {
            String titel = hc.getString(".");
            String isdoctype = hc.getString("@isdoctype");
            String isnotdoctype = hc.getString("@isnotdoctype");

            String replacementText = hc.getString("@replacewith", "");

            if (titel == null) {
                titel = "";
            }
            if (isdoctype == null) {
                isdoctype = "";
            }
            if (isnotdoctype == null) {
                isnotdoctype = "";
            }

            /* wenn nix angegeben wurde, dann anzeigen */
            if (isdoctype.equals("") && isnotdoctype.equals("")) {
                titeldefinition = titel;
                replacement = replacementText;
                break;
            }

            /* wenn beides angegeben wurde */
            if (!isdoctype.equals("") && !isnotdoctype.equals("") && StringUtils.containsIgnoreCase(isdoctype, this.docType)
                    && !StringUtils.containsIgnoreCase(isnotdoctype, this.docType)) {
                titeldefinition = titel;
                replacement = replacementText;
                break;
            }

            /* wenn nur pflicht angegeben wurde */
            if (isnotdoctype.equals("") && StringUtils.containsIgnoreCase(isdoctype, this.docType)) {
                titeldefinition = titel;
                replacement = replacementText;
                break;
            }
            /* wenn nur "darf nicht" angegeben wurde */
            if (isdoctype.equals("") && !StringUtils.containsIgnoreCase(isnotdoctype, this.docType)) {
                titeldefinition = titel;
                replacement = replacementText;
                break;
            }
        }

        StringTokenizer tokenizer = new StringTokenizer(titeldefinition, "+");
        /* jetzt den Bandtitel parsen */
        while (tokenizer.hasMoreTokens()) {
            String myString = tokenizer.nextToken();
            /*
             * wenn der String mit ' anfängt und mit ' endet, dann den Inhalt so übernehmen
             */
            if (myString.startsWith("'") && myString.endsWith("'")) {
                newTitle += myString.substring(1, myString.length() - 1);
            } else {
                /* andernfalls den string als Feldnamen auswerten */
                for (Iterator<AdditionalField> it2 = this.additionalFields.iterator(); it2.hasNext();) {
                    AdditionalField myField = it2.next();

                    /*
                     * wenn es das ATS oder TSL-Feld ist, dann den berechneten atstsl einsetzen, sofern noch nicht vorhanden
                     */
                    if ((myField.getTitel().equals("ATS") || myField.getTitel().equals("TSL")) && myField.getShowDependingOnDoctype(getDocType())
                            && (myField.getWert() == null || myField.getWert().equals(""))) {
                        if (atstsl == null || atstsl.length() == 0) {
                            atstsl = createAtstsl(currentTitle, currentAuthors);
                        }
                        myField.setWert(this.atstsl);
                    }

                    /* den Inhalt zum Titel hinzufügen */
                    if (myField.getTitel().equals(myString) && myField.getShowDependingOnDoctype(getDocType()) && myField.getWert() != null) {
                        newTitle += CalcProcesstitelCheck(myField.getTitel(), myField.getWert());
                    }
                }
            }
        }

        if (newTitle.endsWith("_")) {
            newTitle = newTitle.substring(0, newTitle.length() - 1);
        }
        // remove non-ascii characters for the sake of TIFF header limits
        String regex = ConfigurationHelper.getInstance().getProcessTitleReplacementRegex();

        String filteredTitle = newTitle.replaceAll(regex, replacement);
        prozessKopie.setTitel(filteredTitle);
        CalcTiffheader();
    }

    /* =============================================================== */

    private String CalcProcesstitelCheck(String inFeldName, String inFeldWert) {
        String rueckgabe = inFeldWert;

        /*
         * -------------------------------- Bandnummer --------------------------------
         */
        if (inFeldName.equals("Bandnummer") || inFeldName.equals("Volume number")) {
            try {
                int bandint = Integer.parseInt(inFeldWert);
                java.text.DecimalFormat df = new java.text.DecimalFormat("#0000");
                rueckgabe = df.format(bandint);
            } catch (NumberFormatException e) {
                if (inFeldName.equals("Bandnummer")) {
                    Helper.setFehlerMeldung(Helper.getTranslation("UngueltigeDaten: ") + "Bandnummer ist keine gültige Zahl");
                } else {
                    Helper.setFehlerMeldung(Helper.getTranslation("UngueltigeDaten: ") + "Volume number is not a valid number");
                }
            }
            if (rueckgabe != null && rueckgabe.length() < 4) {
                rueckgabe = "0000".substring(rueckgabe.length()) + rueckgabe;
            }
        }

        // TODO: temporary solution for shelfmark, replace it with configurable solution
        if (inFeldName.equalsIgnoreCase("Signatur") || inFeldName.equalsIgnoreCase("Shelfmark")) {
            if (StringUtils.isNotBlank(rueckgabe)) {
                // replace white spaces with dash, remove other special characters
                rueckgabe = rueckgabe.replace(" ", "-").replace("/", "-").replaceAll("[^\\w-]", "");

            }
        }

        return rueckgabe;
    }

    /* =============================================================== */

    public void CalcTiffheader() {
        String tif_definition = "";
        ConfigProjects cp = null;
        try {
            cp = new ConfigProjects(this.prozessVorlage.getProjekt().getTitel());
        } catch (IOException e) {
            Helper.setFehlerMeldung("IOException", e.getMessage());
            return;
        }
        tif_definition = cp.getParamString("tifheader/" + this.docType, "intranda");

        /*
         * -------------------------------- evtuelle Ersetzungen --------------------------------
         */
        tif_definition = tif_definition.replaceAll("\\[\\[", "<");
        tif_definition = tif_definition.replaceAll("\\]\\]", ">");

        /*
         * -------------------------------- Documentname ist im allgemeinen = Processtitel --------------------------------
         */
        this.tifHeader_documentname = this.prozessKopie.getTitel();
        this.tifHeader_imagedescription = "";
        /*
         * -------------------------------- Imagedescription --------------------------------
         */
        StringTokenizer tokenizer = new StringTokenizer(tif_definition, "+");
        /* jetzt den Tiffheader parsen */
        String title = "";
        while (tokenizer.hasMoreTokens()) {
            String myString = tokenizer.nextToken();
            /*
             * wenn der String mit ' anf�ngt und mit ' endet, dann den Inhalt so übernehmen
             */
            if (myString.startsWith("'") && myString.endsWith("'") && myString.length() > 2) {
                this.tifHeader_imagedescription += myString.substring(1, myString.length() - 1);
            } else if (myString.equals("$Doctype")) {
                /* wenn der Doctype angegeben werden soll */
                this.tifHeader_imagedescription += this.co.getDoctypeByName(this.docType).getTifHeaderType();
            } else {
                /* andernfalls den string als Feldnamen auswerten */
                for (Iterator<AdditionalField> it2 = this.additionalFields.iterator(); it2.hasNext();) {
                    AdditionalField myField = it2.next();
                    if ((myField.getTitel().equals("Titel") || myField.getTitel().equals("Title")) && myField.getWert() != null
                            && !myField.getWert().equals("")) {
                        title = myField.getWert();
                    }
                    /*
                     * wenn es das ATS oder TSL-Feld ist, dann den berechneten atstsl einsetzen, sofern noch nicht vorhanden
                     */
                    if ((myField.getTitel().equals("ATS") || myField.getTitel().equals("TSL")) && myField.getShowDependingOnDoctype(getDocType())
                            && (myField.getWert() == null || myField.getWert().equals(""))) {
                        myField.setWert(this.atstsl);
                    }

                    /* den Inhalt zum Titel hinzufügen */
                    if (myField.getTitel().equals(myString) && myField.getShowDependingOnDoctype(getDocType()) && myField.getWert() != null) {
                        this.tifHeader_imagedescription += CalcProcesstitelCheck(myField.getTitel(), myField.getWert());
                    }

                }
            }
            // reduce to 255 character
        }
        int length = this.tifHeader_imagedescription.length();
        if (length > 255) {
            try {
                int toCut = length - 255;
                String newTitle = title.substring(0, title.length() - toCut);
                this.tifHeader_imagedescription = this.tifHeader_imagedescription.replace(title, newTitle);
            } catch (IndexOutOfBoundsException e) {
                // TODO: handle exception
            }
        }
    }

    /**
     * @param imagesGuessed the imagesGuessed to set
     */
    public void setImagesGuessed(Integer imagesGuessed) {
        if (imagesGuessed == null) {
            imagesGuessed = 0;
        }
        this.guessedImages = imagesGuessed;
    }

    /**
     * @return the imagesGuessed
     */
    public Integer getImagesGuessed() {
        return this.guessedImages;
    }

    public Integer getRulesetSelection() {
        if (this.prozessKopie.getRegelsatz() != null) {
            return this.prozessKopie.getRegelsatz().getId();
        } else {
            return Integer.valueOf(0);
        }
    }

    public void setRulesetSelection(Integer selected) {
        if (selected.intValue() != 0) {
            try {
                Ruleset ruleset = RulesetManager.getRulesetById(selected);
                prozessKopie.setRegelsatz(ruleset);
                prozessKopie.setMetadatenKonfigurationID(selected);
            } catch (DAOException e) {
                Helper.setFehlerMeldung("Projekt kann nicht zugewiesen werden", "");
                log.error(e);
            }
        }
    }

    public List<SelectItem> getRulesetSelectionList() {
        List<SelectItem> rulesets = new ArrayList<>();
        List<Ruleset> temp = RulesetManager.getAllRulesets();
        for (Ruleset ruleset : temp) {
            rulesets.add(new SelectItem(ruleset.getId(), ruleset.getTitel(), null));
        }
        return rulesets;
    }

    public String createAtstsl(String title, String author) {
        StringBuilder result = new StringBuilder(8);
        if (author != null && author.trim().length() > 0) {
            result.append(author.length() > 4 ? author.substring(0, 4) : author);
            result.append(title.length() > 4 ? title.substring(0, 4) : title);
        } else {
            StringTokenizer titleWords = new StringTokenizer(title);
            int wordNo = 1;
            while (titleWords.hasMoreTokens() && wordNo < 5) {
                String word = titleWords.nextToken();
                switch (wordNo) {
                    case 1:
                        result.append(word.length() > 4 ? word.substring(0, 4) : word);
                        break;
                    case 2:
                    case 3:
                        result.append(word.length() > 2 ? word.substring(0, 2) : word);
                        break;
                    case 4:
                        result.append(word.length() > 1 ? word.substring(0, 1) : word);
                        break;
                }
                wordNo++;
            }
        }
        String res = UghHelper.convertUmlaut(result.toString()).toLowerCase();
        return res.replaceAll("[\\W]", ""); // delete umlauts etc.
    }

    public List<Project> getAvailableProjects() throws DAOException {
        List<Project> temp = ProjectManager.getProjectsForUser(Helper.getCurrentUser(), true);
        return temp;
    }

    public boolean isUserInProcessProject(Process process) throws DAOException {
        return this.getAvailableProjects().contains(process.getProjekt());
    }

    public IOpacPlugin getOpacPlugin() {
        if (currentCatalogue != null) {
            return currentCatalogue.getOpacPlugin();
        }
        return null;
    }

    // file upload area

    private Path temporaryFolder = null;

    @Getter
    private TreeSet<UploadImage> uploadedFiles = new TreeSet<>();

    @Getter
    private String uploadFolder;

    public void setUploadFolder(String folder) {
        this.uploadFolder = folder;
        this.uploadRegex = this.getRegexOfFolder(folder);
        log.debug("Regex: " + this.uploadRegex);
    }

    @Getter
    @Setter
    private String uploadRegex;

    @Getter
    @Setter
    private String fileUploadErrorMessage;

    public String generateFileUploadErrorMessage() {
        String key = this.getErrorMessageKeyOfFolder(this.uploadFolder);
        String message = "";

        if (key != null && key.length() > 0) {
            String result = Helper.getTranslation(key, this.uploadRegex);
            if (result != null && result.length() > 0 && !result.equals(key)) {
                message = result;
            } else {
                message = "";
            }
        }
        if (message.length() == 0) {
            message = "The selected file could not be uploaded because it does not match the specified file format.";
        }
        return message;
    }

    @Getter
    @Setter
    private String fileComment;

    @Getter
    @Setter
    private Part uploadedFile = null;

    @Getter
    private List<SelectItem> configuredFolderNames;

    @Getter
    private List<String> configuredFolderRegex;

    @Getter
    private List<String> configuredFolderErrorMessageKeys;

    @Getter
    private boolean enableFileUpload = false;

    private String getRegexOfFolder(String folder) {
        return this.configuredFolderRegex.get(this.getIndexOfFolder(folder));
    }

    private String getErrorMessageKeyOfFolder(String folder) {
        return this.configuredFolderErrorMessageKeys.get(this.getIndexOfFolder(folder));
    }

    private int getIndexOfFolder(String folder) {
        for (int index = 0; index < this.configuredFolderNames.size(); index++) {
            if (this.configuredFolderNames.get(index).getValue().equals(folder)) {
                return index;
            }
        }
        return -1;
    }

    /**
     * Get get temporary folder to upload to
     * 
     * @return path to temporary folder
     */
    private Path getTemporaryFolder() {
        if (temporaryFolder == null) {
            try {
                temporaryFolder = Files.createTempDirectory(Paths.get(ConfigurationHelper.getInstance().getTemporaryFolder()), "upload");
            } catch (IOException e) {
                log.error(e);
            }
        }
        return temporaryFolder;
    }

    /**
     * Handle the upload of a file
     * 
     * @param event
     */
    public void uploadFile(FileUploadEvent event) {
        try {
            UploadedFile upload = event.getFile();
            saveFileTemporary(upload.getFileName(), upload.getInputStream());
        } catch (IOException e) {
            log.error("Error while uploading files", e);
        }
    }

    /**
     * Save the uploaded file temporary in the tmp-folder inside of goobi in a subfolder for the user
     * 
     * @param fileName
     * @param in
     * @throws IOException
     */
    private void saveFileTemporary(String fileName, InputStream in) throws IOException {
        OutputStream out = null;
        try {
            File file = new File(getTemporaryFolder().toString(), fileName);
            out = new FileOutputStream(file);
            int read = 0;
            byte[] bytes = new byte[1024];
            while ((read = in.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            out.flush();

            UploadImage currentImage = new UploadImage(file.toPath(), uploadedFiles.size() + 1, 300, uploadFolder, fileComment);
            uploadedFiles.add(currentImage);

        } catch (IOException e) {
            log.error(e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    log.error(e);
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    log.error(e);
                }
            }
        }
    }

    /**
     * prepare variables and lists for next uploads
     */
    private void clearUploadedData() {
        if (temporaryFolder != null) {
            // delete data in folder
            StorageProvider.getInstance().deleteDir(temporaryFolder);
        }
        uploadedFile = null;
        uploadedFiles.clear();
        fileComment = null;
        temporaryFolder = null;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public class UploadImage extends Image implements Comparable<UploadImage> {
        private String foldername;
        private String descriptionText;
        private boolean deleted = false;

        public UploadImage(Path imagePath, int order, Integer thumbnailSize, String foldername, String descriptionText) throws IOException {
            super(imagePath, order, thumbnailSize);
            this.foldername = foldername;
            this.descriptionText = descriptionText;
        }

        @Override
        public int compareTo(UploadImage o) {
            String one = this.foldername + "_" + this.getTooltip();
            String two = o.foldername + "_" + o.getTooltip();
            return one.compareTo(two);
        }
    }
}
