package ratismal.drivebackup.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.placeholder.PlaceholderResolver;
import ratismal.drivebackup.config.ConfigParser;
import ratismal.drivebackup.config.ConfigParser.Config;
import ratismal.drivebackup.plugin.DriveBackup;

public class MessageUtil {

    private boolean addPrefix = true;
    private List<Component> message = new ArrayList<Component>();
    private Set<CommandSender> recipients = new HashSet<CommandSender>();
    private Boolean sendToConsole = true;
    

    public static MessageUtil Builder() {
        return new MessageUtil();
    }

    public MessageUtil() {
    }

    public MessageUtil addPrefix(boolean prefix) {
        addPrefix = prefix;
        return this;
    }
    
    public MessageUtil text(String text) {
        message.add(Component.text(text, NamedTextColor.DARK_AQUA));
        return this;
    }

    public MessageUtil emText(String text) {
        message.add(Component.text(text, NamedTextColor.GOLD));
        return this;
    }

    /**
     * Parses & adds MiniMessage formatted text to the message
     * @param input the MiniMessage text
     * @return the calling MessageUtil's instance
     */
    public MessageUtil mmText(String text) {
        message.add(MiniMessage.miniMessage().deserialize("<dark_aqua>" + text));
        return this;
    }

    /**
     * Parses & adds MiniMessage formatted text to the message
     * @param input the MiniMessage text
     * @param placeholders optional MiniMessage placeholders
     * @return the calling MessageUtil's instance
     */
    public MessageUtil mmText(String text, String... placeholders) {
        message.add(MiniMessage.miniMessage().deserialize("<dark_aqua>" + text, PlaceholderResolver.resolving(placeholders)));
        return this;
    }

    /**
     * Parses & adds MiniMessage formatted text to the message
     * @param input the MiniMessage text
     * @param templates optional {@code Template}
     * @return the calling MessageUtil's instance
     */
    public MessageUtil mmText(String text, String title, Component content) {
        message.add(MiniMessage.miniMessage().deserialize("<dark_aqua>" + text, PlaceholderResolver.resolving(title, content)));
        return this;
    }

    public MessageUtil text(Component component) {
        message.add(component);
        return this;
    }

    /**
     * Adds a player to the list of recipients
     * @param player the player to be added to the recipients
     * @return the calling MessageUtil's instance
     */
    public MessageUtil to(CommandSender player) {
        recipients.add(player);
        return this;
    }

    /**
     * Adds a list of players to the list of recipients
     * @param players the list of players to be added to the recipients
     * @return the calling MessageUtil's instance
     */
    public MessageUtil to(List<CommandSender> players) {
        for (CommandSender player : players) {
          recipients.add(player);
        }
        return this;
    }

    /**
     * Adds all online players with the specified permissions to the recipients
     * @param permission the specified permission to be added to the recipients
     * @return the calling MessageUtil's instance
     */
    public MessageUtil toPerm(String permission) {
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
          if (player.hasPermission("drivebackup.linkAccounts") && !recipients.contains(player)) {
              recipients.add(player);
          }
        }
        return this;
    }

    /**
     * Set whether or not if the message should be sent to console
     * @param value boolean
     * @return the calling MessageUtil's instance
     */
    public MessageUtil toConsole(boolean value) {
        sendToConsole = value;
        return this;
    }

    /**
     * Adds all online players to the list of recipients
     * @return the calling MessageUtil's instance
     */
    public MessageUtil all() {
        for (Player p : Bukkit.getOnlinePlayers()) {
          recipients.add(p);
        }
        return this;
    }

    /**
     * Sends the message to the recipients
     */
    public void send() {
        JoinConfiguration seperator = JoinConfiguration.separator(Component.text(" "));
        Component builtComponent = Component.join(seperator, message);
        if (addPrefix) {
            builtComponent = prefixMessage(builtComponent);
        }

        if (sendToConsole) recipients.add(Bukkit.getConsoleSender());

        Config config = (Config) ((ConfigParser.getConfig() != null) ? ConfigParser.getConfig() : ConfigParser.defaultConfig());

        for (CommandSender player : recipients) {
            if (player == null || (!config.messages.sendInChat && player instanceof Player)) {
                continue;
            }

            DriveBackup.adventure.sender(player).sendMessage(builtComponent);
        }
    }

    /**
     * Sends the stack trace corresponding to the specified exception to the console, only if suppress errors is disabled
     * <p>
     * Whether suppress errors is enabled is specified by the user in the {@code config.yml}
     * @param exception Exception to send the stack trace of
     */
    public static void sendConsoleException(Exception exception) {
        Config config = (Config) ((ConfigParser.getConfig() != null) ? ConfigParser.getConfig() : ConfigParser.defaultConfig());
    	if (!config.advanced.suppressErrors) {
    		exception.printStackTrace();
    	}
    }
    
    /**
     * Prefixes the specified message with the plugin name
     * @param message the message to prefix
     * @return the prefixed message
     */
    private static Component prefixMessage(Component message) {
        Config config = (Config) ((ConfigParser.getConfig() != null) ? ConfigParser.getConfig() : ConfigParser.defaultConfig());

        return Component.text(translateMessageColors(config.messages.prefix)).append(message);
    }

    /**
     * Translates the color codes in the specified message to the type used internally
     * @param message the message to translate
     * @return the translated message
     */
    public static String translateMessageColors(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}