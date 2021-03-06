/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.event.simulator.core.internal.generator.csv.core;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.event.simulator.core.exception.InvalidConfigException;
import org.wso2.carbon.event.simulator.core.exception.SimulatorInitializationException;
import org.wso2.carbon.event.simulator.core.internal.bean.CSVSimulationDTO;
import org.wso2.carbon.event.simulator.core.internal.generator.EventGenerator;
import org.wso2.carbon.event.simulator.core.internal.generator.csv.util.CSVReader;
import org.wso2.carbon.event.simulator.core.internal.generator.csv.util.FileStore;
import org.wso2.carbon.event.simulator.core.internal.util.EventSimulatorConstants;
import org.wso2.carbon.event.simulator.core.service.EventSimulatorDataHolder;
import org.wso2.carbon.stream.processor.common.exception.ResourceNotFoundException;
import org.wso2.siddhi.core.event.Event;
import org.wso2.siddhi.query.api.definition.Attribute;

import static org.wso2.carbon.event.simulator.core.internal.util.CommonOperations.checkAvailability;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

/**
 * CSVEventGenerator implements EventGenerator interface.
 * This class produces events using csv files
 */
public class CSVEventGenerator implements EventGenerator {
    private final Logger log = LoggerFactory.getLogger(CSVEventGenerator.class);
    private CSVSimulationDTO csvConfiguration;
    private List<Attribute> streamAttributes;
    private long startTimestamp;
    private long endTimestamp;
    /**
     * nextEvent variable holds the next event with least timestamp
     */
    private Event nextEvent;
    private CSVReader csvReader;
    private List<Event> currentTimestampEvents;
    private TreeMap<Long, ArrayList<Event>> eventsMap;


    public CSVEventGenerator() {
    }

    /**
     * init() is used to initialize a CSV event generator
     * performs following actions
     * 1.validate the   by parsing the csv simulation configuration
     * 2.create a CSVSimulationDTO object
     * 2.initialize the start time and end time for timestamps.
     * An even will be sent only if its timestamp falls within the boundaries of the timestamp start timestamp and
     * end time.
     *
     * @param sourceConfig   source configuration object containing configuration for csv simulation
     * @param startTimestamp starting value for timestamp
     * @param endTimestamp   maximum possible event timestamp
     * @throws InvalidConfigException           if invalid configuration is provided for CSV event generation
     * @throws SimulatorInitializationException if the stream or execution plan doesn't exists
     */
    @Override
    public void init(JSONObject sourceConfig, long startTimestamp, long endTimestamp) throws InvalidConfigException {
        csvConfiguration = createCSVConfiguration(sourceConfig);
//            retrieve stream attributes of the stream being simulated
        try {
            streamAttributes = EventSimulatorDataHolder.getInstance().getEventStreamService()
                    .getStreamAttributes(csvConfiguration.getExecutionPlanName(), csvConfiguration.getStreamName());
        } catch (ResourceNotFoundException e) {
            log.error(e.getResourceType().toString().toLowerCase(Locale.ENGLISH).replace("_", " ") + " '" +
                    e.getResourceName() + "' specified for CSV simulation does not exist. Invalid source " +
                    "configuration : " + csvConfiguration.toString(), e);
            throw new SimulatorInitializationException(e.getResourceType().toString().toLowerCase(Locale.ENGLISH)
                    .replace("_", " ") + " '" + e.getResourceName() + "' " + "specified for CSV simulation does" +
                    " not exist. Invalid source configuration : " + csvConfiguration.toString(), e);
        }
        if (log.isDebugEnabled()) {
            log.debug("Initialize CSV generator for file '" + csvConfiguration.getFileName() + "' to simulate" +
                    " stream '" + csvConfiguration.getStreamName() + "'.");
        }
//        initialize timestamp range
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
        if (log.isDebugEnabled()) {
            log.debug("Timestamp range initiated for CSV event generator for stream '" +
                    csvConfiguration.getStreamName() + "'. Timestamp start time : " + startTimestamp + " and" +
                    " timestamp end time : " + endTimestamp);
        }
    }

