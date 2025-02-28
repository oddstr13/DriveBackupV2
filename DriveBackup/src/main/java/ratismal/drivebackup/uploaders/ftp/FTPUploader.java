package ratismal.drivebackup.uploaders.ftp;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPSClient;

import ratismal.drivebackup.uploaders.Uploader;
import ratismal.drivebackup.uploaders.Authenticator.AuthenticationProvider;
import ratismal.drivebackup.UploadThread.UploadLogger;
import ratismal.drivebackup.config.ConfigParser;
import ratismal.drivebackup.config.ConfigParser.Config;
import ratismal.drivebackup.config.configSections.BackupMethods.FTPBackupMethod;
import ratismal.drivebackup.util.MessageUtil;
import ratismal.drivebackup.util.NetUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;

import com.google.api.client.util.Strings;

import static ratismal.drivebackup.config.Localization.intl;

/**
 * Created by Ratismal on 2016-03-30.
 */

public class FTPUploader implements Uploader {
    private UploadLogger logger;

    public static final String UPLOADER_NAME = "(S)FTP";
    public static final String UPLOADER_ID = "ftp";

    private FTPClient ftpClient;
    private SFTPUploader sftpClient;

    private boolean _errorOccurred;

    private String initialRemoteFolder;
    private String _localBaseFolder;
    private String _remoteBaseFolder;
    private String host;

    /**
     * Returns the configured FTP file separator
     * @return the separator
     */
    public static String sep() {
        return ConfigParser.getConfig().advanced.fileSeparator;
    }

    /**
     * Creates an instance of the {@code FTPUploader} object using the server credentials specified by the user in the {@code config.yml}
     */
    public FTPUploader(UploadLogger logger) {
        this.logger = logger;

        try {
            Config config = ConfigParser.getConfig();
            FTPBackupMethod ftp = config.backupMethods.ftp;

            if (ftp.sftp) {
                sftpClient = new SFTPUploader(logger);
            } else {
                connect(ftp.hostname, ftp.port, ftp.username, ftp.password, ftp.ftps);
                host = ftp.hostname;
            }

            _localBaseFolder = ".";
            if (Strings.isNullOrEmpty(ftp.baseDirectory)) {
                _remoteBaseFolder = config.backupStorage.remoteDirectory;
            } else {
                _remoteBaseFolder = ftp.baseDirectory + sep() + config.backupStorage.remoteDirectory;
            }
        } catch (Exception e) {
            MessageUtil.sendConsoleException(e);
            setErrorOccurred(true);
        }
    }

    /**
     * Creates an instance of the {@code FTPUploader} object using the specified credentials
     * @param host the hostname of the FTP server
     * @param port the port
     * @param username the username
     * @param password the password (leave blank if none)
     * @param ftps whether FTP using SSL
     * @param sftp whether FTP using SSH
     * @param publicKey the path to the SSH public key, relative to the "DriveBackupV2 folder" (leave blank if none)
     * @param passphrase the SSH public key passphrase (leave blank if none)
     * @param localBaseFolder the path to the folder which all local file paths are relative to
     * @param remoteBaseFolder the path to the folder which all remote file paths are relative to 
     */
    public FTPUploader(UploadLogger logger, String host, int port, String username, String password, boolean ftps, boolean sftp, String publicKey, String passphrase, String localBaseFolder, String remoteBaseFolder) {
        this.logger = logger;

        try {
            if (sftp) {
                sftpClient = new SFTPUploader(logger, host, port, username, password, publicKey, passphrase, localBaseFolder, remoteBaseFolder);
            } else {
                connect(host, port, username, password, ftps);
                this.host = host;
            }

            _localBaseFolder = localBaseFolder;
            _remoteBaseFolder = remoteBaseFolder;
        } catch (Exception e) {
            MessageUtil.sendConsoleException(e);
            setErrorOccurred(true);
        }        
    }

