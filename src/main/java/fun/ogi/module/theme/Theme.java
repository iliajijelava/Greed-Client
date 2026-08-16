package fun.ogi.module.theme;

public class Theme {
    private String name;
    private String description;
    private ThemeColorPalette palette;
    private boolean favorite;
    private boolean custom;

    public Theme(String name, String description, ThemeColorPalette palette) {
        this.name = name;
        this.description = description;
        this.palette = palette;
        this.favorite = false;
        this.custom = false;
    }

    public Theme(String name, String description, ThemeColorPalette palette, boolean custom) {
        this.name = name;
        this.description = description;
        this.palette = palette;
        this.favorite = false;
        this.custom = custom;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ThemeColorPalette getPalette() {
        return palette;
    }

    public void setPalette(ThemeColorPalette palette) {
        this.palette = palette;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public boolean isCustom() {
        return custom;
    }

    public void setCustom(boolean custom) {
        this.custom = custom;
    }

    public Theme copy() {
        Theme t = new Theme(name, description, ThemeColorPalette.copyOf(palette));
        t.favorite = favorite;
        t.custom = custom;
        return t;

    }
}

