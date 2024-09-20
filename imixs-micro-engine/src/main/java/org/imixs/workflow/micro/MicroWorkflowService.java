package org.imixs.workflow.micro;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.ModelManager;
import org.imixs.workflow.Plugin;
import org.imixs.workflow.WorkflowContext;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.WorkflowManager;
import org.imixs.workflow.bpmn.BPMNUtil;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.InvalidAccessException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.imixs.workflow.micro.plugins.ResultPlugin;
import org.openbpmn.bpmn.BPMNModel;
import org.openbpmn.bpmn.exceptions.BPMNModelException;
import org.openbpmn.bpmn.util.BPMNModelFactory;

/**
 * The MicroWorkflowEngine implements a lightweight workflow service. It can be
 * run on plain Java VMs.
 * <p>
 * The Service is based on the core concepts of Imixs-Workflow.
 * 
 * @see https://www.imixs.org
 */
public class MicroWorkflowService implements WorkflowManager, WorkflowContext {

    private static final Logger logger = Logger.getLogger(MicroWorkflowService.class.getName());

    // workitem properties
    public static final String UNIQUEIDREF = "$uniqueidref";
    public static final String READACCESS = "$readaccess";
    public static final String WRITEACCESS = "$writeaccess";
    public static final String PARTICIPANTS = "$participants";
    public static final String DEFAULT_TYPE = "workitem";

    public static final String INVALID_ITEMVALUE_FORMAT = "INVALID_ITEMVALUE_FORMAT";
    public static final String INVALID_TAG_FORMAT = "INVALID_TAG_FORMAT";

    // private final MicroWorkflowContext workflowContext;
    private MemoryDB database = null;
    private ModelManager openBPMNModelManager;
    private MicroSession session = null;

    /**
     * Constructor creates a WorkflowContext and a WorkflowKernel
     * 
     * @throws PluginException
     */

    public MicroWorkflowService(String device) throws PluginException {
        database = new MemoryDB();
        openBPMNModelManager = new ModelManager();
        session = new MicroSession(device);
    }

    /**
     * Returns the ModelManager Instance
     * 
     * @return
     */
    public ModelManager getModelManager() {
        return this.openBPMNModelManager;
    }

    @Override
    public Object getSessionContext() {
        return this.session;
    }

    public MemoryDB getDatabase() {
        return this.database;
    }

