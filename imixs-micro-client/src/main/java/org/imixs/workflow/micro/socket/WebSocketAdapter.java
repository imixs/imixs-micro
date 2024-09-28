/*******************************************************************************
 *  Imixs Workflow 
 *  Copyright (C) 2001, 2011 Imixs Software Solutions GmbH,  
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
 *  	http://www.imixs.org
 *  	http://java.net/projects/imixs-workflow
 *  
 *  Contributors:  
 *  	Imixs Software Solutions GmbH - initial API and implementation
 *  	Ralph Soika - Software Developer
 *******************************************************************************/

package org.imixs.workflow.micro.socket;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.SignalAdapter;
import org.imixs.workflow.engine.WorkflowService;
import org.imixs.workflow.exceptions.AdapterException;
import org.imixs.workflow.exceptions.PluginException;

import jakarta.inject.Inject;
import jakarta.websocket.DeploymentException;

/**
 * This Imixs-Micro WebSocketAdapter class can be used in a BPMN model to start
 * a new workflow using the Imixs-Micro WebSocket API.
 * 
 * 
 * The Adapter can be configured by the BPMN event
 * <p>
 * Example configuration:
 * 
 * <pre>
 * {@code
 
	  <imixs-micro name="CREATE">
		<endpoint>invoice.date</endpoint>
		<model>date</model>
		<task>date</task>
		<event>date</event>
		<items>date</items>
		<debug>false</debug>
	  </imixs-micro>

 * }
 * </pre>
 * 
 * 
 * 
 * @author Ralph Soika
 * @version 1.0
 *
 */

public class WebSocketAdapter implements SignalAdapter {

	public static final String MODE_CREATE = "CREATE";
	public static final String ERROR_CONFIG = "CONFIG-ERROR";
	public static final String ERROR_API = "API-ERROR";
	private static final Logger logger = Logger.getLogger(WebSocketAdapter.class.getName());

	@Inject
	private WorkflowService workflowService;

	@Override
	public ItemCollection execute(ItemCollection workitem, ItemCollection event)
			throws AdapterException, PluginException {

		List<ItemCollection> createDefinitions = workflowService.evalWorkflowResultXML(event, "imixs-micro",
				MODE_CREATE, workitem, false);

		// Test for CREATE definitions
		if (createDefinitions != null && createDefinitions.size() > 0) {

			for (ItemCollection createDefinition : createDefinitions) {
				String endpoint = createDefinition.getItemValueString("endpoint");
				String model = createDefinition.getItemValueString("model");
				int taskID = createDefinition.getItemValueInteger("task");
				int eventID = createDefinition.getItemValueInteger("event");
				String items = createDefinition.getItemValueString("items");
				boolean debug = createDefinition.getItemValueBoolean("debug");
				// create a new WebSocket Client...
				if (debug) {
					logger.info("connecting: " + endpoint + "...");
				}
				WebSocketClient client = null;
				try {
					client = new WebSocketClient(endpoint);
					// session = ContainerProvider.getWebSocketContainer().connectToServer(client,
					// URI.create(endpoint));

					ItemCollection workitemToSend = new ItemCollection();
					workitemToSend.model(model).task(taskID).event(eventID);
					// Copy items...
					String[] itemList = items.split(";");
					for (String itemName : itemList) {
						workitemToSend.setItemValue(itemName, workitem.getItemValue(itemName));
					}
					// send WebSocket request....
					if (debug) {
						logger.info("sending request...");
					}
					client.sendItemCollection(workitemToSend);

				} catch (DeploymentException | IOException e) {
					throw new PluginException(WebSocketAdapter.class.getSimpleName(), "ERROR_CONFIG",
							"Failed to connect to endpoint '" + endpoint + "' : " + e.getMessage(), e);
				} finally {
					// close open web socket!
					if (client != null) {
						if (debug) {
							logger.info("closing connection...");
						}
						client.close();
					}
				}
			}

		}

		return workitem;
	}

}
