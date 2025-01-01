package cn.ningmo.bellcommand.update;

public class UpdateInfo {
    private final String currentVersion;
    private final String latestVersion;
    private final String downloadUrl;
    private final String description;

    public UpdateInfo(String currentVersion, String latestVersion, String downloadUrl) {
        this(currentVersion, latestVersion, downloadUrl, null);
    }

    public UpdateInfo(String currentVersion, String latestVersion, String downloadUrl, String description) {
        this.currentVersion = currentVersion;
        this.latestVersion = latestVersion;
        this.downloadUrl = downloadUrl;
        this.description = description;
    }

    public boolean isUpdateAvailable() {
        if (latestVersion == null || currentVersion == null) return false;
        
        String[] current = currentVersion.split("\\.");
        String[] latest = latestVersion.split("\\.");
        
        for (int i = 0; i < Math.min(current.length, latest.length); i++) {
            int currentPart = Integer.parseInt(current[i]);
            int latestPart = Integer.parseInt(latest[i]);
            
            if (latestPart > currentPart) {
                return true;
            } else if (currentPart > latestPart) {
                return false;
            }
        }
        
        return latest.length > current.length;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public String getDescription() {
        return description;
    }
} 