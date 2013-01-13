/**
 * Nayidisha Technologies http://www.nayidisha.com
 * All rights reserved 2009-2011
 *
 */
package com.nayidisha.plugins.jacobe;


import org.apache.commons.exec.CommandLine;
import org.apache.maven.plugin.MojoExecutionException;


/**
 * Goal: Formats input files according to the cfg file specified
 *
 * @goal format
 */
public class JacobeFormatMojo extends AbstractJacobeMojo {
   
    protected CommandLine buildCommandLine() { 
        CommandLine cl = new CommandLine(this.jacobeExecutable);

        if (rules != null) {
            for (String rule : this.rules) {
                if (!rule.startsWith("--")) {
                    rule = "--" + rule;
                }
                cl.addArgument(rule);
            }
        }
        if (configurationFile != null) {
            cl.addArgument("-cfg=" + configurationFile.toString());
        }
        if (comparisonThreshold != null && comparisonThreshold >= 0.0) {
            cl.addArgument("-cmpth=" + comparisonThreshold.toString());
        }
        if (header != null) {
            cl.addArgument("-header=" + header.getAbsolutePath());
        }
        if (javadoc != null) {
            cl.addArgument("-javadoc=" + javadoc);
        }
        if (noAssert != null && noAssert == true) {
            cl.addArgument("-noassert");
        }
        if (noEnum != null && noEnum == true) {
            cl.addArgument("-noenum");
        }
        if (noBackup != null && noBackup == true) {
            cl.addArgument("-nobackup");
        }		
        if (overwrite != null && overwrite == true) {
            cl.addArgument("-overwrite");
        }
        if (outputDir != null) {
            cl.addArgument("-outdir=" + outputDir);
        }	
        if (outputExtension != null) {
            cl.addArgument("-outext=" + outputExtension);
        }
        if (input != null) {
            cl.addArgument(input);
        } 
        if (!this.getLog().isDebugEnabled()) {
            cl.addArgument("-quiet");
        }
        return cl;
    }

    public void execute() throws MojoExecutionException {
    	if(this.skip){
    		this.getLog().info("Skipping...");
    		return;
    	}
        runFormatter();
    }
}

