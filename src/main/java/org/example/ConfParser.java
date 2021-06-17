package org.example;

import java.util.HashMap;

public class ConfParser {
    HashMap<String, String> confMap;

    public ConfParser(String s) {
        confMap = new HashMap<>();

        String[] sArr = s.split("\n");
        for (String sLine : sArr) {
            StringBuilder keyBuilder = new StringBuilder();
            StringBuilder valueBuilder = new StringBuilder();
            boolean acqKey = false;
            boolean acqValue = false;
            boolean addToBuilder = false;
            boolean escape = false;
            for (int i = 0; i < sLine.length(); i++) {
                if (sLine.charAt(i) == '"') {
                    if (!escape) {
                        //if not escaped
                        if (!acqKey) {
                            //if false
                            acqKey = true;
                            addToBuilder = true;
                        } else {
                            //if true
                            if (addToBuilder) {
                                if (acqValue) {
                                    confMap.put(keyBuilder.toString(), valueBuilder.toString());
                                    break;
                                } else {
                                    addToBuilder = false;
                                }
                            } else {
                                acqValue = true;
                                addToBuilder = true;
                            }
                        }
                    } else {
                        //if escaped
                        if (addToBuilder) {
                            if (acqKey) {
                                if (acqValue) {
                                    valueBuilder.append(sLine.charAt(i));
                                } else {
                                    keyBuilder.append(sLine.charAt(i));
                                }
                            }
                        }
                        escape = false;
                    }
                } else if (sLine.charAt(i) == '#') {
                    if (!acqKey && !acqValue) {
                        break;
                    }
                } else if (sLine.charAt(i) == '\\') {
                    if (!escape) {
                        escape = true;
                    } else {
                        //if escaped
                        if (addToBuilder) {
                            if (acqKey) {
                                if (acqValue) {
                                    valueBuilder.append(sLine.charAt(i));
                                } else {
                                    keyBuilder.append(sLine.charAt(i));
                                }
                            }
                        }
                        escape = false;
                    }
                } else {
                    if (addToBuilder) {
                        if (acqKey) {
                            if (acqValue) {
                                valueBuilder.append(sLine.charAt(i));
                            } else {
                                keyBuilder.append(sLine.charAt(i));
                            }
                        }
                    }
                }
            }
            confMap.put(keyBuilder.toString(), valueBuilder.toString());
        }
    }

    public HashMap<String, String> getConfMap() {
        return confMap;
    }
}
