package org.goobi.goobiScript;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

import org.goobi.beans.Process;
import org.goobi.production.enums.GoobiScriptResultType;
import org.goobi.production.enums.LogType;

import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.VariableReplacer;
import de.sub.goobi.persistence.managers.ProcessManager;
import lombok.extern.log4j.Log4j2;
import ugh.dl.DocStruct;
import ugh.dl.Fileformat;
import ugh.dl.Metadata;
import ugh.dl.Prefs;

@Log4j2
public class GoobiScriptMetadataChangeValue extends AbstractIGoobiScript implements IGoobiScript {
    // action:metadataChangValue field:DocLanguage prefix:start_ suffix:_end position:top condition:Deutsch
    // action:metadataChangeValue field:DocLanguage prefix:start_ suffix:_end position:child

    @Override
    public String getAction() {
        return "metadataChangeValue";
    }

    @Override
    public String getSampleCall() {
        StringBuilder sb = new StringBuilder();
        addNewActionToSampleCall(sb, "This GoobiScript allows to change existing metadata in the METS files.");
        addParameterToSampleCall(sb, "field", "Classification",
                "Internal name of the metadata field to be used. Use the internal name here (e.g. `TitleDocMain`), not the translated display name (e.g. `Main title`) here.");
        addParameterToSampleCall(sb, "prefix", "Dark",
                "Define a string that shall be added in front of the existing metadata value here. In case ending blanks are wanted please put the prefix into quotes like \"this \".");
        addParameterToSampleCall(sb, "suffix", "color",
                "Define a string that shall be added behind the existing metadata value here. In case leading blanks are wanted please put the suffix into quotes like \" this\".");
        addParameterToSampleCall(sb, "position", "work",
                "Define where in the hierarchy of the METS file the searched term shall be replaced. Possible values are: `work` `top` `child` `any`");
        addParameterToSampleCall(sb, "condition", "blue",
                "Define a value here that shall be present in the metdata field. The metadata is only updated if this term can be found inside of the metadata value.");
        return sb.toString();
    }

    @Override
    public List<GoobiScriptResult> prepare(List<Integer> processes, String command, Map<String, String> parameters) {
        super.prepare(processes, command, parameters);

        if (parameters.get("field") == null || parameters.get("field").equals("")) {
            Helper.setFehlerMeldungUntranslated("goobiScriptfield", "Missing parameter: ", "field");
            return new ArrayList<>();
        }

        if ((parameters.get("prefix") == null || parameters.get("prefix").equals(""))
                && (parameters.get("suffix") == null || parameters.get("suffix").equals(""))) {
            Helper.setFehlerMeldungUntranslated("goobiScriptfield", "Missing parameter: ", "prefix OR suffix");
            return new ArrayList<>();
        }

        if (parameters.get("position") == null || parameters.get("position").equals("")) {
            Helper.setFehlerMeldungUntranslated("goobiScriptfield", "Missing parameter: ", "position");
            return new ArrayList<>();
        }

        // add all valid commands to list
        List<GoobiScriptResult> newList = new ArrayList<>();
        for (Integer i : processes) {
            GoobiScriptResult gsr = new GoobiScriptResult(i, command, parameters, username, starttime);
            newList.add(gsr);
        }
        return newList;
    }

    @Override
    public void execute(GoobiScriptResult gsr) {
        Map<String, String> parameters = gsr.getParameters();
        Process p = ProcessManager.getProcessById(gsr.getProcessId());
        gsr.setProcessTitle(p.getTitel());
        gsr.setResultType(GoobiScriptResultType.RUNNING);
        gsr.updateTimestamp();
        try {
            Fileformat ff = p.readMetadataFile();
            // first get the top element
            DocStruct ds = ff.getDigitalDocument().getLogicalDocStruct();

            // find the right elements to adapt
            List<DocStruct> dsList = new ArrayList<DocStruct>();
            switch (parameters.get("position")) {

                // just the anchor element
                case "top":
                    if (ds.getType().isAnchor()) {
                        dsList.add(ds);
                    } else {
                        gsr.setResultMessage("Error while adapting metadata for top element, as top element is no anchor");
                        gsr.setResultType(GoobiScriptResultType.ERROR);
                        return;
                    }
                    break;

                // fist the first child element
                case "child":
                    if (ds.getType().isAnchor()) {
                        dsList.add(ds.getAllChildren().get(0));
                    } else {
                        gsr.setResultMessage("Error while adapting metadata for child, as top element is no anchor");
                        gsr.setResultType(GoobiScriptResultType.ERROR);
                        return;
                    }
                    break;

                // any element in the hierarchy
                case "any":
                    dsList.add(ds);
                    dsList.addAll(ds.getAllChildrenAsFlatList());
                    break;

                // default "work", which is the first child or the main top element if it is not an anchor
                default:
                    if (ds.getType().isAnchor()) {
                        dsList.add(ds.getAllChildren().get(0));
                    } else {
                        dsList.add(ds);
                    }
                    break;
            }

            // now change the searched metadata and save the file
            String prefix = parameters.get("prefix");
            String suffix = parameters.get("suffix");
            String condition = parameters.get("condition");
            if (prefix == null) {
                prefix = "";
            }
            if (suffix == null) {
                suffix = "";
            }
            if (condition == null) {
                condition = "";
            }
            
            // get the content to be set and pipe it through the variable replacer
            VariableReplacer replacer = new VariableReplacer(ff.getDigitalDocument(), p.getRegelsatz().getPreferences(), p, null);
            prefix = replacer.replace(prefix);
            suffix = replacer.replace(suffix);
            condition = replacer.replace(condition);
            
            changeMetadata(dsList, parameters.get("field"), prefix, suffix, condition, p.getRegelsatz().getPreferences());
            p.writeMetadataFile(ff);
            Thread.sleep(2000);
            Helper.addMessageToProcessLog(p.getId(), LogType.DEBUG,
                    "Metadata changed using GoobiScript: " + parameters.get("field") + " - " + parameters.get("value"), username);
            log.info("Metadata changed using GoobiScript for process with ID " + p.getId());
            gsr.setResultMessage("Metadata changed successfully.");
            gsr.setResultType(GoobiScriptResultType.OK);
        } catch (Exception e1) {
            log.error("Problem while changing the metadata using GoobiScript for process with id: " + p.getId(), e1);
            gsr.setResultMessage("Error while changing metadata: " + e1.getMessage());
            gsr.setResultType(GoobiScriptResultType.ERROR);
            gsr.setErrorText(e1.getMessage());
        }

        gsr.updateTimestamp();
    }

    /**
     * Method to change a given metadata from a {@link DocStruct}
     * 
     * @param dsList as List of structural elements to use
     * @param field the metadata field that is used
     * @param prefix the prefix string that shall be added
     * @param suffix the suffix string that shall be added
     * @param condition a string which shall be contained in the string to restrict just to specific metadata
     * @param prefs the {@link Preferences} to use
     */
    private void changeMetadata(List<DocStruct> dsList, String field, String prefix, String suffix, String condition, Prefs prefs) {
        for (DocStruct ds : dsList) {
            List<? extends Metadata> mdlist = ds.getAllMetadataByType(prefs.getMetadataTypeByName(field));
            if (mdlist != null && mdlist.size() > 0) {
                for (Metadata md : mdlist) {
                    if (condition.isEmpty() || md.getValue().contains(condition)) {
                        md.setValue(prefix + md.getValue() + suffix);
                    }
                }
            }
        }
    }

}
