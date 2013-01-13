/**
 * Nayidisha Technologies http://www.nayidisha.com
 * All rights reserved 2009-2011
 *
 */
package com.nayidisha.plugins.jacobe;


import org.apache.commons.exec.CommandLine;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;


/**
 * Goal: Removes all .jacobe files if they exist
 *
 * @goal clean
 * @abstract Pankaj Tandon
 *
 */
public class JacobeCleanMojo extends AbstractJacobeMojo {

    @Override
    protected CommandLine buildCommandLine() {
        return null;
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
    	if(this.skip){
    		this.getLog().info("Skipping...");
    		return;
    	}
        this.clean();

    }

}