    /**
     * Loads a new model
     * 
     * @param modelPath
     */
    public void loadBPMNModel(String modelPath) {
        try {
            BPMNModel model = BPMNModelFactory.read(modelPath);
            getModelManager().addModel(model);
        } catch (BPMNModelException | ModelException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeWorkItem(ItemCollection workitem) throws AccessDeniedException {
        this.getDatabase().delete(workitem);
    }

    public ItemCollection getWorkItem(String id) {
        return getDatabase().load(id);
    }

    /**
     * This method processes a workItem by the WorkflowKernel and saves the workitem
     * after the processing was finished successful. The workitem have to provide at
     * least the properties '$modelversion', '$taskid' and '$eventid'
     * <p>
     * Before the method starts processing the workitem, the method load the current
     * instance of the given workitem and compares the property $taskID. If it is
     * not equal the method throws an ProcessingErrorException.
     * <p>
     * After the workitem was processed successful, the method verifies the property
     * $workitemList. If this property holds a list of entities these entities will
     * be saved and the property will be removed automatically.
     * 
     * 
     * @param workitem - the workItem to be processed
     * @return updated version of the processed workItem
     * @throws AccessDeniedException    - thrown if the user has insufficient access
     *                                  to update the workItem
     * @throws ProcessingErrorException - thrown if the workitem could not be
     *                                  processed by the workflowKernel
     * @throws PluginException          - thrown if processing by a plugin fails
     * @throws ModelException
     */
    public ItemCollection processWorkItem(ItemCollection workitem)
            throws AccessDeniedException, ProcessingErrorException, PluginException, ModelException {
        boolean debug = logger.isLoggable(Level.FINE);
        long lStartTime = System.currentTimeMillis();

        if (workitem == null)
            throw new ProcessingErrorException(MicroWorkflowService.class.getSimpleName(),
                    ProcessingErrorException.INVALID_WORKITEM, "workitem Is Null!");

        // load current instance of this workitem if a unqiueID is provided
        if (!workitem.getUniqueID().isEmpty()) {
            // try to load the instance
            ItemCollection currentInstance = this.getWorkItem(workitem.getUniqueID());
            // Instance successful loaded ?
            if (currentInstance != null) {
                // test for author access
                if (!currentInstance.getItemValueBoolean(MemoryDB.ISAUTHOR)) {
                    throw new AccessDeniedException(AccessDeniedException.OPERATION_NOTALLOWED, "$uniqueid: "
                            + workitem.getItemValueInteger(WorkflowKernel.UNIQUEID) + " - No Author Access!");
                }
                // test if $taskID matches current instance
                if (workitem.getTaskID() > 0 && currentInstance.getTaskID() != workitem.getTaskID()) {
                    throw new ProcessingErrorException(MicroWorkflowService.class.getSimpleName(),
                            ProcessingErrorException.INVALID_PROCESSID,
                            "$uniqueid: " + workitem.getItemValueInteger(WorkflowKernel.UNIQUEID) + " - $taskid="
                                    + workitem.getTaskID() + " Did Not Match Expected $taskid="
                                    + currentInstance.getTaskID());
                }
                // merge workitem into current instance (issue #86, issue #507)
                // an instance of this WorkItem still exists! so we update the new
                // values....
                workitem.mergeItems(currentInstance.getAllItems());

            } else {
                // In case we have a $UniqueId but did not found an matching workitem
                // and the workitem miss a valid model assignment than
                // processing is not possible - OPERATION_NOTALLOWED

                if ((workitem.getTaskID() <= 0) || (workitem.getEventID() <= 0)
                        || (workitem.getModelVersion().isEmpty() && workitem.getWorkflowGroup().isEmpty())) {
                    // user has no read access -> throw AccessDeniedException
                    throw new InvalidAccessException(InvalidAccessException.OPERATION_NOTALLOWED,
                            "$uniqueid: " + workitem.getItemValueInteger(WorkflowKernel.UNIQUEID)
                                    + " - Insufficient Data or Lack Of Permission!");
                }

            }
        }

        // verify type attribute
        if ("".equals(workitem.getType())) {
            workitem.replaceItemValue("type", DEFAULT_TYPE);
        }

        /*
         * Lookup current processEntity. If not available update model to latest
         * matching model version
         */
        BPMNModel model = null;
        try {
            model = this.getModelManager().getModel(workitem.getModelVersion());
        } catch (ModelException e) {
            throw new ProcessingErrorException(MicroWorkflowService.class.getSimpleName(),
                    ProcessingErrorException.INVALID_PROCESSID, e.getMessage(), e);
        }

        WorkflowKernel workflowkernel = new WorkflowKernel(this);
        // register plugins...
        registerPlugins(workflowkernel, model);
        // register adapters.....
        registerAdapters(workflowkernel);
        // udpate workitem metadata...
        updateMetadata(workitem);

        // now process the workitem
        try {
            long lKernelTime = System.currentTimeMillis();
            workitem = workflowkernel.process(workitem);
            if (debug) {
                logger.log(Level.FINE, "...WorkflowKernel processing time={0}ms",
                        System.currentTimeMillis() - lKernelTime);
            }
        } catch (PluginException pe) {
            // if a plugin exception occurs we roll back the transaction.
            logger.log(Level.SEVERE, "processing workitem ''{0} failed, rollback transaction...",
                    workitem.getItemValueString(WorkflowKernel.UNIQUEID));

            throw pe;
        }

        // Now fire also events for all split versions.....
        List<ItemCollection> splitWorkitems = workflowkernel.getSplitWorkitems();
        for (ItemCollection splitWorkitemm : splitWorkitems) {
            getDatabase().save(splitWorkitemm);
        }

        workitem = getDatabase().save(workitem);
        if (debug) {
            logger.log(Level.FINE, "...total processing time={0}ms", System.currentTimeMillis() - lStartTime);
        }
        return workitem;
    }

    /**
     * The method evaluates the WorkflowResult for a given BPMN event and returns a
     * ItemColleciton containing all item values of a specified xml tag. Each tag
     * definition must contain at least a name attribute and may contain an optional
     * list of additional attributes.
     * <p>
     * The method generates a item for each content element
     * and attribute value. e.g.:
     * <p>
     * {@code<item name="comment" ignore="true">text</item>}
     * <p>
     * This example will result in an ItemCollection with the attributes 'comment'
     * with value 'text' and 'comment.ignore' with the value 'true'
     * <p>
     * Also embedded itemVaues can be resolved (resolveItemValues=true):
     * <p>
     * {@code
     * 		<somedata>ABC<itemvalue>$uniqueid</itemvalue></somedata>
     * }
     * <p>
     * This example will result in a new item 'somedata' with the $uniqueid prefixed
     * with 'ABC'
     * 
     * @see https://stackoverflow.com/questions/1732348/regex-match-open-tags-except-xhtml-self-contained-tags
     * @param event
     * @param xmlTag            - XML tag to be evaluated
     * @param documentContext
     * @param resolveItemValues - if true, itemValue tags will be resolved.
     * @return eval itemCollection or null if no tags are contained in the workflow
     *         result.
     * @throws PluginException if the xml structure is invalid
     */
    public ItemCollection evalWorkflowResult(ItemCollection event, String xmlTag, ItemCollection documentContext,
            boolean resolveItemValues) throws PluginException {
        boolean debug = logger.isLoggable(Level.FINE);
        ItemCollection result = new ItemCollection();
        String workflowResult = event.getItemValueString(BPMNUtil.EVENT_ITEM_WORKFLOW_RESULT);
        // support deprecated itemname
        if (workflowResult.isEmpty()) {
            workflowResult = event.getItemValueString("txtActivityResult");
        }
        if (workflowResult.trim().isEmpty()) {
            return null;
        }
        if (xmlTag == null || xmlTag.isEmpty()) {
            logger.warning("cannot eval workflow result - no tag name specified. Verify model!");
            return null;
        }

        // if no <tag exists we skip the evaluation...
        if (workflowResult.indexOf("<" + xmlTag) == -1) {
            return null;
        }

        boolean invalidPattern = false;
        // Fast first test if the tag really exists....
        Pattern patternSimple = Pattern.compile("<" + xmlTag + " (.*?)>(.*?)|<" + xmlTag + " (.*?)./>", Pattern.DOTALL);
        Matcher matcherSimple = patternSimple.matcher(workflowResult);
        if (matcherSimple.find()) {
            invalidPattern = true;
            // we found the starting tag.....

            // Extract all tags with attributes using regex (including empty tags)
            // see also:
            // https://stackoverflow.com/questions/1732348/regex-match-open-tags-except-xhtml-self-contained-tags
            // e.g. <item(.*?)>(.*?)</item>|<item(.*?)./>
            Pattern pattern = Pattern
                    .compile("(?s)(?:(<" + xmlTag + "(?>\\b(?:\".*?\"|'.*?'|[^>]*?)*>)(?<=/>))|(<" + xmlTag
                            + "(?>\\b(?:\".*?\"|'.*?'|[^>]*?)*>)(?<!/>))(.*?)(</" + xmlTag + "\\s*>))", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(workflowResult);
            while (matcher.find()) {
                invalidPattern = false;
                // we expect up to 4 different result groups
                // group 0 contains complete tag string
                // groups 1 or 2 contain the attributes

                String content = "";
                String attributes = matcher.group(1);
                if (attributes == null) {
                    attributes = matcher.group(2);
                    content = matcher.group(3);
                } else {
                    content = matcher.group(2);
                }

                if (content == null) {
                    content = "";
                }

                // now extract the attributes to verify the tag name..
                if (attributes != null && !attributes.isEmpty()) {
                    // parse attributes...
                    String spattern = "(\\S+)=[\"']?((?:.(?![\"']?\\s+(?:\\S+)=|[>\"']))+.)[\"']?";
                    Pattern attributePattern = Pattern.compile(spattern);
                    Matcher attributeMatcher = attributePattern.matcher(attributes);
                    Map<String, String> attrMap = new HashMap<String, String>();
                    while (attributeMatcher.find()) {
                        String attrName = attributeMatcher.group(1); // name
                        String attrValue = attributeMatcher.group(2); // value
                        attrMap.put(attrName, attrValue);
                    }

                    String tagName = attrMap.get("name");
                    if (tagName == null) {
                        throw new PluginException(ResultPlugin.class.getSimpleName(), INVALID_TAG_FORMAT,
                                "<" + xmlTag + "> tag contains no name attribute.");
                    }

                    // now add optional attributes if available
                    for (String attrName : attrMap.keySet()) {
                        // we need to skip the 'name' attribute
                        if (!"name".equals(attrName)) {
                            result.appendItemValue(tagName + "." + attrName, attrMap.get(attrName));
                        }
                    }

                    // test if the type attribute was provided to convert content?
                    String sType = result.getItemValueString(tagName + ".type");
                    String sFormat = result.getItemValueString(tagName + ".format");
                    if (!sType.isEmpty()) {
                        // convert content type
                        if ("boolean".equalsIgnoreCase(sType)) {
                            result.appendItemValue(tagName, Boolean.valueOf(content));
                        } else if ("integer".equalsIgnoreCase(sType)) {
                            try {
                                result.appendItemValue(tagName, Integer.valueOf(content));
                            } catch (NumberFormatException e) {
                                // append 0 value
                                result.appendItemValue(tagName, new Integer(0));
                            }
                        } else if ("double".equalsIgnoreCase(sType)) {
                            try {
                                result.appendItemValue(tagName, Double.valueOf(content));
                            } catch (NumberFormatException e) {
                                // append 0 value
                                result.appendItemValue(tagName, new Double(0));
                            }
                        } else if ("float".equalsIgnoreCase(sType)) {
                            try {
                                result.appendItemValue(tagName, Float.valueOf(content));
                            } catch (NumberFormatException e) {
                                // append 0 value
                                result.appendItemValue(tagName, new Float(0));
                            }
                        } else if ("long".equalsIgnoreCase(sType)) {
                            try {
                                result.appendItemValue(tagName, Long.valueOf(content));
                            } catch (NumberFormatException e) {
                                // append 0 value
                                result.appendItemValue(tagName, new Long(0));
                            }
                        } else if ("date".equalsIgnoreCase(sType)) {
                            if (content == null || content.isEmpty()) {
                                // no value available - no op!
                                if (debug) {
                                    logger.finer("......can not convert empty string into date object");
                                }
                            } else {
                                // convert content value to date object
                                try {
                                    if (debug) {
                                        logger.finer("......convert string into date object");
                                    }
                                    Date dateResult = null;
                                    if (sFormat == null || sFormat.isEmpty()) {
                                        // use standard format short/short
                                        dateResult = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                                                .parse(content);
                                    } else {
                                        // use given formatter (see: TextItemValueAdapter)
                                        DateFormat dateFormat = new SimpleDateFormat(sFormat);
                                        dateResult = dateFormat.parse(content);
                                    }
                                    result.appendItemValue(tagName, dateResult);
                                } catch (ParseException e) {
                                    if (debug) {
                                        logger.log(Level.FINER, "failed to convert string into date object: {0}",
                                                e.getMessage());
                                    }
                                }
                            }

                        } else
                            // no type conversion
                            result.appendItemValue(tagName, content);
                    } else {
                        // no type definition
                        result.appendItemValue(tagName, content);
                    }

                } else {
                    throw new PluginException(ResultPlugin.class.getSimpleName(), INVALID_TAG_FORMAT,
                            "<" + xmlTag + "> tag contains no name attribute.");

                }
            }
        }

        // test for general invalid format
        if (invalidPattern) {
            throw new PluginException(ResultPlugin.class.getSimpleName(), INVALID_TAG_FORMAT,
                    "invalid <" + xmlTag + "> tag format in workflowResult: " + workflowResult
                            + "  , expected format is <"
                            + xmlTag + " name=\"...\">...</item> ");
        }
        return result;
    }

    /**
     * This method register all plugin classes listed in the model profile
     * 
     * @throws PluginException
     * @throws ModelException
     */
    @SuppressWarnings("unchecked")
    protected void registerPlugins(WorkflowKernel workflowkernel, BPMNModel model)
            throws PluginException, ModelException {
        boolean debug = logger.isLoggable(Level.FINE);
        // Fetch the current Profile Entity for this version.

        ItemCollection profile = this.getModelManager().loadDefinition(model);// model.getDefinition();

        // register plugins defined in the environment.profile ....
        List<String> vPlugins = (List<String>) profile.getItemValue("txtPlugins");
        for (int i = 0; i < vPlugins.size(); i++) {
            String aPluginClassName = vPlugins.get(i);

            Plugin aPlugin = findPluginByName(aPluginClassName);
            // aPlugin=null;
            if (aPlugin != null) {
                // register injected CDI Plugin
                if (debug) {
                    logger.log(Level.FINEST, "......register CDI plugin class: {0}...", aPluginClassName);
                }
                workflowkernel.registerPlugin(aPlugin);
            } else {
                // register plugin by class name
                workflowkernel.registerPlugin(aPluginClassName);
            }
        }
    }

    protected void registerAdapters(WorkflowKernel workflowkernel) {
        boolean debug = logger.isLoggable(Level.FINE);

    }

    /**
     * This method updates the workitem metadata. The following items will be
     * updated:
     * 
     * <ul>
     * <li>$creator</li>
     * <li>$editor</li>
     * <li>$lasteditor</li>
     * <li>$participants</li>
     * </ul>
     * <p>
     * The method also migrates deprected items.
     * 
     * @param workitem
     */
    protected void updateMetadata(ItemCollection workitem) {

        // identify Caller and update CurrentEditor
        String nameEditor;
        nameEditor = "";

        // add namCreator if empty
        // migrate $creator (Backward compatibility)
        if (workitem.getItemValueString("$creator").isEmpty() && !workitem.getItemValueString("namCreator").isEmpty()) {
            workitem.replaceItemValue("$creator", workitem.getItemValue("namCreator"));
        }

        if (workitem.getItemValueString("$creator").isEmpty()) {
            workitem.replaceItemValue("$creator", nameEditor);
        }

        // update namLastEditor only if current editor has changed
        if (!nameEditor.equals(workitem.getItemValueString("$editor"))
                && !workitem.getItemValueString("$editor").isEmpty()) {
            workitem.replaceItemValue("$lasteditor", workitem.getItemValueString("$editor"));

        }

        // update $editor
        workitem.replaceItemValue("$editor", nameEditor);
    }

    /**
     * This method returns an injected Plugin by name or null if no plugin with the
     * requested class name is injected.
     * 
     * @param pluginClassName
     * @return plugin class or null if not found
     */
    private Plugin findPluginByName(String pluginClassName) {
        if (pluginClassName == null || pluginClassName.isEmpty())
            return null;
        boolean debug = logger.isLoggable(Level.FINE);

        return null;
    }

}
