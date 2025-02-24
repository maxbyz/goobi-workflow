package de.sub.goobi.helper;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang.SystemUtils;
import org.goobi.beans.Process;
import org.goobi.goobiScript.GoobiScriptManager;
import org.goobi.goobiScript.GoobiScriptResult;
import org.goobi.goobiScript.IGoobiScript;
import org.goobi.managedbeans.LoginBean;
import org.goobi.production.enums.LogType;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;

import de.sub.goobi.helper.exceptions.UghHelperException;
import de.sub.goobi.helper.tasks.LongRunningTaskManager;
import de.sub.goobi.helper.tasks.ProcessSwapInTask;
import de.sub.goobi.helper.tasks.ProcessSwapOutTask;
import de.sub.goobi.persistence.managers.ProcessManager;
import lombok.extern.log4j.Log4j2;
import ugh.dl.Fileformat;
import ugh.dl.Metadata;
import ugh.dl.MetadataType;
import ugh.exceptions.MetadataTypeNotAllowedException;

@Log4j2
public class GoobiScript {
    public final static String DIRECTORY_SUFFIX = "_tif";

    /**
     * executes the list of GoobiScript commands for all processes that were selected
     * 
     * @param processes List of process identifiers
     * @param allScripts all goobiScript calls that were used
     * @return
     */
    @Deprecated
    public String execute(List<Integer> processes, String allScripts) {
        GoobiScriptManager gsm = Helper.getBeanByClass(GoobiScriptManager.class);
        return execute(processes, allScripts, gsm);
    }

    /**
     * executes the list of GoobiScript commands for all processes that were selected
     * 
     * @param processes List of process identifiers
     * @param allScripts all goobiScript calls that were used
     * @param gsm GoobiScriptManager to use
     * @return
     */
    public String execute(List<Integer> processes, String allScripts, GoobiScriptManager gsm) {
        List<Map<String, String>> scripts = parseGoobiscripts(allScripts);
        return this.execute(processes, scripts, gsm);
    }

    public String execute(List<Integer> processes, List<Map<String, String>> scripts, GoobiScriptManager gsm) {
        if (scripts == null) {
            return "";
        }
        for (Map<String, String> currentScript : scripts) {

            // in case of a missing action parameter skip this goobiscript
            String myaction = currentScript.get("action");
            if (myaction == null || myaction.length() == 0) {
                Helper.setFehlerMeldung("goobiScriptfield", "Missing action!", "Please select one of the allowed commands.");
                continue;
            }

            // in case of missing rights skip this goobiscript
            LoginBean loginForm = Helper.getLoginBean();
            if (!loginForm.hasRole("goobiscript_" + myaction) && !loginForm.hasRole("Workflow_Processes_Allow_GoobiScript")) {
                Helper.setFehlerMeldung("goobiScriptfield", "You are not allowed to execute this GoobiScript: ", myaction);
                continue;
            }

            // now start the correct GoobiScript based on the String
            switch (myaction) {
                case "swapProzessesOut":
                    swapOutProzesses(processes);
                    break;
                case "swapProzessesIn":
                    swapInProzesses(processes);
                    break;
                case "updateContentFiles":
                    updateContentFiles(processes);
                    break;
                case "deleteTiffHeaderFile":
                    deleteTiffHeaderFile(processes);
                    break;
                default:
                    // find the right GoobiScript class
                    Optional<IGoobiScript> optGoobiScript = gsm.getGoobiScriptForAction(myaction);
                    if (optGoobiScript.isPresent()) {
                        IGoobiScript gs = optGoobiScript.get();
                        // initialize the GoobiScript to check if all is valid
                        List<GoobiScriptResult> scriptResults = gs.prepare(processes, currentScript.toString(), currentScript);

                        // just execute the GoobiScript now if the initialization was valid
                        if (!scriptResults.isEmpty()) {
                            Helper.setMeldung("goobiScriptfield", "", "GoobiScript added: " + gs.getAction());
                            gsm.enqueueScripts(scriptResults);
                            gsm.startWork();
                        }
                    } else {
                        Helper.setFehlerMeldung("goobiScriptfield", "Unknown action: " + myaction, " Please use one of the given below.");
                    }
            }
        }
        return "";
    }

    /**
     * Parses YAML to a list of GoobiScript parameter maps
     * 
     * @param allScripts
     * @return
     */
    public static List<Map<String, String>> parseGoobiscripts(String allScripts) {
        YAMLFactory yaml = new YAMLFactory();
        ObjectMapper mapper = new ObjectMapper();
        TypeReference<Map<String, String>> typeRef = new TypeReference<Map<String, String>>() {
        };
        try {
            YAMLParser yamlParser = yaml.createParser(allScripts);
            return mapper.readValues(yamlParser, typeRef).readAll();
        } catch (IOException ioException) {
            log.error(ioException);
            return null;
        }
    }

    /**
     * GoobiScript updateContentFiles
     * 
     * @param inProzesse List of identifiers for this GoobiScript
     */
    private void updateContentFiles(List<Integer> inProzesse) {
        for (Integer processId : inProzesse) {
            Process proz = ProcessManager.getProcessById(processId);
            try {
                Fileformat myRdf = proz.readMetadataFile();
                myRdf.getDigitalDocument().addAllContentFiles();
                proz.writeMetadataFile(myRdf);
                Helper.addMessageToProcessLog(proz.getId(), LogType.DEBUG, "ContentFiles updated using GoobiScript.");
                log.info("ContentFiles updated using GoobiScript for process with ID " + proz.getId());
                Helper.setMeldung("goobiScriptfield", "ContentFiles updated: ", proz.getTitel());
            } catch (ugh.exceptions.DocStructHasNoTypeException e) {
                Helper.setFehlerMeldung("DocStructHasNoTypeException", e.getMessage());

            } catch (Exception e) {
                Helper.setFehlerMeldung("goobiScriptfield", "Error while updating content files", e);
            }
        }
        Helper.setMeldung("goobiScriptfield", "", "GoobiScript 'updateContentFiles' finished");
    }

