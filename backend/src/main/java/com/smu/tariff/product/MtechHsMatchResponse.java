package com.smu.tariff.product;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MtechHsMatchResponse {
    private Data data;

    public Data getData() { return data; }
    public void setData(Data data) { this.data = data; }

    public static class Data {
        @JsonProperty("six_digit_codes")
        private MtechHsCodeEntry[] sixDigitCodes;

        public MtechHsCodeEntry[] getSixDigitCodes() { return sixDigitCodes; }
        public void setSixDigitCodes(MtechHsCodeEntry[] sixDigitCodes) { this.sixDigitCodes = sixDigitCodes; }
    }
}
