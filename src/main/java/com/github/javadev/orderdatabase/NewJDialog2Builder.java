package com.github.javadev.orderdatabase;

import java.awt.Frame;
public class NewJDialog2Builder {
    private Frame parent;
    private boolean modal;
    private boolean useMysql;
    private String hostName;
    private String dbName;
    private String user;
    private String pass;
    private boolean useXlsx;
    private String xlsxPath;
    private boolean showDbNumber;

    public NewJDialog2Builder() {
    }

    public NewJDialog2Builder setParent(Frame parent) {
        this.parent = parent;
        return this;
    }

    public NewJDialog2Builder setModal(boolean modal) {
        this.modal = modal;
        return this;
    }

    public NewJDialog2Builder setUseMysql(boolean useMysql) {
        this.useMysql = useMysql;
        return this;
    }

    public NewJDialog2Builder setHostName(String hostName) {
        this.hostName = hostName;
        return this;
    }

    public NewJDialog2Builder setDbName(String dbName) {
        this.dbName = dbName;
        return this;
    }

    public NewJDialog2Builder setUser(String user) {
        this.user = user;
        return this;
    }

    public NewJDialog2Builder setPass(String pass) {
        this.pass = pass;
        return this;
    }

    public NewJDialog2Builder setUseXlsx(boolean useXlsx) {
        this.useXlsx = useXlsx;
        return this;
    }

    public NewJDialog2Builder setXlsxPath(String xlsxPath) {
        this.xlsxPath = xlsxPath;
        return this;
    }

    public NewJDialog2Builder setShowDbNumber(boolean showDbNumber) {
        this.showDbNumber = showDbNumber;
        return this;
    }

    public NewJDialog2 createNewJDialog2() {
        return new NewJDialog2(parent, modal, useMysql, hostName, dbName, user, pass, useXlsx, xlsxPath, showDbNumber);
    }
    
}
