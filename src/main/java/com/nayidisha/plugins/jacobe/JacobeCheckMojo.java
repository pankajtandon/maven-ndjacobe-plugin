/**
 * Nayidisha Technologies http://www.nayidisha.com
 * All rights reserved 2009-2011
 *
 */
package com.nayidisha.plugins.jacobe;


import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;


/**
 * Goal: Checks if the files specified in the input are compliant.
 * If even ONE file is not compliant then the execution fails with an exception
 * unless failOnFailure is set to false. (default true)
 *
 * @goal check
 * @phase validate
 * @abstract Pankaj Tandon
 */
public class JacobeCheckMojo extends AbstractJacobeMojo {

    private String separator = "===================================================================================";

    /**
     * failOnError is used to determine if this plugin should throw an exception in case of non-compliance
     * 
     * @parameter expression="${failOnError}" default-value="true"
     */
    protected boolean failOnError;
    
    /**
     * Keep Jacobe files for those files that fail the match.
     * 
     * @parameter expression="${keepJacobeFileForFailedMatches}" default-value="false"
     */
    protected boolean keepJacobeFileForFailedMatches;
    
    private int passedFileCount;
    private int failedFileCount;
    
    // Ensure certain flags are set correctly on the input
    @Override
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
		
        // Do not produce header again
        // if (this.headerFile != null) {
        // cl.addArgument("-header=" + headerFile.getAbsolutePath());
        // }
        
        if (javadoc != null) {
            cl.addArgument("-javadoc=" + javadoc);
        }
        if (noAssert != null && noAssert == true) {
            cl.addArgument("-noassert");
        }
        if (noEnum != null && noEnum == true) {
            cl.addArgument("-noenum");
        }

        // ensure that noBackup is not set
        // ensure that overwrite is not set
		
        if (outputDir != null) {
            cl.addArgument("-outdir=" + outputDir);
        }	

        // Force this extention
        cl.addArgument("-outext=" + "jacobe");
		
        if (input != null) {
            cl.addArgument(input);
        } 

