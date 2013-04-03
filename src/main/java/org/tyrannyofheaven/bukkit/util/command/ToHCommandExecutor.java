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

import static org.tyrannyofheaven.bukkit.util.ToHLoggingUtils.error;
import static org.tyrannyofheaven.bukkit.util.ToHLoggingUtils.warn;
import static org.tyrannyofheaven.bukkit.util.ToHMessageUtils.sendMessage;
import static org.tyrannyofheaven.bukkit.util.ToHStringUtils.hasText;
import static org.tyrannyofheaven.bukkit.util.permissions.PermissionUtils.displayPermissionException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.Plugin;
import org.tyrannyofheaven.bukkit.util.ToHStringUtils;
import org.tyrannyofheaven.bukkit.util.permissions.PermissionException;

/**
 * A Bukkit CommandExecutor implementation that ties everything together.
 * 
 * @author asaddi
 */
public class ToHCommandExecutor<T extends Plugin> implements TabExecutor {

    private final T plugin;

    private final HandlerExecutor<T> rootHandlerExecutor;

    private UsageOptions usageOptions = new DefaultUsageOptions();

    private Map<String, TypeCompleter> typeCompleterRegistry = new HashMap<String, TypeCompleter>();

    private boolean quoteAware = false;

    /**
     * Create an instance.
     * 
     * @param plugin the associated plugin
     * @param handlers the handler objects
     */
    public ToHCommandExecutor(T plugin, Object... handlers) {
        if (plugin == null)
            throw new IllegalArgumentException("plugin cannot be null");

        this.plugin = plugin;

        rootHandlerExecutor = new HandlerExecutor<T>(plugin, usageOptions, handlers);
        
        // Register default TypeCompleters
        registerTypeCompleter("constant", new ConstantTypeCompleter());
        registerTypeCompleter("player", new PlayerTypeCompleter());
        registerTypeCompleter("world", new WorldTypeCompleter());
    }

    /**
     * Register top-level commands with the server.
     */
    public void registerCommands() {
        rootHandlerExecutor.registerCommands(this);
    }

    public ToHCommandExecutor<T> registerTypeCompleter(String name, TypeCompleter typeCompleter) {
        if (!ToHStringUtils.hasText(name))
            throw new IllegalArgumentException("name must have a value");
        if (typeCompleter == null)
            throw new IllegalArgumentException("typeCompleter cannot be null");
        
        typeCompleterRegistry.put(name, typeCompleter);
        return this;
    }

    public ToHCommandExecutor<T> setUsageOptions(UsageOptions usageOptions) {
        if (usageOptions == null)
            throw new IllegalArgumentException("usageOptions cannot be null");
        
        this.usageOptions = usageOptions;
        return this;
    }

    public ToHCommandExecutor<T> setQuoteAware(boolean quoteAware) {
        this.quoteAware = quoteAware;
        return this;
    }

    /* (non-Javadoc)
     * @see org.bukkit.command.CommandExecutor#onCommand(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        InvocationChain invChain = new InvocationChain();

        if (quoteAware)
            args = split(ToHStringUtils.delimitedString(" ", (Object[])args));

        try {
            // NB: We use command.getName() rather than label. This allows the
            // user to freely add aliases by editing plugin.yml. However,
            // this also makes aliases in @Command mostly useless.
            rootHandlerExecutor.execute(sender, command.getName(), label, args, invChain, new CommandSession());
            return true;
        }
        catch (PermissionException e) {
            displayPermissionException(sender, e);
            return true;
        }
        catch (ParseException e) {
            // Show message if one was given
            if (hasText(e.getMessage()))
                sendMessage(sender, "%s%s", ChatColor.RED, e.getMessage());
            sendMessage(sender, invChain.getUsageString(usageOptions));
            return true;
        }
        catch (Error e) {
            // Re-throw Errors
            throw e;
        }
        catch (Throwable t) {
            sendMessage(sender, ChatColor.RED + "Plugin error; see server log.");
            error(plugin, "Command handler exception:", t);
            return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (quoteAware) {
            // Isolate query argument (last argument)
            String query;
            String[] argsNoQuery;
            if (args.length > 0) {
                // Have at least one
                query = args[args.length - 1];
                argsNoQuery = Arrays.copyOfRange(args, 0, args.length - 1);
            }
            else {
                query = "";
                argsNoQuery = args;
            }

            args = split(ToHStringUtils.delimitedString(" ", (Object[])argsNoQuery));
            // Extend and add query
            args = Arrays.copyOfRange(args, 0, args.length + 1);
            args[args.length - 1] = query;
        }

        try {
            return rootHandlerExecutor.getTabCompletions(sender, command.getName(), alias, args, null, null, typeCompleterRegistry);
        }
        catch (PermissionException e) {
            displayPermissionException(sender, e);
            return Collections.emptyList();
        }
        catch (ParseException e) {
            // Show message
            if (hasText(e.getMessage()))
                sendMessage(sender, "%s%s", ChatColor.RED, e.getMessage());
            return Collections.emptyList();
        }
        catch (Error e) {
            throw e;
        }
        catch (Throwable t) {
            warn(plugin, "Tab completion exception:", t);
            return Collections.emptyList();
        }
    }

    private String[] split(String input) {
        List<String> result = new ArrayList<String>();

        SplitState state = SplitState.NORMAL;
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            switch (state) {
            case NORMAL:
                if (c == '\\') {
                    // Start of escape sequence
                    state = SplitState.ESCAPED;
                }
                else if (c == '"') {
                    // Open quotes
                    state = SplitState.QUOTED;
                }
                else {
                    if (current.length() == 0) {
                        // Current token empty, skip leading white spaces
                        if (!Character.isWhitespace(c))
                            current.append(c);
                    }
                    else if (Character.isWhitespace(c)) {
                        // End of token
                        result.add(current.toString());
                        current = new StringBuilder();
                    }
                    else {
                        current.append(c);
                    }
                }
                break;
            case ESCAPED:
            case QUOTED_ESCAPED:
                if (c == '\\') {
                    current.append('\\');
                }
                else if (c == '"') {
                    current.append('"');
                }
                else {
                    // Not a valid escape
                    current.append('\\');
                    current.append(c);
                }
                state = state == SplitState.ESCAPED ? SplitState.NORMAL : SplitState.QUOTED;
                break;
            case QUOTED:
                if (c == '\\') {
                    state = SplitState.QUOTED_ESCAPED;
                }
                else if (c == '"') {
                    // Close quotes
                    state = SplitState.NORMAL;
                }
                else {
                    // Append unconditionally
                    current.append(c);
                }
                break;
            default:
                throw new AssertionError("Unhandled SplitState." + state);
            }
        }

        // Check final token
        if (current.length() > 0)
            result.add(current.toString());

        return result.toArray(new String[result.size()]);
    }

    private static enum SplitState {
        NORMAL, ESCAPED, QUOTED, QUOTED_ESCAPED;
    }

}
