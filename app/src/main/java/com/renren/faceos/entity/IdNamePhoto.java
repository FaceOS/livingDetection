package com.renren.faceos.entity;

public class IdNamePhoto {


    /**
     * loginName : ***
     * pwd : ***
     * serviceName : IdNamePhotoCheck
     * param : {"name":"***","idCard":"***","image":"***"}
     */

    private String loginName;
    private String pwd;
    private String serviceName;
    private ParamBean param;

    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public ParamBean getParam() {
        return param;
    }

    public void setParam(ParamBean param) {
        this.param = param;
    }

    public static class ParamBean {
        /**
         * name : ***
         * idCard : ***
         * image : ***
         */

        private String name;
        private String idCard;
        private String image;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getIdCard() {
            return idCard;
        }

        public void setIdCard(String idCard) {
            this.idCard = idCard;
        }

        public String getImage() {
            return image;
        }

        public void setImage(String image) {
            this.image = image;
        }
    }
}
