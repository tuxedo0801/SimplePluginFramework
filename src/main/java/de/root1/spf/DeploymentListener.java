/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.root1.spf;

/**
 *
 * @author achristian
 */
public interface DeploymentListener {

    public void preStop(PluginInterface plugin);

    public void postStop(PluginInterface plugin);

    public void preStart(PluginInterface plugin);

    public void postStart(PluginInterface plugin);

    public void loaded(PluginInterface plugin);
    
}
