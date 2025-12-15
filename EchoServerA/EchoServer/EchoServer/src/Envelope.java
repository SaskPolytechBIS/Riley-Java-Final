/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

import java.io.Serializable;

/**
 *
 * @author Micha
 */
public class Envelope implements Serializable{
    private String command; //the name of the command
    private String arg; //additional information required to complete the command  (optional)
    private Object data; //data needed to complete the command (optional)

    public Envelope() {
    }

    public Envelope(String command, String arg, Object data) {
        this.command = command;
        this.arg = arg;
        this.data = data;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getArg() {
        return arg;
    }

    public void setArg(String arg) {
        this.arg = arg;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
    
    
}
