package ratismal.drivebackup.handler;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import ratismal.drivebackup.config.ConfigParser;
import ratismal.drivebackup.config.configSections.BackupMethods;

/**
 * Created by Ratismal on 2016-01-20.
 */

public class CommandTabComplete implements TabCompleter {

    /**
     * Command tab completer
     *
     * @param player Player who sent command
     * @param cmd    Command that was sent
     * @param label  Command alias that was used
     * @param args   Arguments that followed command
     * @return List<String> of valid command tab options
     */
    @Override
    public List<String> onTabComplete(CommandSender player, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("drivebackup")) {
            if (args.length == 1) {

                List<String> commandList = new ArrayList<>();
                commandList.add("v");
                commandList.add("help");
                if (player.hasPermission("drivebackup.linkAccounts")) commandList.add("linkaccount");
                if (player.hasPermission("drivebackup.reloadConfig")) commandList.add("reloadconfig");
                if (player.hasPermission("drivebackup.reloadConfig")) commandList.add("debug");
                if (player.hasPermission("drivebackup.getBackupStatus")) commandList.add("status");
                if (player.hasPermission("drivebackup.getNextBackup")) commandList.add("nextbackup");
                if (player.hasPermission("drivebackup.backup")) commandList.add("backup");
                if (player.hasPermission("drivebackup.backup")) commandList.add("test");
                if (player.hasPermission("drivebackup.backup")) commandList.add("update");
                    
                return commandList;
            } else if (args[0].equalsIgnoreCase("linkaccount") && args.length == 2) {

                if (!player.hasPermission("drivebackup.linkAccounts")) {
                    return null;
                }
                
                List<String> commandList = new ArrayList<>();
                commandList.add("googledrive");
                commandList.add("onedrive");
                commandList.add("dropbox");
                
                return commandList;
            } else if (args[0].equalsIgnoreCase("test") && args.length == 2) {

                if (!player.hasPermission("drivebackup.backup")) {
                    return null;
                }
                
                List<String> commandList = new ArrayList<>();

                BackupMethods methods = ConfigParser.getConfig().backupMethods;

                if (methods.googleDrive.enabled) {
                    commandList.add("googledrive");
                }
                if (methods.oneDrive.enabled) {
                    commandList.add("onedrive");
                }
                if (methods.dropbox.enabled) {
                    commandList.add("dropbox");
                }
                if (methods.webdav.enabled) {
                    commandList.add("webdav");
                }
                if (methods.ftp.enabled) {
                    commandList.add("ftp");
                }
                
                return commandList;
            }
        }
        
        return null;
    }

}
