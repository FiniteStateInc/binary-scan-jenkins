package io.jenkins.plugins.finitestateuploadbinaryscan;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.jenkins.cli.shaded.org.apache.commons.lang.StringEscapeUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.servlet.ServletException;
import jenkins.model.Jenkins;
import jline.internal.InputStreamReader;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.interceptor.RequirePOST;

public class UploadBinaryScanRecorder extends Recorder {

    private String finiteStateClientId;
    private String finiteStateSecret;
    private String finiteStateOrganizationContext;
    private String assetId;
    private String version;
    private Boolean externalizableId;
    private String filePath;
    private Boolean quickScan;
    private String businessUnitId;
    private String createdByUserId;
    private String productId;
    private String artifactDescription;
    private String parsedVersion;

    @DataBoundConstructor
    public UploadBinaryScanRecorder(
            String finiteStateClientId,
            String finiteStateSecret,
            String finiteStateOrganizationContext,
            String assetId,
            String version,
            Boolean externalizableId,
            String filePath,
            String businessUnitId,
            String createdByUserId,
            String productId,
            String artifactDescription,
            Boolean quickScan) {
        this.finiteStateClientId = finiteStateClientId;
        this.finiteStateSecret = finiteStateSecret;
        this.finiteStateOrganizationContext = finiteStateOrganizationContext;
        this.assetId = assetId;
        this.quickScan = quickScan;
        this.version = version;
        this.externalizableId = externalizableId;
        this.filePath = filePath;
        this.businessUnitId = businessUnitId;
        this.createdByUserId = createdByUserId;
        this.productId = productId;
        this.artifactDescription = artifactDescription;
    }

    public String getFiniteStateClientId() {
        return finiteStateClientId;
    }

    public String getFiniteStateSecret() {
        return finiteStateSecret;
    }

    public String getFiniteStateOrganizationContext() {
        return finiteStateOrganizationContext;
    }

    public String getAssetId() {
        return assetId;
    }

    public String getVersion() {
        return version;
    }

