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
        
        // 移除前面的 'v' 字符并分割
        String v1 = currentVersion.toLowerCase().replace("v", "");
        String v2 = latestVersion.toLowerCase().replace("v", "");
        
        if (v1.equals(v2)) return false;
        
        String[] parts1 = v1.split("[.-]");
        String[] parts2 = v2.split("[.-]");
        
        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            String p1 = i < parts1.length ? parts1[i] : "0";
            String p2 = i < parts2.length ? parts2[i] : "0";
            
            // 尝试将每一部分解析为数字
            try {
                int num1 = Integer.parseInt(p1.replaceAll("\\D", "0"));
                int num2 = Integer.parseInt(p2.replaceAll("\\D", "0"));
                
                if (num2 > num1) return true;
                if (num1 > num2) return false;
            } catch (NumberFormatException e) {
                // 如果解析失败，则按字符串比较
                int res = p2.compareTo(p1);
                if (res > 0) return true;
                if (res < 0) return false;
            }
        }
        
        return false;
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