        if (!this.getLog().isDebugEnabled()) {
            cl.addArgument("-quiet");
        }
        return cl;

    }

    public void execute() throws MojoExecutionException, MojoFailureException {
    	
    	if(this.skip){
    		this.getLog().info("Skipping...");
    		return;
    	}
    	
        passedFileCount = 0;
        failedFileCount = 0;
    
        // Clean existing jacobe files if any
        clean();
        
        // Run Jacobe to produce the jacobe files in the same dir (forced via cli options)
        this.runFormatter();

        // Run the check
        this.runCheck();

        this.getLog().info("Jacobe compliance check passed for " + passedFileCount + " files and failed for " + failedFileCount + " files.");
        if (failedFileCount > 0) {
            this.getLog().info(separator);
            this.getLog().info("Ensure that the rule set(specified on the command line) and configuration file used \nfor the formatting by jacobe is the same as that which is configured for this plugin");
            this.getLog().info(separator);

            if (this.failOnError) {
                throw new MojoExecutionException("There are " + failedFileCount + " non-compliant files");
            }
        }
    }
	
    private void runCheck() throws MojoExecutionException {
        File directoryOrFile = null;
        File jacobeFile = null;

        try {
            directoryOrFile = new File(input);

            if (directoryOrFile.exists()) {
                if (directoryOrFile.isDirectory()) {
                    Collection<File> candidateJacobeFileList = null;

                    candidateJacobeFileList = FileUtils.listFiles(directoryOrFile, new String[] { "java.jacobe" }, true);
                    // Loop thru all the jacobe created files to compare and then
                    // delete
                    if (candidateJacobeFileList != null) {
                        for (File jacobeFile1 : candidateJacobeFileList) {
                            if (!compareFiles(jacobeFile1)) {
                                diff(this.getCorrespondingJavaFile(jacobeFile1), jacobeFile1);
                                if (!this.keepJacobeFileForFailedMatches) {
                                    jacobeFile1.delete();
                                }
                            } else {
                                // Delete matching file
                                jacobeFile1.delete();
                            }
                        }
                    } else {
                        this.getLog().info("There are no jacobe files created. Check passed!");
                    }
                } else {
                    // Java file passed in
                    String path = directoryOrFile.getAbsolutePath();
	
                    if (path.endsWith(".java")) {
                        path = path + ".jacobe";
                        jacobeFile = new File(path);
                        if (!compareFiles(jacobeFile)) {
                            diff(this.getCorrespondingJavaFile(jacobeFile), jacobeFile);
                            if (!this.keepJacobeFileForFailedMatches) {
                                jacobeFile.delete();
                            }
                        } else {
                            // Delete matching file
                            jacobeFile.delete();
                        }
                    } else {
                        this.getLog().debug("Skipping check of non-java file at " + path);
                    }
                }
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Error processing file ", e);
        } finally {
            if (!this.keepJacobeFileForFailedMatches) {
                if (jacobeFile != null) {
                    jacobeFile.delete();
                }
            }
        }
    }

    private boolean compareFiles(File jacobeFile) throws IOException, MojoExecutionException {
        boolean boo = false;
        File javaFile = getCorrespondingJavaFile(jacobeFile);

        if (!FileUtils.contentEquals(jacobeFile, javaFile)) {
            String s = "File " + javaFile.getAbsolutePath() + " is not compliant!";

            failedFileCount++;
            this.getLog().error(s);
            boo = false;
        } else {
            passedFileCount++;
            this.getLog().debug("File " + jacobeFile.getAbsolutePath() + " and " + javaFile.getAbsolutePath() + " are compatible!");
            boo = true;
        }
        return boo;
    }

    private File getCorrespondingJavaFile(File jacobeFile) throws IOException {
        String jacobeFilePath = jacobeFile.getCanonicalPath();
        String javaFilePath = StringUtils.replace(jacobeFilePath, ".jacobe", "");
        File javaFile = new File(javaFilePath);

        return javaFile;
    }
	
    // @SuppressWarnings("unchecked")
    private void diff(File file1, File file2) throws Exception {

        // read in lines of each file
        List<String> list1 = FileUtils.readLines(file1);
        List<String> list2 = FileUtils.readLines(file2);

        String[] x = list1.toArray(new String[0]);
        String[] y = list2.toArray(new String[0]);
        // number of lines of each file
        int M = x.length;
        int N = y.length;

        // opt[i][j] = length of LCS of x[i..M] and y[j..N]
        int[][] opt = new int[M + 1][N + 1];

        // compute length of LCS and all subproblems via dynamic programming
        for (int i = M - 1; i >= 0; i--) {
            for (int j = N - 1; j >= 0; j--) {
                if (x[i].equals(y[j])) {
                    opt[i][j] = opt[i + 1][j + 1] + 1;
                } else { 
                    opt[i][j] = Math.max(opt[i + 1][j], opt[i][j + 1]);
                }
            }
        }

        // recover LCS itself and print out non-matching lines to standard output
        int i = 0, j = 0;

        while (i < M && j < N) {
            if (x[i].equals(y[j])) {
                i++;
                j++;
            } else if (opt[i + 1][j] >= opt[i][j + 1]) {
                this.getLog().debug("Java File       (" + (i + 1) + "): " + x[i++]);
            } else {
                this.getLog().debug("Jacobe Formatted(" + (j + 1) + "):" + y[j++]);
            }
        }

        // dump out one remainder of one string if the other is exhausted
        while (i < M || j < N) {
            if (i == M) {
                this.getLog().debug("Jacobe Formatted(" + (j + 1) + "): " + y[j++]);
            } else if (j == N) {
                this.getLog().debug("Java File       (" + (i + 1) + "): " + x[i++]);
            }
        }
        
        this.getLog().info(separator);
    }

}
