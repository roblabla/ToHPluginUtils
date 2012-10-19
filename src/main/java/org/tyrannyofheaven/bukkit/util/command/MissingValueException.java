/*
 * Copyright 2012 Allan Saddi <allan@saddi.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tyrannyofheaven.bukkit.util.command;

import java.util.Map;

/**
 * Mainly used for tab completion support. Contains state of partial parse.
 * 
 * @author asaddi
 */
public class MissingValueException extends ParseException {

    private static final long serialVersionUID = -6084645961780603112L;

    private final OptionMetaData optionMetaData;

    private final boolean parsedPositional;

    private final Map<String, String> options;

    MissingValueException(OptionMetaData optionMetaData, boolean parsedPositional, Map<String, String> options) {
        super("Missing argument: " + optionMetaData.getName());
        this.optionMetaData = optionMetaData;
        this.parsedPositional = parsedPositional;
        this.options = options;
    }

    MissingValueException(OptionMetaData optionMetaData, String flag, boolean parsedPositional, Map<String, String> options) {
        super("Missing value for flag: " + flag); // could use optionMetaData.getName(), but want passed-in name
        this.optionMetaData = optionMetaData;
        this.parsedPositional = parsedPositional;
        this.options = options;
    }

    public OptionMetaData getOptionMetaData() {
        return optionMetaData;
    }

    public boolean isParsedPositional() {
        return parsedPositional;
    }

    public Map<String, String> getOptions() {
        return options;
    }

}
