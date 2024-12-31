package cn.ningmo.bellcommand.language;

public enum Language {
    ZH_CN("zh_CN", "简体中文"),
    EN_US("en_US", "English"),
    ZH_TW("zh_TW", "繁體中文"),
    JA_JP("ja_JP", "日本語");
    
    private final String code;
    private final String displayName;
    
    Language(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public static Language fromCode(String code) {
        for (Language lang : values()) {
            if (lang.getCode().equalsIgnoreCase(code)) {
                return lang;
            }
        }
        return ZH_CN; // 默认返回简体中文
    }
} 