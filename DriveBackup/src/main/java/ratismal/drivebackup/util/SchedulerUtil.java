package ratismal.drivebackup.util;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.TemporalAccessor;
import java.util.List;

import org.bukkit.Bukkit;

public class SchedulerUtil {
    private static final int TICKS_PER_SECOND = 20;

    /**
     * Cancels the specified tasks
     * @param taskList an List of the IDs of the tasks
     */
    public static void cancelTasks(List<Integer> taskList) {
        for (int task : taskList) {
            Bukkit.getScheduler().cancelTask(task);
        }

        taskList.clear();
    }

    /**
     * Converts the specified number of seconds to game ticks
     * @param seconds the number of seconds
     * @return the number of game ticks
     */
    public static long sToTicks(long seconds) {
        return seconds * TICKS_PER_SECOND;
    }

    /**
     * Parses the time
     * @param time the time, as a String
     * @return the parsed time
     */
    public static TemporalAccessor parseTime(String time) throws IllegalArgumentException {
        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
            .appendOptional(DateTimeFormatter.ofPattern("kk:mm"))
            .appendOptional(DateTimeFormatter.ofPattern("k:mm"))
            .toFormatter();

        return formatter.parse(time);
    }
}