    /**
     * GoobiScript swapOutProzesses
     * 
     * @param inProzesse List of identifiers for this GoobiScript
     */
    private void swapOutProzesses(List<Integer> inProzesse) {
        for (Integer processId : inProzesse) {
            Process p = ProcessManager.getProcessById(processId);
            ProcessSwapOutTask task = new ProcessSwapOutTask();
            task.initialize(p);
            LongRunningTaskManager.getInstance().addTask(task);
            LongRunningTaskManager.getInstance().executeTask(task);
            Helper.addMessageToProcessLog(p.getId(), LogType.DEBUG, "Swapping out started using GoobiScript.");
            log.info("Swapping out started using GoobiScript for process with ID " + p.getId());
        }
        Helper.setMeldung("goobiScriptfield", "", "GoobiScript 'swapOut' executed.");
    }

    /**
     * GoobiScript swapInProzesses
     * 
     * @param inProzesse List of identifiers for this GoobiScript
     */
    private void swapInProzesses(List<Integer> inProzesse) {
        for (Integer processId : inProzesse) {
            Process p = ProcessManager.getProcessById(processId);
            ProcessSwapInTask task = new ProcessSwapInTask();
            task.initialize(p);
            LongRunningTaskManager.getInstance().addTask(task);
            LongRunningTaskManager.getInstance().executeTask(task);
            Helper.addMessageToProcessLog(p.getId(), LogType.DEBUG, "Swapping in started using GoobiScript.");
            log.info("Swapping in started using GoobiScript for process with ID " + p.getId());
        }
        Helper.setMeldung("goobiScriptfield", "", "GoobiScript 'swapIn' executed.");
    }

    /**
     * GoobiScript deleteTiffHeaderFile to delete an existing tiff header file tiffwriter.conf for each process
     * 
     * @param inProzesse List of identifiers for this GoobiScript
     */
    public void deleteTiffHeaderFile(List<Integer> inProzesse) {
        for (Integer processId : inProzesse) {
            Process proz = ProcessManager.getProcessById(processId);
            try {
                Path tiffheaderfile = Paths.get(proz.getImagesDirectory() + "tiffwriter.conf");
                if (StorageProvider.getInstance().isFileExists(tiffheaderfile)) {
                    StorageProvider.getInstance().deleteDir(tiffheaderfile);
                }
                Helper.addMessageToProcessLog(proz.getId(), LogType.DEBUG, "TiffHeaderFile deleted using GoobiScript.");
                log.info("TiffHeaderFile deleted using GoobiScript for process with ID " + proz.getId());
                Helper.setMeldung("goobiScriptfield", "TiffHeaderFile deleted: ", proz.getTitel());
            } catch (Exception e) {
                Helper.setFehlerMeldung("goobiScriptfield", "Error while deleting TiffHeader", e);
            }
        }
        Helper.setMeldung("goobiScriptfield", "", "GoobiScript 'deleteTiffHeaderFile' finished.");
    }

    /**
     * GoobiScript updateImagePath
     * 
     * @param inProzesse List of identifiers for this GoobiScript
     */
    public void updateImagePath(List<Integer> inProzesse) {
        for (Integer processId : inProzesse) {
            Process proz = ProcessManager.getProcessById(processId);
            try {

                Fileformat myRdf = proz.readMetadataFile();
                UghHelper ughhelp = new UghHelper();
                MetadataType mdt = ughhelp.getMetadataType(proz, "pathimagefiles");
                List<? extends ugh.dl.Metadata> alleImagepfade = myRdf.getDigitalDocument().getPhysicalDocStruct().getAllMetadataByType(mdt);
                if (alleImagepfade.size() > 0) {
                    for (Metadata md : alleImagepfade) {
                        myRdf.getDigitalDocument().getPhysicalDocStruct().getAllMetadata().remove(md);
                    }
                }
                Metadata newmd = new Metadata(mdt);
                if (SystemUtils.IS_OS_WINDOWS) {
                    newmd.setValue("file:/" + proz.getImagesDirectory() + proz.getTitel() + DIRECTORY_SUFFIX);
                } else {
                    newmd.setValue("file://" + proz.getImagesDirectory() + proz.getTitel() + DIRECTORY_SUFFIX);
                }
                myRdf.getDigitalDocument().getPhysicalDocStruct().addMetadata(newmd);
                proz.writeMetadataFile(myRdf);
                Helper.addMessageToProcessLog(proz.getId(), LogType.DEBUG, "ImagePath updated using GoobiScript.");
                log.info("ImagePath updated using GoobiScript for process with ID " + proz.getId());
                Helper.setMeldung("goobiScriptfield", "ImagePath updated: ", proz.getTitel());

            } catch (ugh.exceptions.DocStructHasNoTypeException e) {
                Helper.setFehlerMeldung("goobiScriptfield", "DocStructHasNoTypeException", e.getMessage());
            } catch (UghHelperException e) {
                Helper.setFehlerMeldung("goobiScriptfield", "UghHelperException", e.getMessage());
            } catch (MetadataTypeNotAllowedException e) {
                Helper.setFehlerMeldung("goobiScriptfield", "MetadataTypeNotAllowedException", e.getMessage());

            } catch (Exception e) {
                Helper.setFehlerMeldung("goobiScriptfield", "Error while updating imagepath", e);
            }

        }
        Helper.setMeldung("goobiScriptfield", "", "GoobiScript 'updateImagePath' finished.");
    }

}
