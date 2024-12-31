package cn.ningmo.bellcommand.update;

public class UpdateInfo {
    private final String currentVersion;
    private final String latestVersion;
    private final String downloadUrl;

    public UpdateInfo(String currentVersion, String latestVersion, String downloadUrl) {
        this.currentVersion = currentVersion;
        this.latestVersion = latestVersion;
        this.downloadUrl = downloadUrl;
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
} 