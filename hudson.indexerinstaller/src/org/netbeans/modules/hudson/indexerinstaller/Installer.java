/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.hudson.indexerinstaller;

import hudson.cli.CLI;
import java.io.IOException;
import java.net.URL;

/**
 *
 * @author Jan Becicka
 */
public class Installer {
    
    private CLI cli;
    public Installer(String hudson) throws IOException, InterruptedException {
        this.cli = new CLI(new URL(hudson));
    }
    
    public int restartHudson() {
        return this.cli.execute("restart"); //NOI18N
    }
    
    public int installPlugin(String plugin) {
        return this.cli.execute("install-plugin", plugin); //NOI18N
    }
    
}