    /**
     * Authenticates with a server via FTP
     * @param host the hostname of the FTP server
     * @param port the port
     * @param username the username
     * @param password the password (leave blank if none)
     * @param ftps whether FTP using SSL
     * @throws Exception
     */
    private void connect(String host, int port, String username, String password, boolean ftps) throws Exception {
        ftpClient = new FTPClient();
        if (ftps) {
            ftpClient = new FTPSClient();
        }

        ftpClient.connect(host, port);
        ftpClient.login(username, password);

        ftpClient.enterLocalPassiveMode();
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE, FTP.BINARY_FILE_TYPE);
        ftpClient.setFileTransferMode(FTP.STREAM_TRANSFER_MODE);
        ftpClient.setListHiddenFiles(false);

        initialRemoteFolder = ftpClient.printWorkingDirectory();
    }

    public boolean isAuthenticated() {
        if (sftpClient != null) {
            return sftpClient.isAuthenticated();
        } else {
            return ftpClient.isConnected();
        }
    }

    /**
     * Closes the connection to the (S)FTP server
     */
    public void close() {
        try {
            if (sftpClient != null) {
                sftpClient.close();
            } else {
                ftpClient.disconnect();
            }
        } catch (Exception e) {
            MessageUtil.sendConsoleException(e);
            setErrorOccurred(true);
        }
    }

    /**
     * Tests the connection to the (S)FTP server by connecting and uploading a small file
     * @param testFileName name of the test file to upload during the test
     * @param testFileSize the size (in bytes) of the file
     */
    public void test(File testFile) {
        try {
            if (sftpClient != null) {
                sftpClient.test(testFile);
                return;
            }
            
            try (FileInputStream fis = new FileInputStream(testFile)) {
                resetWorkingDirectory();
                createThenEnter(_remoteBaseFolder);
                
                ftpClient.storeFile(testFile.getName(), fis);

                TimeUnit.SECONDS.sleep(5);
                    
                ftpClient.deleteFile(testFile.getName());
            }
        } catch (Exception exception) {
            NetUtil.catchException(exception, host, logger);
            MessageUtil.sendConsoleException(exception);
            setErrorOccurred(true);
        }
    }

    /**
     * Uploads the specified file to the (S)FTP server inside a folder for the specified file type
     * @param file the file
     * @param type the type of file (ex. plugins, world)
     */
    public void uploadFile(File file, String type) {
        try {
            type = type.replace(".."  + sep(), "");

            if (sftpClient != null) {
                sftpClient.uploadFile(file, type);
                return;
            }

            resetWorkingDirectory();
            createThenEnter(_remoteBaseFolder);
            createThenEnter(type);

            FileInputStream fs = new FileInputStream(file);
            ftpClient.storeFile(file.getName(), fs);
            fs.close();

            try {
                pruneBackups(type);
            } catch (Exception e) {
                logger.log(intl("backup-method-prune-failed"));
                
                throw e;
            }
        } catch (Exception exception) {
            NetUtil.catchException(exception, host, logger);
            MessageUtil.sendConsoleException(exception);
            setErrorOccurred(true);
        }
    }

    /**
     * Downloads the specifed file from the (S)FTP server into a folder for the specified file type
     * @param filePath the path of the file
     * @param type the type of file (ex. plugins, world)
     */
    public void downloadFile(String filePath, String type) {
        try {
            if (sftpClient != null) {
                sftpClient.downloadFile(filePath, type);
                return;
            }

            resetWorkingDirectory();
            ftpClient.changeWorkingDirectory(_remoteBaseFolder);

            File outputFile = new File(_localBaseFolder + sep() + type);
            if (!outputFile.exists()) {
                outputFile.mkdirs();
            }

            OutputStream outputStream = new FileOutputStream(_localBaseFolder + "/" + type + "/" + new File(filePath).getName());
            ftpClient.retrieveFile(filePath, outputStream);

            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            MessageUtil.sendConsoleException(e);
            setErrorOccurred(true);
        }
    }

    /**
     * Returns a list of the paths of the files inside the specified folder and subfolders
     * @param folderPath the path of the folder
     * @return the list of file paths
     */
    public ArrayList<String> getFiles(String folderPath) {
        ArrayList<String> filePaths = new ArrayList<>();

        try {
            if (sftpClient != null) {
                return sftpClient.getFiles(folderPath);
            }

            resetWorkingDirectory();
            ftpClient.changeWorkingDirectory(_remoteBaseFolder);
            ftpClient.changeWorkingDirectory(folderPath);

            for (FTPFile file : ftpClient.mlistDir()) {
                if (file.isDirectory()) {
                    // file.getName() = file path
                    filePaths.addAll(prependToAll(getFiles(file.getName()), new File(file.getName()).getName() + sep()));
                } else {
                    filePaths.add(file.getName());
                }
            }
        } catch (Exception e) {
            MessageUtil.sendConsoleException(e);
            setErrorOccurred(true);
        }

        return filePaths;
    }

    /**
     * Gets whether an error occurred while accessing the (S)FTP server
     * @return whether an error occurred
     */
    public boolean isErrorWhileUploading() {
        return this._errorOccurred;
    }

    /**
     * Gets the name of this upload service
     * @return name of upload service
     */
    public String getName() {
        return UPLOADER_NAME;
    }

    /**
     * Gets the id of this upload service
     * @return id of upload service
     */
    public String getId() {
        return UPLOADER_ID;
    }

    public AuthenticationProvider getAuthProvider() {
        return null;
    }

    /**
     * Deletes the oldest files past the number to retain from the FTP server inside the specified folder for the file type
     * <p>
     * The number of files to retain is specified by the user in the {@code config.yml}
     * @param type the type of file (ex. plugins, world)
     * @throws Exception
     */
    private void pruneBackups(String type) throws Exception {
        int fileLimit = ConfigParser.getConfig().backupStorage.keepCount;
        if (fileLimit == -1) {
            return;
        }
        TreeMap<Date, FTPFile> files = getZipFiles();

        if (files.size() > fileLimit) {
            logger.info(
                intl("backup-method-limit-reached"), 
                "file-count", String.valueOf(files.size()),
                "upload-method", getName(),
                "file-limit", String.valueOf(fileLimit));

            while (files.size() > fileLimit) {
                ftpClient.deleteFile(files.firstEntry().getValue().getName());
                files.remove(files.firstKey());
            }
        }
    }

    /**
     * Returns a list of ZIP files and their modification dates inside the current working directory
     * @return the list of files
     * @throws Exception
     */
    private TreeMap<Date, FTPFile> getZipFiles() throws Exception {
        TreeMap<Date, FTPFile> files = new TreeMap<>();

        for (FTPFile file : ftpClient.mlistDir()) {
            if (file.getName().endsWith(".zip"))
                files.put(file.getTimestamp().getTime(), file);
        }

        return files;
    }

    /**
     * Creates a folder with the specified path inside the current working directory, then enters it
     * @param parentFolder the parent folder
     * @param path the relative path of the folder to create
     * @throws Exception
     */
    private void createThenEnter(String path) throws Exception {
        if (!ftpClient.changeWorkingDirectory(path)) {
            ftpClient.makeDirectory(path);
            ftpClient.changeWorkingDirectory(path);
        }
    }

    /**
     * Resets the current working directory to what it was when connection to the SFTP server was established
     * @throws Exception
     */
    private void resetWorkingDirectory() throws Exception {
        ftpClient.changeWorkingDirectory(initialRemoteFolder);
    }

    /**
     * Prepends the specified String to each element in the specified ArrayList
     * @param list the ArrayList
     * @param string the String
     * @return the new ArrayList
     */
    private static ArrayList<String> prependToAll(ArrayList<String> list, String string) {
        for (int i = 0; i < list.size(); i++) {
            list.set(i, string + list.get(i));
        }

        return list;
    }

    /**
     * Sets whether an error occurred while accessing the FTP server
     * @param errorOccurred whether an error occurred
     */
    private void setErrorOccurred(boolean errorOccurred) {
        _errorOccurred = errorOccurred;
    }
}