    public boolean getExternalizableId() {
        return externalizableId;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getBusinessUnitId() {
        return businessUnitId;
    }

    public String getCreatedByUserId() {
        return createdByUserId;
    }

    public String getProductId() {
        return productId;
    }

    public String getArtifactDescription() {
        return artifactDescription;
    }

    public boolean getQuickScan() {
        return quickScan;
    }

    public boolean isQuickScan() {
        return quickScan;
    }

    @DataBoundSetter
    public void setFiniteStateClientId(String finiteStateClientId) {
        this.finiteStateClientId = finiteStateClientId;
    }

    @DataBoundSetter
    public void setFiniteStateSecret(String finiteStateSecret) {
        this.finiteStateSecret = finiteStateSecret;
    }

    @DataBoundSetter
    public void setFiniteStateOrganizationContext(String finiteStateOrganizationContext) {
        this.finiteStateOrganizationContext = finiteStateOrganizationContext;
    }

    @DataBoundSetter
    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    @DataBoundSetter
    public void setVersion(String version) {
        this.version = version;
    }

    @DataBoundSetter
    public void setExternalizableId(boolean externalizableId) {
        this.externalizableId = externalizableId;
    }

    @DataBoundSetter
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    @DataBoundSetter
    public void setQuickScan(boolean quickScan) {
        this.quickScan = quickScan;
    }

    @DataBoundSetter
    public void setBusinessUnitId(String businessUnitId) {
        this.businessUnitId = businessUnitId;
    }

    @DataBoundSetter
    public void setCreatedByUserId(String createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    @DataBoundSetter
    public void setProductId(String productId) {
        this.productId = productId;
    }

    @DataBoundSetter
    public void setArtifactDescription(String artifactDescription) {
        this.artifactDescription = artifactDescription;
    }

    private File getFileFromWorkspace(AbstractBuild build, String relativeFilePath, BuildListener listener) {
        // Get the workspace directory for the current build
        FilePath workspace = build.getWorkspace();
        if (workspace != null) {
            String workspaceRemote = workspace.getRemote();
            // Construct the absolute path to the file
            // Return the file
            return new File(workspaceRemote, relativeFilePath);
        }
        return null;
    }

    /**
     * get secret values from form
     *
     * @param build
     * @param credentialId
     * @return
     */
    public String getSecretTextValue(AbstractBuild build, String credentialId) {
        // Retrieve the credentials by ID
        StandardCredentials credentials =
                CredentialsProvider.findCredentialById(credentialId, StringCredentials.class, build);

        // Check if the credential is of type StringCredentials
        if (credentials instanceof StringCredentials) {
            StringCredentials stringCredentials = (StringCredentials) credentials;
            // Get the secret value
            String secretValue = stringCredentials.getSecret().getPlainText();
            return secretValue;
        } else {
            return null;
        }
    }

    public static String escapeEnvVar(String input) {
        return StringEscapeUtils.escapeJava(input);
    }

    public static String validateEnvVar(String input) {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("Environment variable value cannot be null or empty");
        }
        if (!input.matches("^[a-zA-Z0-9-_]+$")) {
            throw new IllegalArgumentException("Environment variable value contains invalid characters: " + input);
        }
        return input;
    }

    public static boolean isDockerInstalled() {
        ProcessBuilder processBuilder = new ProcessBuilder("docker", "--version");
        try {
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            // Log the exception instead of printing the stack trace
            e.printStackTrace();
        }
        return false;
    }

    private boolean buildDockerImage(File dockerDir, BuildListener listener) {
        ProcessBuilder buildProcessBuilder =
                new ProcessBuilder("docker", "build", "-t", "finite-state-upload", dockerDir.getAbsolutePath());
        buildProcessBuilder.redirectErrorStream(true);
        try {
            Process buildProcess = buildProcessBuilder.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(buildProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    listener.getLogger().println(line);
                }
            }
            int buildExitCode = buildProcess.waitFor();
            if (buildExitCode != 0) {
                listener.getLogger().println("Docker build failed");
                return false;
            }
        } catch (IOException | InterruptedException e) {
            // Log the exception instead of printing the stack trace
            e.printStackTrace();
            listener.getLogger().println("Docker build process encountered an error: " + e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        if (isDockerInstalled()) {
            listener.getLogger().println("Docker is installed");
        } else {
            listener.getLogger().println("Docker is not installed");
            return false;
        }

        String parsedVersion = getExternalizableId() ? build.getExternalizableId() : version;
        String parsedFiniteStateClientId = validateEnvVar(getSecretTextValue(build, finiteStateClientId));
        String parsedFiniteStateSecret = validateEnvVar(getSecretTextValue(build, finiteStateSecret));
        String parsedFiniteStateOrganizationContext =
                validateEnvVar(getSecretTextValue(build, finiteStateOrganizationContext));

        List<String> envList = new ArrayList<>();
        envList.add("INPUT_FINITE-STATE-CLIENT-ID=" + escapeEnvVar(parsedFiniteStateClientId));
        envList.add("INPUT_FINITE-STATE-SECRET=" + escapeEnvVar(parsedFiniteStateSecret));
        envList.add("INPUT_FINITE-STATE-ORGANIZATION-CONTEXT=" + escapeEnvVar(parsedFiniteStateOrganizationContext));
        envList.add("INPUT_ASSET-ID=" + escapeEnvVar(assetId));
        envList.add("INPUT_VERSION=" + escapeEnvVar(parsedVersion));

        envList.add("INPUT_QUICK-SCAN=" + (quickScan ? "true" : "false"));
        envList.add("INPUT_BUSINESS-UNIT-ID=" + escapeEnvVar(businessUnitId));
        envList.add("INPUT_CREATED-BY-USER-ID=" + escapeEnvVar(createdByUserId));
        envList.add("INPUT_PRODUCT-ID=" + escapeEnvVar(productId));
        envList.add("INPUT_ARTIFACT-DESCRIPTION=" + escapeEnvVar(artifactDescription));

        URL resourceUrl = UploadBinaryScanRecorder.class
                .getClassLoader()
                .getResource("io/jenkins/plugins/finitestateuploadbinaryscan/docker/Dockerfile");

        if (resourceUrl == null) {
            listener.getLogger().println("Dockerfile not found");
            return false;
        }
        File dockerfile = new File(resourceUrl.getFile());
        File dockerDir = dockerfile.getParentFile();

        if (!buildDockerImage(dockerDir, listener)) {
            return false;
        }

        buildDockerImage(dockerDir, listener);

        // Step 2: Verify the file exists
        File file = getFileFromWorkspace(build, filePath, listener);
        if (file == null || !file.exists()) {
            listener.getLogger().println("File specified in file path not found: " + filePath);
            return false;
        } else {
            listener.getLogger().println("Found file: " + file.getAbsolutePath());
        }
        envList.add("INPUT_FILE-PATH=/tmp/" + file.getName()); // set env filename

        String hostDirectory = file.getParent();
        String containerDirectoryPath = "/tmp/";

        // Step 3: Run the Docker container
        List<String> runCommand = new ArrayList<>();
        runCommand.add("docker");
        runCommand.add("run");
        runCommand.add("--rm");
        for (String envVar : envList) {
            runCommand.add("-e");
            runCommand.add(envVar);
        }
        runCommand.add("-v");
        runCommand.add(hostDirectory + ":" + containerDirectoryPath);
        runCommand.add("finite-state-upload");

        ProcessBuilder runProcessBuilder = new ProcessBuilder(runCommand);
        runProcessBuilder.redirectErrorStream(true);
        Process runProcess = runProcessBuilder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                listener.getLogger().println(line);
            }
        }

        int runExitCode = runProcess.waitFor();
        if (runExitCode != 0) {
            listener.getLogger().println("Docker run failed");
            return false;
        }
        build.addAction(new UploadBinaryScanAction(assetId));
        return true;
    }

