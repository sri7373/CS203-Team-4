package com.smu.tariff.mtech;

public class ImportResultDto {
    private int importedCount;
    private int skippedCount;
    private String message;

    public ImportResultDto() { }

    public ImportResultDto(int importedCount, int skippedCount, String message) {
        this.importedCount = importedCount;
        this.skippedCount = skippedCount;
        this.message = message;
    }

    public int getImportedCount() { return importedCount; }
    public void setImportedCount(int importedCount) { this.importedCount = importedCount; }
    public int getSkippedCount() { return skippedCount; }
    public void setSkippedCount(int skippedCount) { this.skippedCount = skippedCount; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
