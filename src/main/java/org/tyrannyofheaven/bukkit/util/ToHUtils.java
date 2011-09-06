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
package org.tyrannyofheaven.bukkit.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

/**
 * Miscellaneous utility methods.
 * 
 * @author asaddi
 */
public class ToHUtils {

    private static final int COPY_BUFFER_SIZE = 4096;

    private ToHUtils() {
        throw new AssertionError("Don't instantiate me!");
    }

    /**
     * Copy an InputStream to a file.
     * 
     * @param input the InputStream
     * @param outFile the output File
     * @throws IOException
     */
    public static void copyFile(InputStream input, File outFile) throws IOException {
        OutputStream os = new FileOutputStream(outFile);
        try {
            byte[] buffer = new byte[COPY_BUFFER_SIZE];
            int readLen;
            while ((readLen = input.read(buffer)) != -1) {
                os.write(buffer, 0, readLen);
            }
        }
        finally {
            os.close();
        }
    }

    /**
     * Copy a resource (using a class's classloader) to a file.
     * 
     * @param clazz the class
     * @param resourceName resource name relative to the class
     * @param outFile the output File
     * @throws IOException
     */
    public static void copyResourceToFile(Class<?> clazz, String resourceName, File outFile) throws IOException {
        InputStream is = clazz.getResourceAsStream(resourceName);
        try {
            copyFile(is, outFile);
        }
        finally {
            is.close();
        }
    }

    /**
     * Copies a resource relative to the Plugin class to a file.
     * 
     * @param plugin the plugin
     * @param resourceName resource name relative to plugin's class
     * @param outFile the output file
     * @return true if successful, false otherwise
     */
    public static boolean copyResourceToFile(Plugin plugin, String resourceName, File outFile) {
        try {
            copyResourceToFile(plugin.getClass(), resourceName, outFile);
            return true;
        }
        catch (IOException e) {
            log(plugin, Level.SEVERE, "Error copying %s to %s", resourceName, outFile, e);
            return false;
        }
    }

    // Create a log message
    private static String createLogMessage(Plugin plugin, String format, Object... args) {
        return String.format("[%s] %s", plugin.getDescription().getName(), String.format(format, args));
    }

    /**
     * Log a message.
     * 
     * @param plugin the plugin
     * @param level the log level
     * @param format the format message
     * @param args the format args. Note that if the last argument is a
     *   Throwable, it is removed and passed to {@link java.util.logging.Logger#log(Level, String, Throwable)}.
     *   It will not be available for the format.
     */
    public static void log(Plugin plugin, Level level, String format, Object... args) {
        if (args.length > 0 && args[args.length - 1] instanceof Throwable) {
            // Last argument is a Throwable, treat accordingly
            plugin.getServer().getLogger().log(level, createLogMessage(plugin, format, Arrays.copyOf(args, args.length - 1)), (Throwable)args[args.length - 1]);
        }
        else {
            plugin.getServer().getLogger().log(level, createLogMessage(plugin, format, args));
        }
    }

    /**
     * Convenience method for sending messages. Supports {@link String#format(String, Object...)}
     * formatting and multiple lines.
     * 
     * @param sender the receiver of the message
     * @param format the message format string
     * @param args format arguments
     */
    public static void sendMessage(CommandSender sender, String format, Object... args) {
        String message = String.format(format, args);
        for (String line : message.split("\n")) {
            sender.sendMessage(line);
        }
    }

    /**
     * Returns true if text is non-null and contains non-whitespace characters.
     * 
     * @param text the string to test
     * @return true if text is non-null and contains non-whitespace characters
     */
    public static boolean hasText(String text) {
        return text != null && text.trim().length() > 0;
    }

