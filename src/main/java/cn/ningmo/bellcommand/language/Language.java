package cn.ningmo.bellcommand.language;

public enum Language {
    DEFAULT("messages", "简体中文");
    
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
        return DEFAULT;
    }
} 