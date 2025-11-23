/**
 * Represents time units for simulation speed control
 */
public enum TimeUnit {
    SECONDS("Seconds", 1.0 / 86400.0),
    MINUTES("Minutes", 1.0 / 1440.0),
    HOURS("Hours", 1.0 / 24.0),
    DAYS("Days", 1.0),
    WEEKS("Weeks", 7.0),
    MONTHS("Months", 30.0),
    YEARS("Years", 365.25);

    final String label;
    final double daysPerUnit;

    TimeUnit(String label, double daysPerUnit) {
        this.label = label;
        this.daysPerUnit = daysPerUnit;
    }

    @Override
    public String toString() {
        return label;
    }
}