    /**
     * Returns a string representation of each object, with each object delimited
     * by the given delimiter. Similar to "string join" in other languages.
     * 
     * @param delimiter the string delimiter
     * @param coll a collection of objects
     * @return the delimited string
     */
    public static String delimitedString(String delimiter, Collection<?> coll) {
        StringBuilder sb = new StringBuilder();
        for (Iterator<?> i = coll.iterator(); i.hasNext();) {
            sb.append(i.next());
            if (i.hasNext())
                sb.append(delimiter);
        }
        return sb.toString();
    }

    /**
     * Returns a string representation of each object, with each object delimited
     * by the given delimiter. Similar to "string join" in other languages.
     * 
     * @param delimiter the string delimiter
     * @param objs an array of objects
     * @return the delimited string
     */
    public static String delimitedString(String delimiter, Object... objs) {
        return delimitedString(delimiter, Arrays.asList(new Object[objs.length]));
    }

    /**
     * Throws an AssertionError if test is false.
     * 
     * @param test the test
     * @param message assertion message
     */
    public static void assertTrue(boolean test, String message) {
        if (!test)
            throw new AssertionError(message);
    }

    /**
     * Throws an AssertionError if test is false.
     * 
     * @param test the test
     */
    public static void assertTrue(boolean test) {
        if (!test)
            throw new AssertionError();
    }

    /**
     * Throws an AssertionError if test is true.
     * 
     * @param test the test
     * @param message assertion message
     */
    public static void assertFalse(boolean test, String message) {
        if (test)
            throw new AssertionError(message);
    }

    /**
     * Throws an AssertionError if test is true.
     * 
     * @param test the test
     */
    public static void assertFalse(boolean test) {
        if (test)
            throw new AssertionError();
    }

    // For colorize()
    private static enum ColorizeState {
        TEXT, COLOR_OPEN, COLOR_NAME, COLOR_CLOSE;
    }

    /**
     * Scans text for color tokens and replaces with the appropriate color
     * code. Might be more convenient than string concatenation? Tokens
     * consist of the color name enclosed in curly-braces. Braces may be
     * escaped by doubling.
     * 
     * @param text the text to colorize
     * @return the colorized result
     */
    public static String colorize(String text) {
        if (text == null) return null;
        
        StringBuilder out = new StringBuilder();

        ColorizeState state = ColorizeState.TEXT;
        StringBuilder color = null;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (state == ColorizeState.TEXT) {
                if (c == '{') {
                    state = ColorizeState.COLOR_OPEN;
                }
                else if (c == '}') {
                    state = ColorizeState.COLOR_CLOSE;
                }
                else {
                    out.append(c);
                }
            }
            else if (state == ColorizeState.COLOR_OPEN) {
                if (c == '{') { // Escaped bracket
                    out.append('{');
                    state = ColorizeState.TEXT;
                }
                else if (Character.isUpperCase(c)) {
                    // First character of color name
                    color = new StringBuilder();
                    color.append(c);
                    state = ColorizeState.COLOR_NAME;
                }
                else {
                    // Invalid
                    throw new IllegalArgumentException("Invalid color name");
                }
            }
            else if (state == ColorizeState.COLOR_NAME) {
                if (Character.isUpperCase(c) || c == '_') {
                    color.append(c);
                }
                else if (c == '}') {
                    ChatColor chatColor = ChatColor.valueOf(color.toString());
                    out.append(chatColor);
                    state = ColorizeState.TEXT;
                }
                else {
                    // Invalid
                    throw new IllegalArgumentException("Invalid color name");
                }
            }
            else if (state == ColorizeState.COLOR_CLOSE) {
                // Optional, but for sanity's sake, to keep brackets matched
                if (c == '}') {
                    out.append('}'); // Collapse to single bracket
                }
                else {
                    out.append('}');
                    out.append(c);
                }
                state = ColorizeState.TEXT;
            }
        }
        
        // End of string
        if (state == ColorizeState.COLOR_CLOSE) {
            out.append('}');
        }
        else if (state != ColorizeState.TEXT) {
            // Was in the middle of color name
            throw new IllegalArgumentException("Invalid color name");
        }

        return out.toString();
    }

}
