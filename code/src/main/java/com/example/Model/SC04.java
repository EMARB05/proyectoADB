package com.example.Model;

import java.util.List;

public final class SC04 {

        private static final String CALLER_ID_NETWORK_DEFAULT_SEQUENCE = "__SC04_CALLER_ID_NETWORK_DEFAULT__";
        private static final String CALLER_ID_HIDE_NUMBER_SEQUENCE = "__SC04_CALLER_ID_HIDE_NUMBER__";
        private static final String CALLER_ID_SHOW_NUMBER_SEQUENCE = "__SC04_CALLER_ID_SHOW_NUMBER__";

        private SC04() {
        }

        public static List<BloquePrueba> crearBloquesAdditionalSettings(String modelo) {
                String modeloNormalizado = modelo == null ? "" : modelo.trim().toUpperCase();

                if (modeloNormalizado.contains("SC04")) {
                        return crearBloquesAdditionalSettingsSC04();
                }

                return crearBloquesAdditionalSettingsSC04();
        }

        private static List<BloquePrueba> crearBloquesAdditionalSettingsSC04() {
                return List.of(
                                new BloquePrueba("SOFT.035.001",
                                                "Caller ID: With \"network default\" selected, make a call and check phone number showed in other device",
                                                CALLER_ID_NETWORK_DEFAULT_SEQUENCE, true, false),

                                new BloquePrueba("SOFT.035.002",
                                                "Caller ID: With \"hide number\" selected, make a call and check phone number showed in other device",
                                                CALLER_ID_HIDE_NUMBER_SEQUENCE, true, false),

                                new BloquePrueba("SOFT.035.003",
                                                "Caller ID: With \"show number\" selected, make a call and check phone number showed in other device",
                                                CALLER_ID_SHOW_NUMBER_SEQUENCE, true, false),

                                new BloquePrueba("SOFT.035.004",
                                                "Test USSD code (*31# / #31#) and APN integrity",
                                                "__SC04_USSD_PRIVATE_APN_CHECK__", true, false),

                                new BloquePrueba("SOFT.035.005",
                                                "Active call waiting",
                                                "__SC04_CALL_WAITING_ACTIVATE__", true, false),

                                new BloquePrueba("SOFT.035.006",
                                                "Check call waiting during a call",
                                                "__SC04_CALL_WAITING_CHECK_DURING__", true, false),

                                new BloquePrueba("SOFT.035.007",
                                                "Deactivate call waiting",
                                                "__SC04_CALL_WAITING_DEACTIVATE__", true, false)
                                        );
                                                
        }
}