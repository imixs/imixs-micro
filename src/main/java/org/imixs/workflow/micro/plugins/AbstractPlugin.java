/*  
 *  Imixs-Workflow 
 *  
 *  Copyright (C) 2001-2020 Imixs Software Solutions GmbH,  
 *  http://www.imixs.com
 *  
 *  This program is free software; you can redistribute it and/or 
 *  modify it under the terms of the GNU General Public License 
 *  as published by the Free Software Foundation; either version 2 
 *  of the License, or (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful, 
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of 
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 *  General Public License for more details.
 *  
 *  You can receive a copy of the GNU General Public
 *  License at http://www.gnu.org/licenses/gpl.html
 *  
 *  Project: 
 *      https://www.imixs.org
 *      https://github.com/imixs/imixs-workflow
 *  
 *  Contributors:  
 *      Imixs Software Solutions GmbH - Project Management
 *      Ralph Soika - Software Developer
 */

package org.imixs.workflow.micro.plugins;

import org.imixs.workflow.Plugin;
import org.imixs.workflow.WorkflowContext;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.micro.MicroWorkflowService;

/**
 * This abstract class implements different helper methods used by subclasses
 * 
 * @author Ralph Soika
 * @version 1.1
 * @see org.imixs.workflow.WorkflowManager
 * 
 */
public abstract class AbstractPlugin implements Plugin {

    public static final String INVALID_ITEMVALUE_FORMAT = "INVALID_ITEMVALUE_FORMAT";
    public static final String INVALID_PROPERTYVALUE_FORMAT = "INVALID_PROPERTYVALUE_FORMAT";

    private WorkflowContext ctx;
    private MicroWorkflowService workflowService;

    /**
     * Initialize Plugin and get an instance of the EJB Session Context
     */
    public void init(WorkflowContext actx) throws PluginException {
        ctx = actx;
        // get WorkflowService by check for an instance of WorkflowService
        if (actx instanceof MicroWorkflowService) {
            // yes we are running in a WorkflowService EJB
            workflowService = (MicroWorkflowService) actx;
        }
    }

    @Override
    public void close(boolean rollbackTransaction) throws PluginException {

    }

    public WorkflowContext getCtx() {
        return ctx;
    }

    /**
     * Returns an instance of the WorkflowService EJB.
     * 
     * @return
     */
    public MicroWorkflowService getMicroWorkflowService() {
        return workflowService;
    }

}