    /**
     * start() method begins event simulation by creating the first event
     */
    @Override
    public void start() {
        /*
         * if the CSV file is ordered by timestamp, create the first event and assign it as the nextEvent of
         * the generator.
         * else, create a treeMap of events. Retrieve the list of events with least timestamp as currentTimestampEvents
         * and assign the first event of the least timestamp as the nextEvent of the generator
         * */
        csvReader = new CSVReader(csvConfiguration.getFileName(), csvConfiguration.getIsOrdered());
        if (csvConfiguration.getIsOrdered()) {
            nextEvent = csvReader.getNextEvent(csvConfiguration, streamAttributes, startTimestamp,
                    endTimestamp);
        } else {
            currentTimestampEvents = new ArrayList<>();
            eventsMap = new TreeMap<>();
            eventsMap = csvReader.getEventsMap(csvConfiguration, streamAttributes, startTimestamp,
                    endTimestamp);
            if (!eventsMap.isEmpty()) {
                currentTimestampEvents = eventsMap.pollFirstEntry().getValue();
                nextEvent = currentTimestampEvents.get(0);
                currentTimestampEvents.remove(0);
            } else {
                currentTimestampEvents = null;
                nextEvent = null;
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Start CSV generator for file '" + csvConfiguration.getFileName() + "' for simulation of stream" +
                    " '" + csvConfiguration.getStreamName() + "'.");
        }
    }


    /**
     * stop() method is used to release resources used to read CSV file
     */
    @Override
    public void stop() {
        csvReader.closeParser(csvConfiguration.getFileName(), csvConfiguration.getIsOrdered());
        if (log.isDebugEnabled()) {
            log.debug("Stop CSV generator for file '" + csvConfiguration.getFileName() + "' for stream '" +
                    csvConfiguration.getStreamName() + "'.");
        }
    }


    /**
     * poll() returns nextEvent of the generator and assign the event with next least timestamp as the nextEvent
     *
     * @return event with least timestamp
     */
    @Override
    public Event poll() {
        Event tempEvent = null;
        /*
         * nextEvent != null implies that the generator may be able to produce more events. Hence, call getNextEvent()
         * to obtain the next event.
         * if nextEvent == null, return null to indicate that the generator will not be producing any more events
         */
        if (nextEvent != null) {
            tempEvent = nextEvent;
            getNextEvent();
        }
        return tempEvent;
    }


    /**
     * peek() method is used to view the nextEvent of the generator
     *
     * @return the event with least timestamp
     */
    @Override
    public Event peek() {
        return nextEvent;
    }

    /**
     * getStreamName() is used to obtain the name of the stream to which events are generated
     *
     * @return name of the stream
     */
    @Override
    public String getStreamName() {
        return csvConfiguration.getStreamName();
    }


    /**
     * getExecutionPlanName() is used to obtain the name of execution plan which is being simulated
     *
     * @return name of the execution plan
     */
    @Override
    public String getExecutionPlanName() {
        return csvConfiguration.getExecutionPlanName();
    }

    /**
     * getNextEvent() is used to obtain the next event with least timestamp
     */
    @Override
    public void getNextEvent() {
        /*
         * if the CSV file is ordered by timestamp, create next event and assign it as the nextEvent of generator
         * else, assign the next event with current timestamp as nextEvent of generator
         */
        if (csvConfiguration.getIsOrdered()) {
            nextEvent = csvReader.getNextEvent(csvConfiguration, streamAttributes, startTimestamp,
                    endTimestamp);
        } else {
            getNextEventForCurrentTimestamp();
        }
    }


    /**
     * getEventsForNextTimestamp() is used to get list of events with the next least timestamp
     */
    private void getEventsForNextTimestamp() {
        /*
         * if the events map is not empty, it implies that there are more event. Hence, retrieve the list of events with
         * the next least timestamp
         * else, there are no more events, hence list of events with current timestamp is set to null
         * */
        if (!eventsMap.isEmpty()) {
            currentTimestampEvents = eventsMap.pollFirstEntry().getValue();
        } else {
            currentTimestampEvents = null;
        }
        if (log.isDebugEnabled()) {
            log.debug("Get events for next timestamp from CSV generator for file '" + csvConfiguration.getFileName()
                    + "' for stream '" + csvConfiguration.getStreamName() + "'.");
        }
    }


    /**
     * getNextEventForCurrentTimestamp() method is used to retrieve an event with the least timestamp
     */
    private void getNextEventForCurrentTimestamp() {
        /*
         * if currentTimestampEvents != null , it implies that more events will be created by the generator
         * if currentTimestampEvents list is not empty, get the next event in list as nextEvent and remove that even
         * from the list.
         * else, call getEventsForNextTimestamp() to retrieve a list of events with the next least timestamp.
         * if currentTimestampEvents != null after the method call, it implies that more events will be generated.
         * assign the first event in list as nextEvent and remove it from the list.
         * else if currentTimestampEvents == null, it implies that no more events will be created, hence assign null
         * to nextEvent.
         * */
        if (currentTimestampEvents != null) {
            if (!currentTimestampEvents.isEmpty()) {
                nextEvent = currentTimestampEvents.get(0);
                currentTimestampEvents.remove(0);
            } else {
                getEventsForNextTimestamp();
                if (currentTimestampEvents != null) {
                    nextEvent = currentTimestampEvents.get(0);
                    currentTimestampEvents.remove(0);
                } else {
                    nextEvent = null;
                }
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Get next event for current timestamp from CSV generator for file '" +
                    csvConfiguration.getFileName() + "' for stream '" + csvConfiguration.getStreamName() + "'.");
        }
    }