    @Symbol("fs-upload-binary-scan")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        @RequirePOST
        public ListBoxModel doFillFiniteStateClientIdItems(
                @AncestorInPath Item item, @QueryParameter String finiteStateClientId) {
            StandardListBoxModel items = new StandardListBoxModel();
            if (item == null) {
                // Check if the user has the ADMINISTER permission at the Jenkins root level
                if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                    // If not, return the current value without adding any new items
                    return items.includeCurrentValue(finiteStateClientId);
                }
            } else {
                // Check if the user has the EXTENDED_READ or USE_ITEM permissions on the item
                if (!item.hasPermission(Item.EXTENDED_READ) && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                    // If not, return the current value without adding any new item
                    return items.includeCurrentValue(finiteStateClientId);
                }
            }

            // Retrieve a list of credentails in a global context:
            for (StandardCredentials credential : CredentialsProvider.lookupCredentials(
                    StandardCredentials.class, (Item) null, ACL.SYSTEM, Collections.emptyList())) {
                items.add(credential.getId());
            }

            // Return the populated StandardListBoxModel
            return items;
        }

        @RequirePOST
        public ListBoxModel doFillFiniteStateSecretItems(
                @AncestorInPath Item item, @QueryParameter String finiteStateSecret) {
            StandardListBoxModel items = new StandardListBoxModel();
            if (item == null) {
                if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                    return items.includeCurrentValue(finiteStateSecret);
                }
            } else {
                if (!item.hasPermission(Item.EXTENDED_READ) && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return items.includeCurrentValue(finiteStateSecret);
                }
            }
            for (StandardCredentials credential : CredentialsProvider.lookupCredentials(
                    StandardCredentials.class, (Item) null, ACL.SYSTEM, Collections.emptyList())) {
                items.add(credential.getId());
            }
            return items;
        }

        @RequirePOST
        public ListBoxModel doFillFiniteStateOrganizationContextItems(
                @AncestorInPath Item item, @QueryParameter String finiteStateOrganizationContext) {
            StandardListBoxModel items = new StandardListBoxModel();
            if (item == null) {
                if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                    return items.includeCurrentValue(finiteStateOrganizationContext);
                }
            } else {
                if (!item.hasPermission(Item.EXTENDED_READ) && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return items.includeCurrentValue(finiteStateOrganizationContext);
                }
            }
            for (StandardCredentials credential : CredentialsProvider.lookupCredentials(
                    StandardCredentials.class, (Item) null, ACL.SYSTEM, Collections.emptyList())) {
                items.add(credential.getId());
            }
            return items;
        }

        private FormValidation checkRequiredValue(Item item, String value) {
            if (item == null
                    || !item.hasPermission(Item.EXTENDED_READ) && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                return FormValidation.error("You do not have permission to perform this action.");
            }
            if (value == null || value.trim().isEmpty()) {
                return FormValidation.error("This value is required");
            }
            return FormValidation.ok();
        }

        @RequirePOST
        // lgtm[jenkins/no-permission-check]
        public FormValidation doCheckFiniteStateClientId(@AncestorInPath Item item, @QueryParameter String value)
                throws IOException, ServletException {
            return checkRequiredValue(item, value);
        }

        @RequirePOST
        // lgtm[jenkins/no-permission-check]
        public FormValidation doCheckFiniteStateSecret(@AncestorInPath Item item, @QueryParameter String value)
                throws IOException, ServletException {
            return checkRequiredValue(item, value);
        }

        @RequirePOST
        // lgtm[jenkins/no-permission-check]
        public FormValidation doCheckFiniteStateOrganizationContext(
                @AncestorInPath Item item, @QueryParameter String value) throws IOException, ServletException {
            return checkRequiredValue(item, value);
        }

        @RequirePOST
        // lgtm[jenkins/no-permission-check]
        public FormValidation doCheckAssetId(@AncestorInPath Item item, @QueryParameter String value)
                throws IOException, ServletException {
            return checkRequiredValue(item, value);
        }

        @RequirePOST
        // lgtm[jenkins/no-permission-check]
        public FormValidation doCheckVersion(@AncestorInPath Item item, @QueryParameter String value)
                throws IOException, ServletException {
            return checkRequiredValue(item, value);
        }

        @RequirePOST
        // lgtm[jenkins/no-permission-check]
        public FormValidation doCheckFilePath(@AncestorInPath Item item, @QueryParameter String value)
                throws IOException, ServletException {
            return checkRequiredValue(item, value);
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Finite State - Upload Binary Scan";
        }
    }
}
