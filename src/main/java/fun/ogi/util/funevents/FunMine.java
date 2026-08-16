package fun.ogi.util.funevents;

import static fun.ogi.util.funevents.FunEventsUtil.translateRarity;

public class FunMine {
    private final String server;
    private final String serverRuName;
    private final String mineName;
    private final String rarity;
    private final String nextRarity;
    private final int resetSecondsLeft;

    public FunMine(String server, String serverRuName, String mineName, String rarity,
                   String nextRarity, int resetSecondsLeft) {
        this.server = server;
        this.serverRuName = serverRuName;
        this.mineName = mineName;
        this.rarity = rarity;
        this.nextRarity = nextRarity;
        this.resetSecondsLeft = resetSecondsLeft;
    }

    public String getServer() {
        return server;
    }

    public String getServerRuName() {
        return serverRuName;
    }

    public String getMineName() {
        return mineName;
    }

    public String getRarity() {
        return rarity;
    }

    public String getRarityRu() {
        return translateRarity(rarity);
    }

    public String getNextRarity() {
        return nextRarity;
    }

    public String getNextRarityRu() {
        return translateRarity(nextRarity);
    }

    public int getResetSecondsLeft() {
        return resetSecondsLeft;
    }

    public String getResetFormatted() {
        if (resetSecondsLeft <= 0) return "сейчас";
        int minutes = resetSecondsLeft / 60;
        int seconds = resetSecondsLeft % 60;
        return minutes > 0 ? minutes + " мин " + seconds + " сек" : seconds + " сек";
    }

    @Override
    public String toString() {
        return "Шахта: " + serverRuName + " (" + server + ")"
                + " | " + mineName
                + " | Редкость: " + getRarityRu()
                + " | Следующая: " + getNextRarityRu()
                + " | Сброс через: " + getResetFormatted();
    }
}

