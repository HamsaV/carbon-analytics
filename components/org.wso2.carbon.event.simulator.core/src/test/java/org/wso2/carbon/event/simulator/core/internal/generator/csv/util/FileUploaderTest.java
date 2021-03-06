package org.wso2.carbon.event.simulator.core.internal.generator.csv.util;


import org.apache.commons.io.FilenameUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.event.simulator.core.exception.FileAlreadyExistsException;
import org.wso2.carbon.event.simulator.core.exception.FileLimitExceededException;
import org.wso2.carbon.event.simulator.core.exception.InvalidFileException;
import org.wso2.carbon.event.simulator.core.service.EventSimulatorDataHolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * FileUploaderTest verifies the functionality of uploading CSv files
 */
public class FileUploaderTest {
    private static File testDir = Paths.get("target", "FileUploaderTest").toFile();
    private static String sampleOrderedCSVFile = Paths.get("src", "test", "resources", "files",
            "sample(ordered).csv").toString();
    private static String sampleORDEREDcsv = Paths.get("src", "test", "resources", "files",
            "SAMPLE(ORDERED).csv").toString();
    private static String sampleUnOrderedCSVFile = Paths.get("src", "test", "resources", "files",
            "sample(unordered).csv").toString();
    private static String sampleInsufficientAttributesCSVFile = Paths.get("src", "test", "resources", "files",
            "sample(insufficientAttributes).csv").toString();
    private static String sampleNoTimestampCSVFile = Paths.get("src", "test", "resources", "files",
            "sample(noTimestamp).csv").toString();
    private static String sampleTextFile = Paths.get("src", "test", "resources", "files",
            "sample.txt").toString();

    @BeforeClass
    public void setUp() throws Exception {
        new File(testDir, "tempCSVFolder").mkdirs();
    }

    @BeforeMethod
    public void beforeMethod() throws Exception {
        deleteFile(FilenameUtils.getName(sampleOrderedCSVFile));
        EventSimulatorDataHolder.getInstance().setMaximumFileSize(200);
    }

    @Test
    public void testUploadValidCSV() throws Exception {
        File csvFile = new File(Paths.get(testDir.toString(), "tempCSVFolder", FilenameUtils.getName
                (sampleOrderedCSVFile)).toString());
        uploadFile(sampleOrderedCSVFile);
        Assert.assertTrue(csvFile.exists());

    }

    @Test
    public void testDeleteCSVFIle() throws Exception {
        uploadFile(sampleOrderedCSVFile);
        boolean deleted = FileUploader.getFileUploaderInstance().deleteFile(FilenameUtils.getName(sampleOrderedCSVFile),
                FilenameUtils.concat(testDir.toString(), "tempCSVFolder"));
        Assert.assertTrue(deleted);
        Assert.assertFalse(new File(Paths.get(testDir.toString(), "tempCSVFolder", FilenameUtils
                .getName(sampleOrderedCSVFile)).toString()).exists());
    }

    @Test(expectedExceptions = FileAlreadyExistsException.class)
    public void testCSVFileAlreadyExists() throws Exception {
        uploadFile(sampleOrderedCSVFile);
        uploadFile(sampleOrderedCSVFile);
    }

    @Test(expectedExceptions = InvalidFileException.class)
    public void testUploadInvalidFileType() throws Exception {
        uploadFile(sampleTextFile);
    }

    @Test(expectedExceptions = FileLimitExceededException.class)
    public void testValidateFileSize() throws Exception {
        EventSimulatorDataHolder.getInstance().setMaximumFileSize(60);
        uploadFile(sampleOrderedCSVFile);
    }


    @Test(expectedExceptions = InvalidFileException.class)
    public void testInvalidSourceLocation() throws Exception {
        uploadFile(Paths.get("src", "test", "sample(ordered).csv").toString());
    }

    @Test
    public void testDeleteFileNotExist() throws Exception {
        boolean deleted = FileUploader.getFileUploaderInstance()
                .deleteFile(FilenameUtils.getName(sampleOrderedCSVFile), FilenameUtils.concat(testDir.toString(),
                        "tempCSVFolder"));
        Assert.assertFalse(deleted);
    }

    @Test
    public void testCaseSensitiveFileUpload() throws Exception {
        uploadFile(sampleOrderedCSVFile);
        uploadFile(sampleORDEREDcsv);
        Assert.assertTrue(new File(Paths.get(testDir.toString(), "tempCSVFolder", FilenameUtils.getName
                (sampleOrderedCSVFile)).toString()).exists());
        Assert.assertTrue(new File(Paths.get(testDir.toString(), "tempCSVFolder", FilenameUtils.getName
                (sampleORDEREDcsv)).toString()).exists());
    }

    private void deleteFile(String fileName) {
        try {
            Files.deleteIfExists(Paths.get(Paths.get(testDir.toString(), "tempCSVFolder", fileName).toString()));
        } catch (IOException e) {
            Assert.assertTrue(false);
        }
    }

    private void uploadFile(String filePath) throws Exception {
        FileUploader.getFileUploaderInstance().uploadFile(filePath, FilenameUtils.concat(testDir.toString(),
                "tempCSVFolder"));
    }
}
