package com.example.bank.model;

import java.util.Map;
import java.util.Optional;

/** Which bank issued a card, from its first six digits (Iranian card prefixes). */
public final class BankDirectory {

    public static final String OWN_BANK = "WA Bank";

    private static final Map<String, String> PREFIXES = Map.ofEntries(
            Map.entry("627412", "Eghtesad Novin"), Map.entry("207177", "Export Development Bank"),
            Map.entry("627381", "Ansar Bank"), Map.entry("502229", "Pasargad Bank"), Map.entry("505785", "Iran Zamin Bank"),
            Map.entry("502806", "Shahr Bank"), Map.entry("622106", "Parsian Bank"), Map.entry("502908", "Tosee Taavon Bank"),
            Map.entry("639194", "Parsian Bank"), Map.entry("502910", "Karafarin Bank"), Map.entry("627884", "Parsian Bank"),
            Map.entry("502938", "Dey Bank"), Map.entry("639347", "Pasargad Bank"), Map.entry("505416", "Tourism Bank"),
            Map.entry("636214", "Ayandeh Bank"), Map.entry("505801", "Koosar Institute Bank"), Map.entry("627353", "Tejarat Bank"),
            Map.entry("589210", "Sepah Bank"), Map.entry("589463", "Refah Kargaran Bank"), Map.entry("603769", "Export Development Bank"),
            Map.entry("603770", "Agricultural Bank"), Map.entry("603799", "Bank Melli Iran"),
            Map.entry("606373", "Qarz Al-Hasanah Mehr Iran Bank"), Map.entry("610433", "Mellat Bank"), Map.entry("621986", "Saman Bank"),
            Map.entry("639607", "Sarmayeh Bank"), Map.entry("639346", "Sina Bank"), Map.entry("627961", "Sanat o Madan Bank"),
            Map.entry("627760", "Post Bank"), Map.entry("639599", "Ghavamin Bank"), Map.entry("628023", "Maskan Bank"),
            Map.entry("636795", "Markazi Bank"), Map.entry("639949", "Hekmat Iranian Bank"));

    private BankDirectory() {
    }

    public static Optional<String> bankOfCard(String cardNumber) {
        return cardNumber == null || cardNumber.length() < 6 ? Optional.empty() : Optional.ofNullable(PREFIXES.get(cardNumber.substring(0, 6)));
    }
}
