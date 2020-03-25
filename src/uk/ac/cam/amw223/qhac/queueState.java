package uk.ac.cam.amw223.qhac;

import java.util.HashMap;
import java.util.Map;

public enum queueState {
    ZERO("0"), FIVE("5"),
    TEN("10"), FIFTEEN("15"),
    TWENTY("20"), TWENTY_FIVE("25"),
    THIRTY("30"), THIRTY_FIVE("35"),
    FORTY("40"), FORTY_FIVE("45"),
    FIFTY("50"), FIFTY_FIVE("55"),
    SIXTY("60"), SIXTY_FIVE("65"),
    SEVENTY("70"), SEVENTY_FIVE("75"),
    EIGHTY("80"), EIGHTY_FIVE("85"),
    NINETY("90"),

    CLOSED("Closed"), CURRENTLY_UNAVAILABLE("Currently Unavailable"),
    ERT("ERT"), OPENS_SOON("Opens 10:00");


    private String name;

    private static Map<String, queueState> queueStates = new HashMap<>();
    static {
        for (queueState state : queueState.values()) {
            queueStates.put(state.name, state);
        }
    }

    queueState(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return String.valueOf(name);
    }

    public static queueState strToState(String name) {
        queueState result = queueStates.get(name);
        if (result == null) {
            return OPENS_SOON;
        } else {
            return result;
        }
    }
}
