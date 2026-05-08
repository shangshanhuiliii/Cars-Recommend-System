package com.carsrecommend.system.vo;

public class AuthMenuVO {

    private String code;
    private String label;
    private String path;

    public AuthMenuVO() {
    }

    public AuthMenuVO(String code, String label, String path) {
        this.code = code;
        this.label = label;
        this.path = path;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
