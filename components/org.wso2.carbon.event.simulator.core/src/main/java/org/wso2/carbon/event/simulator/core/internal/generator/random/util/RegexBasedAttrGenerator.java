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

package org.wso2.carbon.event.simulator.core.internal.generator.random.util;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.event.simulator.core.exception.InvalidConfigException;
import org.wso2.carbon.event.simulator.core.internal.bean.RegexBasedAttributeDTO;
import org.wso2.carbon.event.simulator.core.internal.generator.random.RandomAttributeGenerator;
import org.wso2.carbon.event.simulator.core.internal.util.EventSimulatorConstants;

import static org.wso2.carbon.event.simulator.core.internal.util.CommonOperations.checkAvailability;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * RegexBasedAttrGenerator is used to generate random data using regex provided
 */
public class RegexBasedAttrGenerator implements RandomAttributeGenerator {
    private static final Logger log = LoggerFactory.getLogger(RegexBasedAttrGenerator.class);
    private RegexBasedAttributeDTO regexBasedAttrConfig = new RegexBasedAttributeDTO();


    public RegexBasedAttrGenerator() {
    }

    /**
     * validateAttributeConfiguration() validates the regex based attribute configuration provided a
     *
     * @param attributeConfig JSON object of the custom data attribute configuration
     * @throws InvalidConfigException if the regex provided is incorrect
     */
    @Override
    public void validateAttributeConfiguration(JSONObject attributeConfig) throws InvalidConfigException {
        if (checkAvailability(attributeConfig, EventSimulatorConstants.REGEX_BASED_ATTRIBUTE_PATTERN)) {
            try {
                Pattern.compile(attributeConfig.getString(EventSimulatorConstants.REGEX_BASED_ATTRIBUTE_PATTERN));
            } catch (PatternSyntaxException e) {
                log.error("Invalid regular expression '" + attributeConfig.getString(
                        EventSimulatorConstants.REGEX_BASED_ATTRIBUTE_PATTERN) + "' provided for " +
                        RandomAttributeGenerator.RandomDataGeneratorType.REGEX_BASED + " attribute generation." +
                        " Invalid attribute configuration : " + attributeConfig.toString() + "'. ", e);
                throw new InvalidConfigException("Invalid regular expression '" + attributeConfig.getString(
                        EventSimulatorConstants.REGEX_BASED_ATTRIBUTE_PATTERN) + "' provided for " +
                        RandomAttributeGenerator.RandomDataGeneratorType.REGEX_BASED + " attribute generation. " +
                        "Invalid attribute configuration : " + attributeConfig.toString() + "'. ", e);
            }
        } else {
            throw new InvalidConfigException("Pattern is required for " +
                    RandomAttributeGenerator.RandomDataGeneratorType.REGEX_BASED + " simulation. Invalid attribute " +
                    "configuration : " + attributeConfig.toString());
        }
    }

    /**
     * createRandomAttributeDTO() creates PropertyBasedAttributeDTO using the attribute configuration provided
     *
     * @param attributeConfig attribute configuration for regex based attribute generation
     */
    @Override
    public void createRandomAttributeDTO(JSONObject attributeConfig) {
        regexBasedAttrConfig.setPattern(attributeConfig
                .getString(EventSimulatorConstants.REGEX_BASED_ATTRIBUTE_PATTERN));
    }

    /**
     * Generate data according to given regular expression.
     * It uses  A Java library called Generex for generating String that match the given regular expression
     *
     * @return Generated value
     * @see "https://github.com/mifmif/Generex"
     */
    @Override
    public String generateAttribute() {
//        Generex generex = new Generex(regexBasedAttrConfig.getPattern());
//        return generex.random();
        return null;
    }

    @Override
    public String getAttributeConfiguration() {
        return regexBasedAttrConfig.toString();
    }
}
