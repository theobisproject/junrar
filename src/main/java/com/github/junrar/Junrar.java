package com.github.junrar;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.junrar.exception.RarException;
import com.github.junrar.impl.FileVolumeManager;
import com.github.junrar.rarfile.FileHeader;

public class Junrar {

	private static Log logger = LogFactory.getLog(Junrar.class.getName());

	public static void extract(final String rarPath, final String destinationPath) throws IOException, RarException {
		if (rarPath == null || destinationPath == null) {
			throw new RuntimeException("archive and destination must me set");
		}
		final File arch = new File(rarPath);
		final File dest = new File(destinationPath);
		extract(arch, dest);
	}

	public static void extract(final File rar, final File destinationFolder) throws RarException, IOException {
		validateRarPath(rar);
		validateDestinationPath(destinationFolder);
		
		final Archive archive = createArchiveOrThrowException(logger, rar);
		LocalFolderExtractor lfe = new LocalFolderExtractor(destinationFolder);
		extractArchiveTo(archive, lfe);
	}
	
	public static void extract(final InputStream resourceAsStream, final File destinationFolder) throws RarException, IOException {
		validateDestinationPath(destinationFolder);
		
		final Archive arch = createArchiveOrThrowException(logger, resourceAsStream);
		LocalFolderExtractor lfe = new LocalFolderExtractor(destinationFolder);
		extractArchiveTo(arch, lfe);
	}
	
	public static void extract(
		final ExtractDestination destination, 
		final VolumeManager volumeManager
	) throws RarException, IOException {
		final Archive archive = new Archive(volumeManager);
		extractArchiveTo(archive, destination);
	}

	public static List<ContentDescription> getContentsDescription(final File rar) throws RarException, IOException {
		validateRarPath(rar);

		final Archive arch = createArchiveOrThrowException(logger, rar);

		final List<ContentDescription> contents = new ArrayList<ContentDescription>();
		try{
			if (arch.isEncrypted()) {
				logger.warn("archive is encrypted cannot extract");
				return new ArrayList<ContentDescription>();
			}
			for(final FileHeader fileHeader : arch ) {
				contents.add(new ContentDescription(fileHeader.getFileNameString(), fileHeader.getUnpSize()));
			}
		}finally {
			arch.close();
		}
		return contents;
	}

	private static Archive createArchiveOrThrowException(final Log logger, final InputStream rarAsStream) throws RarException, IOException {
		try {
			return new Archive(rarAsStream);
		} catch (final RarException e) {
			logger.error(e);
			throw e;
		} catch (final IOException e1) {
			logger.error(e1);
			throw e1;
		}
	}

	private static Archive createArchiveOrThrowException(
		final Log logger, 
		final File file
	) throws RarException, IOException {
		try {
			return new Archive(new FileVolumeManager(file));
		} catch (final RarException e) {
			logger.error(e);
			throw e;
		} catch (final IOException e1) {
			logger.error(e1);
			throw e1;
		}
	}

	private static void validateDestinationPath(final File destinationFolder) {
		if (destinationFolder == null) {
			throw new RuntimeException("archive and destination must me set");
		}
		if (!destinationFolder.exists() || !destinationFolder.isDirectory()) {
			throw new IllegalArgumentException("the destination must exist and point to a directory: " + destinationFolder);
		}
	}

	private static void validateRarPath(final File rar) {
		if (rar == null) {
			throw new RuntimeException("archive and destination must me set");
		}
		if (!rar.exists()) {
			throw new IllegalArgumentException("the archive does not exit: " + rar);
		}
		if(!rar.isFile()) {
			throw new IllegalArgumentException("First argument should be a file but was "+rar.getAbsolutePath());
		}
	}

	private static void extractArchiveTo(final Archive arch, final ExtractDestination destination) throws IOException, RarException {
		if (arch.isEncrypted()) {
			logger.warn("archive is encrypted cannot extract");
			arch.close();
			return;
		}

		try{
			for(final FileHeader fh : arch ) {
				try {
					tryToExtract(logger, destination, arch, fh);
				} catch (final IOException e) {
					logger.error("error extracting the file", e);
					throw e;
				} catch (final RarException e) {
					logger.error("error extraction the file", e);
					throw e;
				}
			}
		}finally {
			arch.close();
		}
	}

	private static void tryToExtract(
		final Log logger,
		final ExtractDestination destination, 
		final Archive arch, 
		final FileHeader fileHeader
	) throws IOException, RarException {
		final String fileNameString = fileHeader.getFileNameString();
		if (fileHeader.isEncrypted()) {
			logger.warn("file is encrypted cannot extract: "+ fileNameString);
			return;
		}
		logger.info("extracting: " + fileNameString);
		if (fileHeader.isDirectory()) {
			destination.createDirectory(fileHeader);
		} else {
			destination.extract(arch, fileHeader);
		}
	}		

}
