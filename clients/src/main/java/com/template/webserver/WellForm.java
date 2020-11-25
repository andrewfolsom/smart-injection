package com.template.webserver;

import org.apache.tomcat.jni.File;
import org.springframework.web.multipart.MultipartFile;

import javax.tools.FileObject;

public class WellForm {
    private String wellName;
    private String lease;
    private String wellType;
    private String xLoc;
    private String yLoc;
    private String zLoc;
    private String locationType;
    private MultipartFile attachment;

    public WellForm() {
        super();
    }

    public WellForm(String wellName, String lease, String type, String xLoc, String yLoc, String zLoc, String locationType, MultipartFile attachments) {
        this.wellName = wellName;
        this.lease = lease;
        this.wellType = type;
        this.xLoc = xLoc;
        this.yLoc = yLoc;
        this.zLoc = zLoc;
        this.locationType = locationType;
        this.attachment = attachments;
    }


    public String getWellName() {
        return wellName;
    }

    public String getLease() {
        return lease;
    }

    public String getxLoc() {
        return xLoc;
    }

    public String getyLoc() {
        return yLoc;
    }

    public String getzLoc() {
        return zLoc;
    }

    public String getLocationType() {
        return locationType;
    }

    public String getWellType() {
        return wellType;
    }

    public MultipartFile getAttachment() {
        return attachment;
    }
}
