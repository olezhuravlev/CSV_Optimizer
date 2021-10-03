package com.csvoptimizer;

import java.text.SimpleDateFormat;
import java.util.Locale;

public final class Constants {

    // This class not supposed to be instantiated.
    private Constants() {
    }

    public static final String OPERATION = "operation";
    public static final String COLUMN_NAME = "columnName";

    public static final String COLUMNS = "columns";
    public static final String ROW = "row";

    public static final String PRINT_WRITER = "printWriter";

    public static final Locale DATE_LOCALE = Locale.ENGLISH;

    public static final String DATE_TIME_DELIMITER = "_";
    public static final String DEFAULT_START_DATE = "2021-01-01" + DATE_TIME_DELIMITER + "12:00:00";
    public static final String DATE_FORMAT_INPUT = "yyyy-MM-dd" + DATE_TIME_DELIMITER + "HH:mm:ss";
    public static final String DATE_FORMAT_USER = DATE_FORMAT_INPUT;
    public static final String DATE_FORMAT_GPX = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String DATE_FORMAT_MS = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    public static final SimpleDateFormat DATE_FORMATTER_INPUT = new SimpleDateFormat(DATE_FORMAT_INPUT, DATE_LOCALE);
    public static final SimpleDateFormat DATE_FORMATTER_USER = new SimpleDateFormat(DATE_FORMAT_USER, DATE_LOCALE);
    public static final SimpleDateFormat DATE_FORMATTER_GPX = new SimpleDateFormat(DATE_FORMAT_GPX, DATE_LOCALE);

    public static final int DEFAULT_STEP = 1;

    public static final String CLI_PARAM_FILE = "pars";

    public static final String CLI_PARAM_IN = "in";
    public static final String CLI_PARAM_OUT = "out";
    public static final String CLI_PARAM_STEP = "step";
    public static final String CLI_PARAM_DATE = "date";
    public static final String CLI_PARAM_AVER_COLUMNS = "avercols";
    public static final String CLI_PARAM_AVER_DEPTH = "averdep";
    public static final String CLI_PARAM_NAME_DELIMITER = "=";

    public static final String AVER_COLUMNS = CLI_PARAM_AVER_COLUMNS;
    public static final String AVER_DEPTH = CLI_PARAM_AVER_DEPTH;

    public static final String CLI_ARRAY_BEGIN = "{";
    public static final String CLI_ARRAY_END = "}";
    public static final String CLI_ARRAY_DELIMITER = ",";
    public static final String COMMA_DELIMITER = ",";
    public static final String POINT_DELIMITER = ".";

    public static final String CSV_DELIMITER = COMMA_DELIMITER;

    public static final String INDEX_OUT_OF_BOUND_MESSAGE = "Wrong column index!";

    // Columns used in calculations.
    public static final String TIME_COLUMN_NAME = "time (us)";
    public static final String BARO_ALT_COLUMN_NAME = "BaroAlt (cm)";

    // Additional columns.
    // Column header for date compatible with GPX-format (e.g. "2000-01-01T00:00:41.092541Z").
    public static final String GPX_DATE_COLUMN_HEADER = "gpxDate";

    // Column header for date to be shown in prescribed format.
    public static final String USER_DATE_COLUMN_HEADER = "userDate";

    // Column header for vertical speed calculated from change of barometer altitude.
    public static final String V_SPEED_BARO_HEADER = "vSpeedBaroAlt (cm/s)";

    // Digital representation of flight mode flags.
    public static final String FLIGHT_MODE_HEADER = "flightModeFlags (flags)";
    public static final String FLIGHT_MODE_INDICATOR_HEADER = "flightModeIndicator";

    // Digital representation of state flags.
    public static final String STATE_HEADER = "stateFlags (flags)";
    public static final String STATE_INDICATOR_HEADER = "stateIndicator";

    // Digital representation of failsafe phase.
    public static final String FAILSAFE_PHASE_HEADER = "failsafePhase (flags)";
    public static final String FAILSAFE_PHASE_INDICATOR_HEADER = "failsafePhaseIndicator";

    // Icon to display flight mode, state or failsafe phase.
    public static final String STATUS_ICON_INDICATOR_HEADER = "statusIconIndicator";
}
