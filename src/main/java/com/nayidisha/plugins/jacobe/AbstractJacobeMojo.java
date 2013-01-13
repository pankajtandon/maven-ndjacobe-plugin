/**
 * Nayidisha Technologies http://www.nayidisha.com
 * All rights reserved 2009-2011
 *
 */
package com.nayidisha.plugins.jacobe;


import java.io.File;
import java.util.Collection;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.ShutdownHookProcessDestroyer;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;


/**
 * Nayidisha Jacobe maven plugin mojo.
 * 
 * @author pankajtandon@gmail.com
 * @since 1.0
 */
public abstract class AbstractJacobeMojo extends AbstractMojo {

	/**
	 * Should this goal be skipped?
	 * 
	 * @parameter expression="${jacobe.skip}" default-value="false"
	 */
	protected boolean skip;
	
    /**
     * The jacobe executable.
     * 
     * @parameter expression="${jacobeExecutable}" default-value="jacobe.exe"
     */
    protected File jacobeExecutable;

    /**
     * The .cfg file used by Jacobe
     * 
     * @parameter default-value="sun.cfg"
     */
    protected File configurationFile;

    /**
     * The comparison threshold when comparing header comments
     * 
     * @parameter default-value=0.05
     */
    protected Float comparisonThreshold;

    /**
     * An array of rules in format &lt;id&gt;=[&lt;int&gt;] See
     * http://www.tiobe.com/index.php/content/products/jacobe/Jacobe.html for
     * rules.
     * 
     * @parameter
     */
    protected String[] rules;

    /**
     * The file containing the header comment to insert if the comparison
     * threshold is exceeded.
     * 
     * 
     * @parameter
     */

    protected File header;

    /**
     * Only available in the Professional Edition
     * 
     * @parameter default-value="protected"
     */
    protected String javadoc;

    /**
     * Treat .java files as Java 1.3 sources. The default is to parse all input
     * as Java 1.5 sources.
     * 
     * @parameter default-value=false
     */
    protected Boolean noAssert;

    /**
     * Treat .java files as Java 1.4 sources (unless -noassert is also
     * specified).
     * 
     * @parameter default-value=false
     */
    protected Boolean noEnum;

    /**
     * Do not keep a copy of the original input file.
     * 
     * @parameter default-value=false
     */
    protected Boolean noBackup;

    /**
     * Overwrite the original input file instead of creating a file with the
     * .jacobe extension.
     * 
     * @parameter default-value=false
     */
    protected Boolean overwrite;

    /**
     * Generate output files in the specified directory.Default is to generate
     * output files in the same directory as the input file.
     * 
     * @parameter
     */
    protected String outputDir;

    /**
     * The extension for output files. By default, the .jacobe extension is
     * appended to the input file.
     * 
     * @parameter default-value="jacobe"
     */
    protected String outputExtension;

    /**
     * Directory to look for the java files.
     * 
     * @parameter
     * @required
     */
    protected String input;
    
    protected int formattedFileCount;

    protected abstract CommandLine buildCommandLine();

    protected void runFormatter() throws MojoExecutionException {

        checkMandatoryParameters();
		
        this.getLog().debug(this.buildCommandLine().toString());

        if (!this.checkIfJavaFilesExist()) {
            this.getLog().info("There are no Java files at " + this.input);
            return;
        }
        final DefaultExecutor executor = new DefaultExecutor();
        ExecuteWatchdog watchDog = new ExecuteWatchdog(60000);

        executor.setWatchdog(watchDog);
        PumpStreamHandler streamHandler = new PumpStreamHandler();

        executor.setStreamHandler(streamHandler);
        ShutdownHookProcessDestroyer processDestroyer = new ShutdownHookProcessDestroyer();

        executor.setProcessDestroyer(processDestroyer);
        int exitValue = 0;

        executor.setExitValue(exitValue);
        try {
            exitValue = executor.execute(this.buildCommandLine());
            this.getLog().info("Formatted " + formattedFileCount + " java files!");
        } catch (Exception e) {
            throw new MojoExecutionException("Error with exitValue = " + exitValue, e);
        }
    }

    protected void clean() throws MojoExecutionException {
        File directoryOrFile = null;
        File jacobeFile = null;
        int fileCount = 0;

        try {
            directoryOrFile = new File(input);
            if (directoryOrFile.isDirectory()) {
                Collection<File> jacobeFileList = FileUtils.listFiles(directoryOrFile, new String[] { "java.jacobe" }, true);

                // Loop thru all the jacobe created files to delete
                if (jacobeFileList != null) {
                    for (File jacobeFile1 : jacobeFileList) {
                        this.getLog().info("Deleting:" + jacobeFile1.getAbsolutePath());
                        jacobeFile1.delete();
                        fileCount++;
                    }
                }
            } else {
                // File passed in
				
                String path = directoryOrFile.getAbsolutePath();

                if (path.endsWith(".java")) {
                    path = path + ".jacobe";
                    jacobeFile = new File(path);
                    if (jacobeFile.exists()) {
                        this.getLog().info("Deleting:" + jacobeFile.getAbsolutePath());
                        jacobeFile.delete();
                        fileCount++;
                    }
                }
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Error processing file ", e);
        } finally {
            if (jacobeFile != null) {
                jacobeFile.delete();
            }
            this.getLog().info("Cleaned " + fileCount + " files.");
        }
    }
	
    protected void checkMandatoryParameters() throws MojoExecutionException {
        if (!this.jacobeExecutable.exists()) {
            throw new MojoExecutionException("The Jacobe executable needs to be available for this plugin to run. Please download it from www.jacobe.com and specify the full path (ending in .exe) in the jacobeExecutable parameter");
        }
		
        if (!this.configurationFile.exists()) {
            throw new MojoExecutionException("The full path (ending in .cfg) of a jacobe configuration file, needs to be specified in the configurationFile parameter.");
        }

    }

    protected boolean checkIfJavaFilesExist() {
        boolean boo = false;

        try {
            File directoryOrFile = new File(input);

            if (directoryOrFile.isDirectory()) {
                Collection<File> jacobeFileList = FileUtils.listFiles(directoryOrFile, new String[] { "java" }, true);

                if (jacobeFileList != null && jacobeFileList.size() > 0) {
                    formattedFileCount = jacobeFileList.size();
                    boo = true;
                }
            } else {
                // File passed in
                String path = directoryOrFile.getAbsolutePath();

                if (directoryOrFile.exists() && path.endsWith(".java")) {
                    formattedFileCount = 1;
                    boo = true;
                }
            }
        } catch (Exception e) {
            // squelch
            this.getLog().debug("Error checking for existance of input " + this.input, e);
            // boo remains false
        }
        return boo;
    }

}
