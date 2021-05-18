package com.renren.faceos.entity;

public class API {

    private static API instance = new API();

    private API() {
    }

    public static API getInstance() {
        return instance;
    }

    private String baseUrl = "http://api.faceos.com:8181";
    private String IdNamePhotoCheck = "/openapi/IdNamePhotoCheck";
    private String faceLivenessImg = "/openapi/facelivenessImg";


    public String getFacelivenessImg(String appKey, String appScrect) {
        return baseUrl + faceLivenessImg + "?appKey=" + appKey + "&appScrect=" + appScrect;
    }

    public String getIdNamePhotoCheck(String appKey, String appScrect) {
        return baseUrl + IdNamePhotoCheck + "?appKey=" + appKey + "&appScrect=" + appScrect;
    }
}
