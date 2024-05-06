package com.finitestate.uploadbinaryscan;

import hudson.model.Run;
import jenkins.model.RunAction2;

public class UploadBinaryScanAction implements RunAction2 {
    private transient Run build;
    private String assetId;

    public UploadBinaryScanAction(String assetId) {
        this.assetId = assetId;
    }

    public String getAssetId() {
        return assetId;
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return "Binary Scan";
    }

    @Override
    public String getUrlName() {
        return "binary_scan";
    }

    @Override
    public void onAttached(Run<?, ?> build) {
        this.build = build;
    }

    @Override
    public void onLoad(Run<?, ?> build) {
        this.build = build;
    }

    public Run getRun() {
        return build;
    }
}