    /**
     * validateCSVConfiguration() validates the source configuration provided for csv simulation
     *
     * @param sourceConfig JSON object containing configuration required for csv simulation
     * @throws InvalidConfigException if configuration is invalid
     */
    @Override
    public void validateSourceConfiguration(JSONObject sourceConfig) throws InvalidConfigException {
        try {
            /*
             * Perform the following checks prior to setting the properties.
             * 1. has
             * 2. isNull
             * 3. isEmpty
             * */
            if (!checkAvailability(sourceConfig, EventSimulatorConstants.STREAM_NAME)) {
                throw new InvalidConfigException("Stream name is required for CSV simulation. Invalid source " +
                        "configuration : " + sourceConfig.toString());
            }
            if (!checkAvailability(sourceConfig, EventSimulatorConstants.EXECUTION_PLAN_NAME)) {
                throw new InvalidConfigException("Execution plan name is required for CSV simulation of stream '" +
                        sourceConfig.getString(EventSimulatorConstants.STREAM_NAME) + "'. Invalid source " +
                        "configuration : " + sourceConfig.toString());
            }
            /*
             * check whether the execution plan has been deployed.
             * if streamAttributes == null, it implies that execution plan has not been deployed yet or the stream
             * name does not exist in the execution plan
             */
            try {
                EventSimulatorDataHolder.getInstance().getEventStreamService().getStreamAttributes(
                        sourceConfig.getString(EventSimulatorConstants.EXECUTION_PLAN_NAME),
                        sourceConfig.getString(EventSimulatorConstants.STREAM_NAME));
            } catch (ResourceNotFoundException e) {
                log.error(e.getResourceType().toString().toLowerCase(Locale.ENGLISH).replace("_", " ") + " '" +
                        e.getResourceName() + "' specified for CSV simulation does not exist. Invalid source" +
                        " configuration : " + sourceConfig.toString(), e);
                throw new InvalidConfigException(e.getResourceType().toString().toLowerCase(Locale.ENGLISH)
                        .replace("_", " ") + " '" + e.getResourceName() + "' " + "specified for CSV simulation does" +
                        " not exist. Invalid source configuration : " + sourceConfig.toString(), e);
            }
            if (!checkAvailability(sourceConfig, EventSimulatorConstants.FILE_NAME)) {
                throw new InvalidConfigException("File name is required for CSV simulation of stream '" +
                        sourceConfig.getString(EventSimulatorConstants.STREAM_NAME) + "'. Invalid source " +
                        "configuration : " + sourceConfig.toString());
            }
            if (checkAvailability(sourceConfig, EventSimulatorConstants.TIMESTAMP_ATTRIBUTE)) {
                /*
                 * check for the availability of the flag isOrdered only if the timestampAttribute is specified since
                 * isOrdered flag indicates whether a csv file is ordered by the timestamp attribute or not
                 * since timestampAttribute specifies a column index, verify that its > 0
                 * */
                if (sourceConfig.getInt(EventSimulatorConstants.TIMESTAMP_ATTRIBUTE) < 0) {
                    throw new InvalidConfigException("Timestamp attribute for CSV simulation of stream '" +
                            sourceConfig.getString(EventSimulatorConstants.STREAM_NAME) + "' must be positive. " +
                            "Invalid source configuration : " + sourceConfig.toString());
                }
                if (!sourceConfig.has(EventSimulatorConstants.IS_ORDERED)
                        || sourceConfig.isNull(EventSimulatorConstants.IS_ORDERED)) {
                    throw new InvalidConfigException("isOrdered flag is required for CSV simulation of stream '" +
                            sourceConfig.getString(EventSimulatorConstants.STREAM_NAME) + "'. Invalid source " +
                            "configuration : " + sourceConfig.toString());
                }
            } else if (checkAvailability(sourceConfig, EventSimulatorConstants.TIMESTAMP_INTERVAL)) {
                if (sourceConfig.getLong(EventSimulatorConstants.TIMESTAMP_INTERVAL) < 0) {
                    throw new InvalidConfigException("Time interval for CSV simulation of stream '" +
                            sourceConfig.getString(EventSimulatorConstants.STREAM_NAME) + "' must be positive. " +
                            "Invalid source configuration : " + sourceConfig.toString());
                }
            }
            if (!checkAvailability(sourceConfig, EventSimulatorConstants.DELIMITER)) {
                throw new InvalidConfigException("Delimiter is required for CSV simulation of stream '" +
                        sourceConfig.getString(EventSimulatorConstants.STREAM_NAME) + "'. Invalid source " +
                        "configuration : " + sourceConfig.toString());
            }
            if (!FileStore.getFileStore().checkExists(sourceConfig.getString(EventSimulatorConstants.FILE_NAME))) {
                throw new InvalidConfigException("File '" + sourceConfig.getString(EventSimulatorConstants.FILE_NAME)
                        + "' required for simulation of stream '" +
                        sourceConfig.getString(EventSimulatorConstants.STREAM_NAME) + "' has not been " +
                        "uploaded. Invalid source config : " + sourceConfig.toString());
            }
        } catch (JSONException e) {
            log.error("Error occurred when accessing CSV simulation configuration of stream '" +
                    sourceConfig.getString(EventSimulatorConstants.STREAM_NAME) + "'. Invalid source configuration " +
                    "provided : " + sourceConfig.toString() + ". ", e);
            throw new InvalidConfigException("Error occurred when accessing CSV simulation configuration of" +
                    " stream '" + sourceConfig.getString(EventSimulatorConstants.STREAM_NAME) + "'. Invalid" +
                    " source configuration provided : " + sourceConfig.toString() + ". ", e);
        }
    }

