/*
 * Copyright 2011 Allan Saddi <allan@saddi.com>
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.permissions.Permissible;
import org.tyrannyofheaven.bukkit.util.permissions.PermissionUtils;

/**
 * Holds a chain of CommandInvocations. Used for generating the usage string.
 * 
 * @author asaddi
 */
final class InvocationChain {

    private final List<CommandInvocation> chain;

    private InvocationChain(List<CommandInvocation> chain) {
        this.chain = chain;
    }

    InvocationChain() {
        this(new ArrayList<CommandInvocation>());
    }

    // Adds a new invocation to the chain
    void addInvocation(String label, CommandMetaData commandMetaData) {
        chain.add(new CommandInvocation(label, commandMetaData));
    }

    // Generate a usage string
    String getUsageString() {
        boolean first = true;
        
        StringBuilder usage = new StringBuilder();
        for (Iterator<CommandInvocation> i = chain.iterator(); i.hasNext();) {
            CommandInvocation ci = i.next();
            if (first) {
                usage.append('/');
                first = false;
            }
            usage.append(ci.getLabel());
            
            CommandMetaData cmd = ci.getCommandMetaData();
            if (!cmd.getFlagOptions().isEmpty() || !cmd.getPositionalArguments().isEmpty()) {
                usage.append(' ');
                
                for (Iterator<OptionMetaData> j = cmd.getFlagOptions().iterator(); j.hasNext();) {
                    OptionMetaData omd = j.next();
                    usage.append('[');
                    usage.append(omd.getName());
                    if (omd.getType() != Boolean.class && omd.getType() != Boolean.TYPE) {
                        // Show a value
                        usage.append(" <");
                        usage.append(omd.getValueName());
                        usage.append('>');
                    }
                    usage.append(']');
                    if (j.hasNext())
                        usage.append(' ');
                }
                
                for (Iterator<OptionMetaData> j = cmd.getPositionalArguments().iterator(); j.hasNext();) {
                    OptionMetaData omd = j.next();
                    if (omd.isOptional())
                        usage.append('[');
                    usage.append('<');
                    usage.append(omd.getName());
                    usage.append('>');
                    if (omd.isOptional())
                        usage.append(']');
                    if (j.hasNext())
                        usage.append(' ');
                }
            }
            
            if (i.hasNext())
                usage.append(' ');
        }
        return usage.toString();
    }

    // Tests whether the given permissible can execute this entire chain
    boolean canBeExecutedBy(Permissible permissible) {
        for (CommandInvocation ci : chain) {
            if (!PermissionUtils.hasPermissions(permissible, ci.getCommandMetaData().isRequireAll(), ci.getCommandMetaData().getPermissions()))
                return false;
        }
        return true;
    }

    // Returns a copy of this chain
    InvocationChain copy() {
        // Feh to clone()
        return new InvocationChain(new ArrayList<CommandInvocation>(chain));
    }

}