    /**
     * createCSVConfiguration() creates a csv simulation configuration object
     *
     * @param sourceConfig JSON object containing configuration required to simulate stream
     * @return CSVSimulationDTO containing csv simulation configuration
     */
    private CSVSimulationDTO createCSVConfiguration(JSONObject sourceConfig) throws InvalidConfigException {
        try {
            /*
             * either a timestamp attribute must be specified or the timeInterval between timestamps of 2 consecutive
             * events must be specified.
             * if time interval is specified the timestamp of the first event will be the startTimestamp and
             * consecutive event will have timestamp = last timestamp + time interval
             * if both timestamp attribute and time interval are not specified set time interval to 1 second
             * */
            String timestampAttribute = "-1";
            long timestampInterval = -1;
            /*
             * since the isOrdered timestamp will be retrieved only if the timestamp attribute is specified, set the
             * isOrdered = true as the default value
             * */
            boolean isOrdered = true;
            if (checkAvailability(sourceConfig, EventSimulatorConstants.TIMESTAMP_ATTRIBUTE)) {
                timestampAttribute = sourceConfig.getString(EventSimulatorConstants.TIMESTAMP_ATTRIBUTE);
                isOrdered = sourceConfig.getBoolean(EventSimulatorConstants.IS_ORDERED);
            } else if (checkAvailability(sourceConfig, EventSimulatorConstants.TIMESTAMP_INTERVAL)) {
                timestampInterval = sourceConfig.getLong(EventSimulatorConstants.TIMESTAMP_INTERVAL);
            } else {
                timestampInterval = 1000;
                log.warn("Either timestamp attribute or time interval is required for CSV simulation of stream '" +
                        sourceConfig.getString(EventSimulatorConstants.STREAM_NAME) + "'. Time interval will " +
                        "be set to 1 second for source configuration : " + sourceConfig.toString());
            }
//        create CSVSimulationDTO containing csv simulation configuration
            CSVSimulationDTO csvSimulationConfig = new CSVSimulationDTO();
            csvSimulationConfig.setStreamName(sourceConfig.getString(EventSimulatorConstants.STREAM_NAME));
            csvSimulationConfig.setExecutionPlanName(sourceConfig.getString(EventSimulatorConstants
                    .EXECUTION_PLAN_NAME));
            csvSimulationConfig.setFileName(sourceConfig.getString(EventSimulatorConstants.FILE_NAME));
            csvSimulationConfig.setTimestampAttribute(timestampAttribute);
            csvSimulationConfig.setTimestampInterval(timestampInterval);
            csvSimulationConfig.setDelimiter((String) sourceConfig.get(EventSimulatorConstants.DELIMITER));
            csvSimulationConfig.setIsOrdered(isOrdered);
            return csvSimulationConfig;
        } catch (JSONException e) {
            log.error("Error occurred when accessing CSV simulation configuration of stream '" +
                    sourceConfig.getString(EventSimulatorConstants.STREAM_NAME) + "'. Invalid source configuration " +
                    "provided : " + sourceConfig.toString() + ". ", e);
            throw new InvalidConfigException("Error occurred when accessing CSV simulation configuration of" +
                    " stream '" + sourceConfig.getString(EventSimulatorConstants.STREAM_NAME) + "'. Invalid" +
                    " source configuration provided : " + sourceConfig.toString() + ". ", e);
        }
    }

    @Override
    public String toString() {
        return csvConfiguration.toString();
    }

    @Override
    public void setStartTimestamp(long startTimestamp) {
        this.startTimestamp = startTimestamp;
    }
}
